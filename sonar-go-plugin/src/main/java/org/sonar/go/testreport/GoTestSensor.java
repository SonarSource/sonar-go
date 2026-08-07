/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.testreport;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.go.coverage.GoPathContext;
import org.sonar.go.plugin.GoLanguage;

public class GoTestSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoTestSensor.class);

  public static final String REPORT_PATH_KEY = "sonar.go.tests.reportPaths";

  GoPathContext goPathContext = GoPathContext.DEFAULT;

  // Matches every top-level test/benchmark/fuzz/example function declaration in a single pass.
  // Go requires these functions to start with one of these prefixes, which is exactly what the
  // "Test" field of a `go test -json` report references.
  private static final Pattern FUNC_DECL = Pattern.compile(
    "^func\\s+((?:Test|Benchmark|Fuzz|Example)\\w*)\\s*\\(", Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS);

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(GoLanguage.KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PATH_KEY))
      .name("Go Unit Test Report");
  }

  @Override
  public void execute(SensorContext context) {
    List<TestInfo> testInfoList = getReportPaths(context).stream()
      .flatMap(path -> parseReport(path).stream())
      .toList();

    // Knowing upfront every test function referenced across all reports, per package, lets
    // indexTestFunctions stop reading files as soon as all of them have been located,
    // instead of always scanning every test file in the package.
    Map<String, Set<String>> requiredFuncNamesByPackage = new HashMap<>();
    for (TestInfo testInfo : testInfoList) {
      requiredFuncNamesByPackage
        .computeIfAbsent(testInfo.pkg, key -> new HashSet<>())
        .add(testInfo.testSanitized);
    }

    var testFileByFuncNameByPackage = indexTestFunctions(context.fileSystem(), requiredFuncNamesByPackage);

    var testInfoByFile = new HashMap<InputFile, List<TestInfo>>();
    for (TestInfo testInfo : testInfoList) {
      var testFileByFuncName = testFileByFuncNameByPackage.getOrDefault(testInfo.pkg, Collections.emptyMap());
      var testFile = testFileByFuncName.get(testInfo.testSanitized);

      if (testFile != null) {
        testInfoByFile
          .computeIfAbsent(testFile, key -> new ArrayList<>())
          .add(testInfo);
      } else {
        LOG.warn("Failed to find test file for package {} and test {}", testInfo.pkg, testInfo.testSanitized);
      }
    }

    testInfoByFile.forEach((key, value) -> saveTestMetrics(context, key, value));
  }

  private static List<Path> getReportPaths(SensorContext context) {
    List<Path> result = new ArrayList<>();
    String[] reportPaths = context.config().getStringArray(REPORT_PATH_KEY);
    for (String reportPath : reportPaths) {
      Path path = Paths.get(reportPath);
      if (!path.isAbsolute()) {
        path = context.fileSystem().baseDir().toPath().resolve(path);
      }
      if (path.toFile().exists()) {
        result.add(path);
      } else {
        LOG.warn("Test report can't be loaded, file not found: '{}', ignoring this file.", path);
      }
    }

    return result;
  }

  private static List<TestInfo> parseReport(Path reportPath) {
    try {
      return Files.readAllLines(reportPath).stream()
        .filter(line -> line.startsWith("{"))
        .map(line -> getRelevantTestInfo(line, reportPath))
        .filter(Objects::nonNull)
        .toList();
    } catch (IOException e) {
      LOG.warn("Failed to read unit test report file " + reportPath, e);
      return Collections.emptyList();
    }
  }

  @Nullable
  private static TestInfo getRelevantTestInfo(String line, Path reportPath) {
    try {
      TestInfo testInfo = new TestInfo(Json.parse(line).asObject());
      if (testInfo.isRelevant()) {
        return testInfo;
      }
    } catch (Exception e) {
      LOG.warn("Failed to parse unit test report line (file {}):\n {}", reportPath, line);
    }

    return null;
  }

  // visible for testing purposes
  Map<String, Map<String, InputFile>> indexTestFunctions(FileSystem fileSystem, Map<String, Set<String>> requiredFuncNamesByPackage) {
    // Caching, per package, an index from test function name to the file that declares it.
    // This ensures each test file is read and scanned only once, regardless of how many tests it contains.
    var testFileByFuncNameByPackage = new HashMap<String, Map<String, InputFile>>();

    for (Map.Entry<String, Set<String>> functionsPerPackage : requiredFuncNamesByPackage.entrySet()) {
      String packageName = functionsPerPackage.getKey();
      Map<String, InputFile> stringInputFileMap = indexTestFunctionsForPackage(fileSystem, packageName, functionsPerPackage.getValue());
      testFileByFuncNameByPackage.put(packageName, stringInputFileMap);
    }

    return testFileByFuncNameByPackage;
  }

  // visible for testing purposes
  Map<String, InputFile> indexTestFunctionsForPackage(FileSystem fileSystem, String goPackage, Set<String> requiredFuncNames) {
    Map<String, InputFile> testFileByFuncName = new HashMap<>();
    for (InputFile testFile : getTestFilesForPackage(fileSystem, goPackage)) {
      try {
        var matcher = FUNC_DECL.matcher(testFile.contents());
        while (matcher.find()) {
          testFileByFuncName.putIfAbsent(matcher.group(1), testFile);
        }
      } catch (IOException ioe) {
        LOG.warn("Failed to read test file {}", testFile.uri());
        LOG.debug("Stacktrace:", ioe);
      }

      // Once every test function referenced by the report has been located, there is no need
      // to read and scan the remaining test files in this package.
      if (testFileByFuncName.keySet().containsAll(requiredFuncNames)) {
        break;
      }
    }
    return testFileByFuncName;
  }

  private List<InputFile> getTestFilesForPackage(FileSystem fileSystem, String goPackage) {
    FilePredicates predicates = fileSystem.predicates();
    String packageDirectory = goPathContext.resolve(goPackage);

    if (!new File(packageDirectory).exists()) {
      packageDirectory = findPackageDirectory(goPackage, fileSystem);
      if (packageDirectory == null) {
        return Collections.emptyList();
      }
    }

    try (Stream<Path> stream = Files.list(Paths.get(packageDirectory))) {
      // Files.list does not guarantee any particular order; sorting makes the early-termination
      // behavior of indexTestFunctions deterministic. Go forbids duplicate top-level function
      // names within a package, so the sort order never affects which file a test resolves to.
      return stream
        .map(path -> fileSystem.inputFile(testFilePredicate(predicates, path)))
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(InputFile::filename))
        .toList();

    } catch (IOException e) {
      LOG.warn("Failed to read package directory " + packageDirectory, e);
      return Collections.emptyList();
    }
  }

  private static FilePredicate testFilePredicate(FilePredicates predicates, Path path) {
    return predicates.and(
      predicates.hasType(Type.TEST),
      predicates.hasAbsolutePath(path.toString()),
      predicates.hasLanguage(GoLanguage.KEY));
  }

  private static String findPackageDirectory(String packagePath, FileSystem fileSystem) {
    File resolved = fileSystem.baseDir().toPath().resolve(packagePath).toFile();
    if (resolved.exists()) {
      return resolved.toString();
    }

    Path path = Paths.get(packagePath);
    if (path.getNameCount() == 1) {
      // It either means that the package last element was the baseDir, or that the test is in the root dir of a go project
      // with module (the "Package" will be the name of the module, we should ignore it).
      return fileSystem.baseDir().toString();
    } else {
      Path subpath = path.subpath(1, path.getNameCount());
      return findPackageDirectory(subpath.toString(), fileSystem);
    }
  }

  private static void saveTestMetrics(SensorContext context, InputFile testFile, List<TestInfo> tests) {
    int skip = 0;
    long timeMs = 0;
    int fail = 0;
    for (TestInfo test : tests) {
      timeMs += test.elapsed * 1000;
      if (test.action.equals("skip")) {
        skip++;
      } else if (test.action.equals("fail")) {
        fail++;
      }
    }

    context.<Integer>newMeasure().on(testFile).withValue(skip).forMetric(CoreMetrics.SKIPPED_TESTS).save();
    context.<Long>newMeasure().on(testFile).withValue(timeMs).forMetric(CoreMetrics.TEST_EXECUTION_TIME).save();
    context.<Integer>newMeasure().on(testFile).withValue(tests.size()).forMetric(CoreMetrics.TESTS).save();
    context.<Integer>newMeasure().on(testFile).withValue(fail).forMetric(CoreMetrics.TEST_FAILURES).save();
  }

  static class TestInfo {
    final String action;
    final String pkg;
    final String testSanitized;
    final Double elapsed;

    public TestInfo(@Nullable String action, @Nullable String pkg, @Nullable String test, @Nullable Double elapsed) {
      this.action = action;
      this.pkg = pkg;
      this.testSanitized = stripSubTestSuffix(test);
      this.elapsed = elapsed;
    }

    TestInfo(JsonObject json) {
      this(json.getString("Action", null),
        json.getString("Package", null),
        json.getString("Test", null),
        json.get("Elapsed") != null ? json.getDouble("Elapsed", 0.0d) : null);
    }

    boolean isRelevant() {
      return action != null && testSanitized != null && pkg != null && elapsed != null &&
        (action.equals("pass") || action.equals("fail") || action.equals("skip"));
    }

    /**
     * If the test was actually a sub-test, the name is of the form "TestFunc/Sub_Test_Name".
     */
    private static String stripSubTestSuffix(@Nullable String testName) {
      return testName != null ? testName.split("/", 2)[0] : null;
    }

  }
}
