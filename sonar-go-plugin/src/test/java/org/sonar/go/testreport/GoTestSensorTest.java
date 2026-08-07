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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestFileSystem;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.go.coverage.GoPathContext;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;
import org.sonar.scanner.plugin.api.impl.fs.DefaultInputFile;
import org.sonar.scanner.plugin.api.impl.sensor.DefaultSensorDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoTestSensorTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private final Path goPath = Paths.get("src", "test", "resources", "testReportGoPath").toAbsolutePath();
  private final Path packagePath = Paths.get("github.com", "myOrg", "myProject");

  @Test
  void absolute_package_path_in_report() {
    Path packageAbsPath = goPath.resolve("src").resolve(packagePath);

    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, null);
    String transformedPackageAbsPath;
    if (File.pathSeparator.equals(":")) {
      transformedPackageAbsPath = "_" + packageAbsPath;
    } else {
      transformedPackageAbsPath = "_\\" + packageAbsPath.toString().replaceFirst(":", "_");
    }
    SensorContextTester contextTester = SensorContextTester.create(packageAbsPath);
    TestFileSystem fs = contextTester.fileSystem();
    DefaultInputFile testFile = getTestInputFile(fs, "func TestFoo(", "foo_test.go");

    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, transformedPackageAbsPath, Set.of("TestFoo"));
    assertThat(testFileByFuncName).containsEntry("TestFoo", testFile);
  }

  @Test
  void relative_package_path_in_report() {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();
    DefaultInputFile testFile = getTestInputFile(fs, "func TestFoo(", "foo_test.go");

    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, packagePath.toString(), Set.of("TestFoo"));
    assertThat(testFileByFuncName).containsEntry("TestFoo", testFile);
  }

  @Test
  void invalid_package_path_in_report() {
    Path nestedPackagePath = packagePath.resolve("packageFoo");

    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, null);

    Path baseDir = Paths.get("src", "test", "resources", "myProject").toAbsolutePath();
    SensorContextTester contextTester = SensorContextTester.create(baseDir);

    TestFileSystem fs = contextTester.fileSystem();

    DefaultInputFile topTestFile = getTestInputFile(fs, "func TestFoo(", "foo_test.go");
    DefaultInputFile nestedTestFile = getTestInputFile(fs, "\nfunc   TestFoo (", "packageFoo/foo_test.go");

    Map<String, InputFile> testFileByFuncName;
    testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, packagePath.toString(), Set.of("TestFoo"));
    assertThat(testFileByFuncName).containsEntry("TestFoo", topTestFile);

    testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, nestedPackagePath.toString(), Set.of("TestFoo"));
    assertThat(testFileByFuncName).containsEntry("TestFoo", nestedTestFile);
  }

  @Test
  void test_describe() {
    GoTestSensor goTestSensor = new GoTestSensor();
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    goTestSensor.describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("Go Unit Test Report");
    assertThat(descriptor.languages()).containsOnly("go");
  }

  @Test
  void import_report() {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);

    SensorContextTester context = SensorContextTester.create(baseDir);
    TestFileSystem fs = context.fileSystem();
    DefaultInputFile fooTestFile = getTestInputFile(fs, "something  \nfunc TestFoo1( \nfunc TestFoo2(  ", "foo_test.go");
    DefaultInputFile barTestFile = getTestInputFile(fs, "func TestBar(", "bar_test.go");

    MapSettings settings = new MapSettings();
    String absoluteReportPath = baseDir.resolve("report1.out").toString();
    settings.setProperty(GoTestSensor.REPORT_PATH_KEY, "report.out,invalid/report/path," + absoluteReportPath);
    context.setSettings(settings);

    goTestSensor.execute(context);

    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TESTS).value()).isEqualTo(3); // one test comes from report1.out
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.SKIPPED_TESTS).value()).isZero();
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_ERRORS)).isNull();
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(4);

    // TestBar is present two times, once with a correct package path, and once with an incorrect one.
    // We can not differentiate the second invalid path from a module name (see go_module_report_test),
    // so we consider it as valid. It is really unlikely to happen in a real situation.
    assertThat(context.measure(barTestFile.key(), CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(barTestFile.key(), CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(2);
    assertThat(context.measure(barTestFile.key(), CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(barTestFile.key(), CoreMetrics.TEST_ERRORS)).isNull();
    assertThat(context.measure(barTestFile.key(), CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(7 + 7);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(String.join("\n", logTester.logs(Level.WARN)))
      .contains("Test report can't be loaded, file not found");
  }

  @Test
  void importReportShouldNotBreakWhenOneInputFileIsThrowingIOException() throws IOException {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());
    Path baseDir = goPath.resolve("src").resolve(packagePath);

    SensorContextTester context = SensorContextTester.create(baseDir);
    DefaultInputFile fooTestFile = getTestInputFile(context.fileSystem(), "something  \nfunc TestFoo1( \nfunc TestFoo2(  ", "foo_test.go");
    DefaultInputFile brokenInputFile = spy(new TestInputFileBuilder("moduleKey", "bar_test.go")
      .setLanguage("go")
      .setType(Type.TEST)
      .setContents("")
      .build());
    when(brokenInputFile.contents()).thenThrow(new IOException("BOOM"));
    context.fileSystem().add(brokenInputFile);

    MapSettings settings = new MapSettings();
    String absoluteReportPath = baseDir.resolve("report1.out").toString();
    settings.setProperty(GoTestSensor.REPORT_PATH_KEY, "report.out,invalid/report/path," + absoluteReportPath);
    context.setSettings(settings);

    goTestSensor.execute(context);

    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TESTS).value()).isEqualTo(3); // one test comes from report1.out
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.SKIPPED_TESTS).value()).isZero();
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_ERRORS)).isNull();
    assertThat(context.measure(fooTestFile.key(), CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(4);
    assertThat(logTester.logs(Level.WARN)).anyMatch(log -> log.startsWith("Failed to parse unit test report line"));
  }

  private DefaultInputFile getTestInputFile(TestFileSystem fs, String content, String relativePath) {
    DefaultInputFile nestedTestFile = new TestInputFileBuilder("moduleKey", relativePath)
      .setLanguage("go")
      .setType(Type.TEST)
      .setContents(content)
      .build();
    fs.add(nestedTestFile);
    return nestedTestFile;
  }

  @Test
  void subtests() throws Exception {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);

    SensorContextTester context = SensorContextTester.create(baseDir);
    TestFileSystem fs = context.fileSystem();
    DefaultInputFile mulTestFile = getTestInputFile(fs, new String(Files.readAllBytes(baseDir.resolve("mul_test.go"))), "mul_test.go");

    MapSettings settings = new MapSettings();
    String absoluteReportPath = baseDir.resolve("subtest_report.json").toString();
    settings.setProperty(GoTestSensor.REPORT_PATH_KEY, absoluteReportPath);
    context.setSettings(settings);

    goTestSensor.execute(context);

    assertThat(context.measure(mulTestFile.key(), CoreMetrics.TESTS).value()).isEqualTo(4);
  }

  @Test
  void go_module_report_test() throws Exception {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);

    SensorContextTester context = SensorContextTester.create(baseDir);
    TestFileSystem fs = context.fileSystem();
    DefaultInputFile mulTestFile = getTestInputFile(fs, new String(Files.readAllBytes(baseDir.resolve("mul_test.go"))), "mul_test.go");

    MapSettings settings = new MapSettings();
    // Reports created from Go projects with modules contains the module name instead of the package path in the "Package". Ex:
    // "my/module/subpackage" is in fact referring to the subpackage folder.
    // "my/module" is referring to the root.
    String absoluteReportPath = baseDir.resolve("module_report.json").toString();
    settings.setProperty(GoTestSensor.REPORT_PATH_KEY, absoluteReportPath);
    context.setSettings(settings);

    goTestSensor.execute(context);

    assertThat(context.measure(mulTestFile.key(), CoreMetrics.TESTS).value()).isEqualTo(4);
  }

  @Test
  void indexesEachTestFileOnlyOnceRegardlessOfNumberOfTests() throws IOException {
    Path earlyExitPackagePath = Paths.get("github.com", "myOrg", "earlyExit");

    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(earlyExitPackagePath);
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();

    DefaultInputFile aTestFile = getSpyTestInputFile(fs, "func TestA1(t *testing.T) {}\nfunc TestA2(t *testing.T) {}", "a_test.go");
    DefaultInputFile zTestFile = getSpyTestInputFile(fs, "func TestZ1(t *testing.T) {}", "z_extra_test.go");

    Set<String> requiredFuncNames = Set.of("TestA1", "TestA2", "TestZ1");
    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, earlyExitPackagePath.toString(), requiredFuncNames);

    // Even though "a_test.go" declares two referenced functions, it is read only once.
    assertThat(testFileByFuncName)
      .containsEntry("TestA1", aTestFile)
      .containsEntry("TestA2", aTestFile)
      .containsEntry("TestZ1", zTestFile);
    verify(aTestFile).contents();
    verify(zTestFile).contents();
  }

  @Test
  void stopsScanningTestFilesOnceAllReferencedFunctionsAreFound() throws IOException {
    Path earlyExitPackagePath = Paths.get("github.com", "myOrg", "earlyExit");

    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(earlyExitPackagePath);
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();

    // "a_test.go" sorts before "z_extra_test.go", and declares every function referenced below.
    DefaultInputFile aTestFile = getSpyTestInputFile(fs, "func TestA1(t *testing.T) {}\nfunc TestA2(t *testing.T) {}", "a_test.go");
    DefaultInputFile zTestFile = getSpyTestInputFile(fs, "func TestZ1(t *testing.T) {}", "z_extra_test.go");

    // Only functions declared in "a_test.go" are referenced by the report.
    Set<String> requiredFuncNames = Set.of("TestA1", "TestA2");

    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, earlyExitPackagePath.toString(), requiredFuncNames);

    assertThat(testFileByFuncName).containsEntry("TestA2", aTestFile);
    verify(aTestFile).contents();
    verify(zTestFile, never()).contents();
  }

  private DefaultInputFile getSpyTestInputFile(TestFileSystem fs, String content, String relativePath) {
    DefaultInputFile inputFile = spy(new TestInputFileBuilder("moduleKey", relativePath)
      .setLanguage("go")
      .setType(Type.TEST)
      .setContents(content)
      .build());
    fs.add(inputFile);
    return inputFile;
  }

  @Test
  void resolvesTestFunctionsWithNonAsciiNames() {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();
    // "foo_test.go" is an existing file in the fixture's package directory; getTestFilesForPackage()
    // lists that real directory on disk, so the registered InputFile's relative path must match
    // a file that is physically present there.
    DefaultInputFile testFile = getTestInputFile(fs, "func TestÜmlaut(t *testing.T) {}", "foo_test.go");

    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(fs, packagePath.toString(), Set.of("TestÜmlaut"));

    assertThat(testFileByFuncName).containsEntry("TestÜmlaut", testFile);
  }

  @Test
  void indexesBenchmarkFuzzAndExampleFunctions() {
    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    Path baseDir = goPath.resolve("src").resolve(packagePath);
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();
    DefaultInputFile testFile = getTestInputFile(
      fs,
      "func BenchmarkFoo(b *testing.B) {}\nfunc FuzzFoo(f *testing.F) {}\nfunc ExampleFoo() {}",
      "bar_test.go");

    Map<String, InputFile> testFileByFuncName = goTestSensor.indexTestFunctionsForPackage(
      fs, packagePath.toString(), Set.of("BenchmarkFoo", "FuzzFoo", "ExampleFoo"));

    assertThat(testFileByFuncName)
      .containsEntry("BenchmarkFoo", testFile)
      .containsEntry("FuzzFoo", testFile)
      .containsEntry("ExampleFoo", testFile);
  }

  @Test
  void indexesFunctionsAcrossMultiplePackages() {
    Path earlyExitPackagePath = Paths.get("github.com", "myOrg", "earlyExit");

    GoTestSensor goTestSensor = new GoTestSensor();
    goTestSensor.goPathContext = new GoPathContext(File.separatorChar, File.pathSeparator, goPath.toString());

    // The fs baseDir must be a common ancestor of both package directories: InputFile lookup
    // resolves each registered relative path against this single baseDir.
    Path baseDir = goPath.resolve("src");
    SensorContextTester contextTester = SensorContextTester.create(baseDir);
    TestFileSystem fs = contextTester.fileSystem();
    DefaultInputFile fooTestFile = getTestInputFile(fs, "func TestFoo(t *testing.T) {}", packagePath.resolve("foo_test.go").toString());
    DefaultInputFile aTestFile = getTestInputFile(fs, "func TestA1(t *testing.T) {}", earlyExitPackagePath.resolve("a_test.go").toString());

    Map<String, Set<String>> requiredFuncNamesByPackage = Map.of(
      packagePath.toString(), Set.of("TestFoo"),
      earlyExitPackagePath.toString(), Set.of("TestA1"));

    Map<String, Map<String, InputFile>> testFileByFuncNameByPackage = goTestSensor.indexTestFunctions(fs, requiredFuncNamesByPackage);

    assertThat(testFileByFuncNameByPackage.get(packagePath.toString())).containsEntry("TestFoo", fooTestFile);
    assertThat(testFileByFuncNameByPackage.get(earlyExitPackagePath.toString())).containsEntry("TestA1", aTestFile);
  }
}
