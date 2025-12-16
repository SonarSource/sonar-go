/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.coverage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GoCoverSensorTest {

  static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void shouldTestDescriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    GoCoverSensor coverSensor = new GoCoverSensor();
    coverSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Go Cover sensor for Go coverage");
  }

  @Test
  void shouldFailWhenCoverageFileDoesntExit() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.settings().setProperty("sonar.go.coverage.reportPaths", "invalid-coverage-path.out");
    GoCoverSensor coverSensor = new GoCoverSensor();
    coverSensor.execute(context);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Coverage report can't be loaded, report file not found, ignoring this file invalid-coverage-path.out.");
  }

  @Test
  void shouldVerifyModeLineRegex() {
    Predicate<String> regexp = line -> GoCoverSensor.MODE_LINE_REGEXP.matcher(line).matches();
    assertThat(regexp.test("mode: set")).isTrue();
    assertThat(regexp.test("mode: count")).isTrue();
    assertThat(regexp.test("mode: atomic")).isTrue();
    assertThat(regexp.test("my-app/my-app.go:3.2,3.10 1 1")).isFalse();
  }

  @Test
  void shouldParseCoverageLinuxRrelative() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.linux.relative.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/go/src/github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);
  }

  @Test
  void shouldParseCoverageLinuxAbsolute() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.linux.absolute.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/dev/github/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);
  }

  @Test
  void shouldParseCoverageWindowsRelative() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.win.relative.out");
    GoPathContext windowsContext = new GoPathContext('\\', ";", "C:\\Users\\paul\\go");
    String coverPath = "C:\\Users\\paul\\go\\src\\github.com\\SonarSource\\slang\\sonar-go-plugin\\src\\test\\resources\\coverage\\cover.go";
    assertCoverGo(coverageFile, windowsContext, coverPath);
  }

  @Test
  void shouldParseCoverageWindowsAbsolute() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.win.absolute.out");
    GoPathContext windowsContext = new GoPathContext('\\', ";", "C:\\Users\\paul\\go");
    String coverPath = "C:\\Users\\paul\\dev\\github\\SonarSource\\slang\\sonar-go-plugin\\src\\test\\resources\\coverage\\cover.go";
    assertCoverGo(coverageFile, windowsContext, coverPath);
  }

  @Test
  void shouldParseCoverageOneBrokenLine() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.one.broken.line.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/go/src/github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);

    assertThat(logTester.logs(Level.DEBUG))
      .containsExactly("Ignoring line in coverage report: Invalid go coverage at line 7.");
  }

  @ParameterizedTest
  @ValueSource(ints = {5, 9997, 9998, 9999, 10_000, 10_001})
  void shouldStoreBigCoverage10000LinesReport(int numberOfLines) throws IOException {
    var lines = IntStream.range(0, numberOfLines).mapToObj(i -> "\tprint(\"foo\")\n").collect(Collectors.joining());
    var code = """
      package pgk1

      func foo() {
      %s
      }
      """.formatted(lines);
    saveFile("cover_big.go", code);

    var coverageLines = IntStream.range(0, numberOfLines)
      .mapToObj(i -> "github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover_big.go:" + (3 + i) + ".2," + (3 + i) + ".10 1 1\n")
      .collect(Collectors.joining());
    var coverageContent = "mode: set\n" + coverageLines;
    saveFile("coverage.big.out", coverageContent);

    Path coverageFile = COVERAGE_DIR.resolve("coverage.big.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    String coverPath = "/home/paul/go/src/github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover_big.go";
    AtomicInteger linesCount = new AtomicInteger();

    GoCoverSensor sensor = new GoCoverSensor();
    sensor.setCoverageStorage((sensorContext, coverage, goModFileData, reportPath) -> {
      if (!coverage.fileMap.isEmpty()) {
        linesCount.getAndAdd(coverage.fileMap.get(coverPath).size());
      }
    });
    sensor.parseAndSave(coverageFile, context, linuxContext);

    assertThat(linesCount.get()).isEqualTo(numberOfLines);
    if (numberOfLines >= 9998) {
      // Every 10_000 lines of code the debug is printed
      // The code contains 2 extra lines (package and new line) that are not counted in coverage
      assertThat(logTester.logs(Level.DEBUG)).contains("Save 10000 lines from coverage report 'src/test/resources/coverage/coverage.big.out'");
    }
  }

  private static void saveFile(String filename, String code) throws IOException {
    var file = new File(COVERAGE_DIR.toFile(), filename);
    file.deleteOnExit();
    var fileOutputStream = new FileOutputStream(file);
    fileOutputStream.write(code.getBytes(UTF_8));
    fileOutputStream.close();
  }

  @Test
  void shouldGetReportPaths() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    Path coverageFile1 = COVERAGE_DIR.resolve("coverage.linux.relative.out").toAbsolutePath();
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      coverageFile1 + ",coverage.linux.absolute.out");

    Stream<Path> reportPaths = GoCoverSensor.getReportPaths(context);

    assertThat(reportPaths).containsExactlyInAnyOrder(
      coverageFile1,
      Paths.get("src", "test", "resources", "coverage", "coverage.linux.absolute.out"));
  }

  @Test
  void shouldGetReportPathsWithWildcards() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "*.absolute.out,glob" + File.separator + "*.out, test*" + File.separator + "*.out, coverage?.out");

    Stream<Path> reportPaths = GoCoverSensor.getReportPaths(context);

    assertThat(reportPaths).containsExactlyInAnyOrder(
      Paths.get("src", "test", "resources", "coverage", "coverage.linux.absolute.out"),
      Paths.get("src", "test", "resources", "coverage", "coverage.win.absolute.out"),
      Paths.get("src", "test", "resources", "coverage", "glob", "coverage.glob.out"),
      Paths.get("src", "test", "resources", "coverage", "test1", "coverage.out"),
      Paths.get("src", "test", "resources", "coverage", "coverage1.out"));

    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "**" + File.separator + "coverage.glob.out");
    Stream<Path> reportPaths2 = GoCoverSensor.getReportPaths(context);
    assertThat(reportPaths2).containsExactlyInAnyOrder(
      Paths.get("src", "test", "resources", "coverage", "glob", "coverage.glob.out"));
  }

  @Test
  void getReportPathWithWildCardsShouldContinueIfOnePatternIsEmpty() {
    var emptyPattern = "*.foo.out";
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      emptyPattern + ",*.absolute.out");
    Stream<Path> reportPaths = GoCoverSensor.getReportPaths(context);
    assertThat(reportPaths).containsExactlyInAnyOrder(
      Paths.get("src", "test", "resources", "coverage", "coverage.linux.absolute.out"),
      Paths.get("src", "test", "resources", "coverage", "coverage.win.absolute.out"));

    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Coverage report can't be loaded, file(s) not found for pattern: '" +
        emptyPattern + "', ignoring this file.");
  }

  @Test
  void shouldContinueWhenParsingFails() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "test1" + File.separator + "coverage.out, coverage.relative.out");
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, baseDir.toString());
    GoCoverSensor sensor = new GoCoverSensor();

    sensor.execute(context, goContext);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN))
      .hasSize(3)
      .anyMatch(log -> log.endsWith("coverage.out: Invalid go coverage, expect 'mode:' on the first line."));
  }

  @Test
  void shouldUploadReports() throws IOException {
    String fileName = "cover.go";
    SensorContextTester context = setUpContext(fileName, "coverage.relative.out");
    String fileKey = "moduleKey:" + fileName;
    assertThat(context.lineHits(fileKey, 3)).isNull();
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 5)).isEqualTo(2);
    assertThat(context.conditions(fileKey, 5)).isNull();
    assertThat(context.coveredConditions(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isZero();
    assertThat(context.lineHits(fileKey, 7)).isZero();
    assertThat(context.lineHits(fileKey, 8)).isNull();

    String ignoredFileLog = "File 'doesntexists.go' is not included in the project, ignoring coverage";
    assertThat(logTester.logs(Level.WARN)).contains(ignoredFileLog);
  }

  @Test
  void shouldCoverageSwitchCase() throws Exception {
    String fileName = "coverage.switch.go";
    SensorContextTester context = setUpContext(fileName, "coverage.switch.out");
    String fileKey = "moduleKey:" + fileName;
    // Opening brace of function should not be included into the switch
    assertThat(context.lineHits(fileKey, 3)).isNull();
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    // Switch case should not be counted
    assertThat(context.lineHits(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 7)).isNull();
    assertThat(context.lineHits(fileKey, 8)).isZero();
    assertThat(context.lineHits(fileKey, 9)).isNull();
    assertThat(context.lineHits(fileKey, 10)).isZero();
    assertThat(context.lineHits(fileKey, 11)).isNull();
    assertThat(context.lineHits(fileKey, 12)).isZero();
    assertThat(context.lineHits(fileKey, 13)).isNull();
    assertThat(context.lineHits(fileKey, 14)).isZero();
  }

  @Test
  void shouldLogFailureOnFailedCoverageSaving() throws IOException {
    String fileName = "cover.go";
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths", "coverage.relative.out");
    Path goFilePath = baseDir.resolve(fileName);
    String content = new String(Files.readAllBytes(goFilePath), UTF_8);
    DefaultInputFile brokenInputFile = spy(TestInputFileBuilder.create("moduleKey", baseDir.toFile(), goFilePath.toFile())
      .setLanguage("go")
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build());
    when(brokenInputFile.contents()).thenThrow(new IOException("BOOM"));
    context.fileSystem().add(brokenInputFile);
    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, "");
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);

    assertThat(logTester.logs(Level.WARN)).contains("Failed saving coverage info for file: cover.go");
  }

  @Test
  void shouldImportGoCoverageReportWithModuleName() throws IOException {
    // this dir contains a go.mod with module name 'example.com/greetings', Go files and Go coverage report 'cover.out'
    var coverageMonorepoDir = Paths.get("src", "test", "resources", "coverage-monorepo", "greetings");
    Path baseDir = coverageMonorepoDir.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths", "cover.out");

    addFile(context, baseDir, "go.mod");
    addFile(context, baseDir, "greetings.go");
    addFile(context, baseDir, "greetings_test.go");

    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, "");
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);

    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Saving coverage measures for file 'example.com/greetings/greetings.go'");
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(text -> text.startsWith(
      "Resolved file 'example.com/greetings/greetings.go' to 'greetings.go' using absolute path, without module name in report path"));
  }

  @Test
  void shouldImportGoCoverageReportForMonorepo() throws IOException {
    // this dir contains a subdir greetings, that contains Go project. It simulates a monorepo structure
    var coverageMonorepoDir = Paths.get("src", "test", "resources", "coverage-monorepo");
    Path baseDir = coverageMonorepoDir.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths", "greetings/cover.out");

    addFile(context, baseDir, "greetings/go.mod");
    addFile(context, baseDir, "greetings/greetings.go");
    addFile(context, baseDir, "greetings/greetings_test.go");

    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, "");
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);

    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Saving coverage measures for file 'example.com/greetings/greetings.go'");
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(text -> text.startsWith(
      "Resolved file 'example.com/greetings/greetings.go' to 'greetings/greetings.go' using absolute path, without module name in report path"));
  }

  private void addFile(SensorContextTester context, Path baseDir, String fileName) throws IOException {
    Path filepath = baseDir.resolve(fileName);
    String content = Files.readString(filepath);
    var file = TestInputFileBuilder.create("moduleKey", baseDir.toFile(), filepath.toFile())
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build();
    context.fileSystem().add(file);
  }

  private SensorContextTester setUpContext(String fileName, String coverageFile) throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths", coverageFile);
    Path goFilePath = baseDir.resolve(fileName);
    String content = Files.readString(goFilePath);
    context.fileSystem().add(TestInputFileBuilder.create("moduleKey", baseDir.toFile(), goFilePath.toFile())
      .setLanguage("go")
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build());
    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, "");
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);
    return context;
  }

  private void assertCoverGo(Path coverageFile, GoPathContext goContext, String absolutePath) {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    GoCoverSensor sensor = new GoCoverSensor();

    sensor.setCoverageStorage((sensorContext, coverage, goModFileData, reportPath) -> {
      assertThat(coverage.fileMap.keySet()).containsExactlyInAnyOrder(absolutePath);
      List<CoverageStat> coverageStats = coverage.fileMap.get(absolutePath);
      FileCoverage fileCoverage = new FileCoverage(coverageStats, null);
      assertThat(fileCoverage.lineMap.keySet()).containsExactlyInAnyOrder(3, 4, 5, 6, 7, 8);
      assertThat(fileCoverage.lineMap.get(2)).isNull();
      assertThat(fileCoverage.lineMap.get(3).hits).isEqualTo(1);
      assertThat(fileCoverage.lineMap.get(4).hits).isEqualTo(2);
      assertThat(fileCoverage.lineMap.get(5).hits).isEqualTo(2);
      assertThat(fileCoverage.lineMap.get(6).hits).isZero();
      assertThat(fileCoverage.lineMap.get(7).hits).isZero();
      assertThat(fileCoverage.lineMap.get(8).hits).isZero();
      assertThat(fileCoverage.lineMap.get(9)).isNull();
    });
    sensor.parseAndSave(coverageFile, context, goContext);
  }
}
