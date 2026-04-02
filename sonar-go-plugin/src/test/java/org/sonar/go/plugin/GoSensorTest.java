/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonar.go.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.check.Rule;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.converter.GoParseCommand;
import org.sonar.go.testing.TestInputFileCreator;
import org.sonar.go.testing.TextRangeAssert;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sonar.go.testing.TestInputFileCreator.createInputFile;

class GoSensorTest {

  private static final SonarRuntime SQ_LTS_RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.DEVELOPER);
  private static final SonarRuntime SONAR_LINT_RUNTIME = SonarRuntimeImpl.forSonarLint(Version.create(13, 0));
  public static final SonarRuntime SQ_TELEMETRY_SUPPORTING_RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(10, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
  private GoConverter singleInstanceGoConverter;
  private Path projectDir;
  private File baseDir;
  private SensorContextTester context;

  private SensorContextTester sensorContext;
  private final FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
  private FileLinesContextTester fileLinesContext;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  void setUp(@TempDir File tmpBaseDir) throws IOException {
    baseDir = tmpBaseDir;
    context = SensorContextTester.create(baseDir);

    var workDir = Files.createTempDirectory("gotest");
    workDir.toFile().deleteOnExit();
    singleInstanceGoConverter = new GoConverter(workDir.toFile());
    projectDir = Files.createTempDirectory("gotestProject");
    projectDir.toFile().deleteOnExit();
    sensorContext = SensorContextTester.create(workDir);
    sensorContext.fileSystem().setWorkDir(workDir);
    sensorContext.settings().setProperty("sonar.go.converter.validation", "throw");
    sensorContext.setRuntime(SQ_LTS_RUNTIME);
    fileLinesContext = new FileLinesContextTester();
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
  }

  @Test
  void testDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    sensor("S1110").describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Code Quality and Security for Go");
    assertThat(descriptor.languages()).containsOnly("go");
  }

  @Test
  void testIssue() {
    InputFile inputFile = TestInputFileCreator.createInputFile("lets.go",
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""", baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor("S1110");

    goSensor.execute(sensorContext);

    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void test_file_issue() {
    InputFile inputFile = createInputFile("lets.go",
      "// TODO implement the logic \n package main \n",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_line_issue() {
    InputFile inputFile = createInputFile("lets.go",
      "package                                                                                                                                                                                                                               main\n",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor("S103");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_failure() throws Exception {
    InputFile failingFile = createInputFile("lets.go",
      """
        package main\s

        func test() {
         pwd := "secret"
        }""",
      baseDir, null, InputFile.Type.MAIN);
    failingFile = spy(failingFile);
    doThrow(new IOException("The file is corrupted")).when(failingFile).contents();

    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = sensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isNotEmpty().anyMatch(l -> l.startsWith("Unable to parse directory"));
  }

  @Test
  void test_empty_file() {
    InputFile failingFile = createInputFile("lets.go", "", baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = sensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void metrics() {
    InputFile inputFile = createInputFile("lets.go", """
      // This is not a line of code
      package main
      import "fmt"
      type class1 struct { x, y int }
      type class2 struct { a, b string }
      type anyObject interface {}
      func fun1() {
        fmt.Println("Statement 1")
      }
      func fun2(i int) {
        switch i { // Statement 2
        case 2:
          fmt.Println(
            "Not a Statement 3",
          )
        }
      }
      func fun3(x interface{}) int {
        return 42 // Statement 4
      }""",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(19);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isEqualTo(3);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(3);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(3);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(4);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COGNITIVE_COMPLEXITY).value()).isEqualTo(1);

    assertThat(fileLinesContext.saveCount).isEqualTo(1);

    assertThat(fileLinesContext.metrics.keySet()).containsExactlyInAnyOrder(CoreMetrics.NCLOC_DATA_KEY,
      CoreMetrics.EXECUTABLE_LINES_DATA_KEY);

    assertThat(fileLinesContext.metrics.get(CoreMetrics.NCLOC_DATA_KEY)).containsExactlyInAnyOrder(
      "2:1", "3:1", "4:1", "5:1", "6:1", "7:1", "8:1", "9:1", "10:1", "11:1",
      "12:1", "13:1", "14:1", "15:1", "16:1", "17:1", "18:1", "19:1", "20:1");

    assertThat(fileLinesContext.metrics.get(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)).containsExactlyInAnyOrder(
      "8:1", "11:1", "13:1", "19:1");
  }

  @Test
  void test_not_executable_lines() {
    InputFile inputFile = createInputFile("lets.go",
      """
        package awesomeProject
        const a = "a"
        var myVar int = 12
        var i, j int = 1, 2
        var c, c1, c2, c3, c4 int
        const (
        \tUpdate = "update"
        \tDelete = "delete"
        )
        type Message struct {
        }
        type (
        \tRrsType string
        )
        """,
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);

    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(14);
    assertThat(fileLinesContext.metrics.get(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)).isNull();
  }

  @Test
  void metrics_for_test_file() {
    InputFile inputFile = createInputFile("lets_test.go",
      """
        // This is not a line of code
        package main
        import "fmt"
        func main() {
          fmt.Println("Hello")
        }
        """,
      baseDir, null, InputFile.Type.TEST);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isZero();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isZero();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isZero();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isZero();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COGNITIVE_COMPLEXITY).value()).isZero();

    assertThat(fileLinesContext.saveCount).isEqualTo(1);
    assertThat(fileLinesContext.metrics).isEmpty();
  }

  @Test
  void cognitive_complexity_metric() {
    InputFile inputFile = createInputFile("lets.go",
      """
        package main
        import "fmt"
        func fun1(i int) int {
          if i < 0 { // +1
            i++
          }
          return i
        }
        func fun2(i int) int {
          if i < 0 { // +1
            i--
          }
          f := func(int) int {
            if i < 0 { // +2 (incl 1 for nesting)
              i++
            }
            return i
          }
          return i + f(i)
        }

        """,
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COGNITIVE_COMPLEXITY).value()).isEqualTo(4);
  }

  @Test
  void highlighting() {
    InputFile inputFile = createInputFile("lets.go",
      """
        //abc
        /*x*/
        package main
        import "fmt"
        type class1 struct { }
        func fun2(x string) int {
          return 42
        }
        """,
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);

    String componentKey = "moduleKey:lets.go";
    // //abc
    assertHighlighting(componentKey, 1, 1, 5, TypeOfText.COMMENT);
    // /*x*/
    assertHighlighting(componentKey, 2, 1, 5, TypeOfText.COMMENT);
    // package main
    assertHighlighting(componentKey, 3, 1, 7, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 3, 9, 12, null);
    // import "fmt"
    assertHighlighting(componentKey, 4, 1, 6, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 4, 8, 12, TypeOfText.STRING);
    // type class1 struct { }
    assertHighlighting(componentKey, 5, 1, 4, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 5, 6, 11, null);
    assertHighlighting(componentKey, 5, 13, 18, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 5, 20, 22, null);
    // func fun2(x string) int {
    assertHighlighting(componentKey, 6, 1, 4, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 6, 6, 9, null);
    assertHighlighting(componentKey, 6, 6, 12, null);
    assertHighlighting(componentKey, 6, 13, 18, null);
    assertHighlighting(componentKey, 6, 19, 20, null);
    assertHighlighting(componentKey, 6, 21, 23, null);
    // return 42
    assertHighlighting(componentKey, 7, 3, 8, TypeOfText.KEYWORD);
    assertHighlighting(componentKey, 7, 10, 11, TypeOfText.CONSTANT);
  }

  @Test
  void repository_key() {
    assertThat(sensor().repositoryKey()).isEqualTo("go");
  }

  @Rule(key = "GoVersionCheck")
  public static class GoVersionCheck implements GoCheck {
    @Override
    public void initialize(InitContext init) {
      init.register(VariableDeclarationTree.class, (ctx, tree) -> {
        if (!ctx.goModFileData().goVersion().isUnknownVersion()) {
          ctx.reportIssue(tree, "issue");
        }
      });
    }
  }

  @Test
  void versionShouldBeDetected() {
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule

        go 1.23.4
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        var a int
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = sensor(List.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void versionShouldNotBeDetectedOnMissingVersion() {
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        var a int
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = sensor(List.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void versionShouldNotBeDetectedOnMissingGoModFile() {
    InputFile goFile = createInputFile("lets.go",
      """
        package main
        var a int
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = sensor(List.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void versionShouldNotBeDetectedInSonarQubeIDEContext() {
    sensorContext.setRuntime(SONAR_LINT_RUNTIME);
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule

        go 1.23.4
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        var a int
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = sensor(List.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void shouldSendTelemetryWithGoVersion() {
    sensorContext.setRuntime(SQ_TELEMETRY_SUPPORTING_RUNTIME);
    sensorContext = spy(sensorContext);
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule

        go 1.23.4
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);

    sensor("S1135").execute(sensorContext);

    verify(sensorContext).addTelemetryProperty("go.used_version", "1.23.4");
  }

  @Test
  void shouldNotSendTelemetry() {
    sensorContext = spy(sensorContext);
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule

        go 1.23.4
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);

    sensor("S1135").execute(sensorContext);

    verify(sensorContext, never()).addTelemetryProperty(anyString(), anyString());
  }

  @Test
  void shouldSendTelemetryUnknownWhenGoModHasNoVersion() {
    sensorContext.setRuntime(SQ_TELEMETRY_SUPPORTING_RUNTIME);
    sensorContext = spy(sensorContext);
    InputFile goModFile = createInputFile("go.mod",
      """
        module myModule
        """,
      baseDir, null, InputFile.Type.MAIN);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    sensor("S1135").execute(sensorContext);

    verify(sensorContext).addTelemetryProperty("go.used_version", "unknown");
  }

  @Test
  void shouldSendTelemetryNoGoModFileWhenNoGoModPresent() {
    sensorContext.setRuntime(SQ_TELEMETRY_SUPPORTING_RUNTIME);
    sensorContext = spy(sensorContext);
    InputFile goFile = createInputFile("lets.go",
      """
        package main
        """,
      baseDir, null, InputFile.Type.MAIN);

    sensorContext.fileSystem().add(goFile);
    sensor("S1135").execute(sensorContext);

    verify(sensorContext).addTelemetryProperty("go.used_version", "noGoModFile");
  }

  @Test
  void shouldSendTelemetryWithMultipleGoVersions() throws IOException {
    var tempDir = Files.createTempDirectory("goMultiMod");
    tempDir.toFile().deleteOnExit();
    var moduleA = tempDir.resolve("moduleA");
    Files.createDirectories(moduleA);
    var moduleB = tempDir.resolve("moduleB");
    Files.createDirectories(moduleB);

    var multiModContext = spy(SensorContextTester.create(tempDir));
    multiModContext.fileSystem().setWorkDir(tempDir);
    multiModContext.settings().setProperty("sonar.go.converter.validation", "throw");
    multiModContext.setRuntime(SQ_TELEMETRY_SUPPORTING_RUNTIME);

    var goModFileA = createInputFile("moduleA/go.mod",
      """
        module moduleA

        go 1.21
        """,
      tempDir.toFile(), null, InputFile.Type.MAIN);

    var goModFileB = createInputFile("moduleB/go.mod",
      """
        module moduleB

        go 1.23
        """,
      tempDir.toFile(), null, InputFile.Type.MAIN);

    var goFileA = createInputFile("moduleA/main.go",
      """
        package main
        """,
      tempDir.toFile(), null, InputFile.Type.MAIN);

    var goFileB = createInputFile("moduleB/main.go",
      """
        package main
        """,
      tempDir.toFile(), null, InputFile.Type.MAIN);

    multiModContext.fileSystem().add(goModFileA);
    multiModContext.fileSystem().add(goModFileB);
    multiModContext.fileSystem().add(goFileA);
    multiModContext.fileSystem().add(goFileB);

    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    new GoSensor(checkFactory("S1135"), fileLinesContextFactory, new DefaultNoSonarFilter(),
      new GoLanguage(new MapSettings().asConfig()), singleInstanceGoConverter).execute(multiModContext);

    verify(multiModContext).addTelemetryProperty("go.used_version", "1.21;1.23");
  }

  @Test
  void shouldRaiseIssueOnConverterLogValidation() {
    InputFile inputFile = createInputFile("lets.go",
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    sensorContext.settings().setProperty("sonar.go.converter.validation", "log");
    GoSensor goSensor = sensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void shouldRaiseIssueOnWhenConverterPropertyIsInvalid() {
    InputFile inputFile = createInputFile("lets.go",
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    sensorContext.settings().setProperty("sonar.go.converter.validation", "invalid");
    GoSensor goSensor = sensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).contains("Unsupported mode for converter validation: 'invalid', falling back to no validation");
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.internal.analysis.failFast,true,true",
    "sonar.internal.analysis.failFast,false,false",
    "other,true,false",
  })
  void shouldReturnFailFastValue(String propertyKey, String propertyValue, boolean expected) {
    sensorContext.settings().setProperty(propertyKey, propertyValue);
    assertThat(GoSensor.isFailFast(sensorContext)).isEqualTo(expected);
  }

  @Test
  void two_checks_registering_on_leave_should_not_interfere() {
    BiConsumer<CheckContext, Tree> consumer1 = spy(new ConsumerToSpy());
    BiConsumer<CheckContext, Tree> consumer2 = spy(new ConsumerToSpy());

    GoSensor sensor = sensor(List.of(CheckRegisteringOnLeave1.class, CheckRegisteringOnLeave2.class));
    List<GoCheck> allChecks = sensor.checks().all();
    assertThat(allChecks).hasSize(2);
    ((CheckRegisteringOnLeave) allChecks.get(0)).setConsumerToRegister(consumer1);
    ((CheckRegisteringOnLeave) allChecks.get(1)).setConsumerToRegister(consumer2);

    InputFile goFile = createInputFile("lets.go",
      """
        package main
        var a int
        """,
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(goFile);

    sensor.execute(sensorContext);

    verify(consumer1, Mockito.times(1)).accept(any(), any());
    verify(consumer2, Mockito.times(1)).accept(any(), any());
  }

  @Test
  void shouldLogDurationStatisticsAndMemoryMeasurementsWhenEnabled() {
    MapSettings settings = sensorContext.settings();
    settings.setProperty("sonar.go.duration.statistics", "true");
    InputFile inputFile = new TestInputFileBuilder("projectKey", "foo.go")
      .setType(InputFile.Type.MAIN)
      .setLanguage(GoLanguage.KEY)
      .setContents("package main\n\nfunc Foo() {}")
      .build();
    sensorContext.fileSystem().add(inputFile);

    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);

    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.startsWith("Duration Statistics"))
      .anyMatch(log -> log.startsWith("Go memory statistics"));
  }

  @Test
  void shouldNotLogDurationStatisticsAndMemoryMeasurementsOnDefault() {
    InputFile inputFile = new TestInputFileBuilder("projectKey", "foo.go")
      .setType(InputFile.Type.MAIN)
      .setLanguage(GoLanguage.KEY)
      .setContents("package main\n\nfunc Foo() {}")
      .build();
    sensorContext.fileSystem().add(inputFile);

    GoSensor goSensor = sensor();
    goSensor.execute(sensorContext);

    assertThat(logTester.logs(Level.INFO))
      .noneMatch(log -> log.startsWith("Duration Statistics"))
      .noneMatch(log -> log.startsWith("Go memory statistics"));
  }

  @Test
  void shouldSetDebugTypeCheck() {
    var command = mock(GoParseCommand.class);
    singleInstanceGoConverter = new GoConverter(command);
    sensorContext.settings().setProperty("sonar.go.internal.debugTypeCheck", true);

    GoSensor sensor = sensor();
    sensor.execute(sensorContext);

    verify(command, times(1)).debugTypeCheck();
  }

  @Test
  void shouldNotRaiseIssueWhenInactive() {
    InputFile inputFile = createInputFile("lets.go",
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""",
      baseDir, null, InputFile.Type.MAIN);
    sensorContext.fileSystem().add(inputFile);
    sensorContext.settings().setProperty("sonar.go.activate", "false");
    GoSensor goSensor = sensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  private abstract static class CheckRegisteringOnLeave implements GoCheck {
    private BiConsumer<CheckContext, Tree> consumerToRegister;

    public void setConsumerToRegister(BiConsumer<CheckContext, Tree> consumerToRegister) {
      this.consumerToRegister = consumerToRegister;
    }

    @Override
    public void initialize(InitContext init) {
      init.registerOnLeave(consumerToRegister);
    }
  }

  @Rule(key = "CheckRegisteringOnLeave1")
  public static class CheckRegisteringOnLeave1 extends CheckRegisteringOnLeave {
  }

  @Rule(key = "CheckRegisteringOnLeave2")
  public static class CheckRegisteringOnLeave2 extends CheckRegisteringOnLeave {
  }

  private static class ConsumerToSpy implements BiConsumer<CheckContext, Tree> {
    @Override
    public void accept(CheckContext checkContext, Tree tree) {
      // do nothing
    }
  }

  private void assertHighlighting(String componentKey, int line, int columnFirst, int columnLast, @Nullable TypeOfText type) {
    for (int column = columnFirst; column <= columnLast; column++) {
      List<TypeOfText> typeOfTexts = sensorContext.highlightingTypeAt(componentKey, line, column - 1);
      if (type != null) {
        assertThat(typeOfTexts).as("Expect highlighting " + type + " at line " + line + " lineOffset " + column).containsExactly(type);
      } else {
        assertThat(typeOfTexts).as("Expect no highlighting at line " + line + " lineOffset " + column).containsExactly();
      }
    }
  }

  @Test
  void testOneRule() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func main() {
        print (1 == 1)
      }""", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");

    sensor(checkFactory).execute(context);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1764");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Correct one of the identical sub-expressions on both sides of this operator.");
    TextRangeAssert.assertThat(location.textRange()).hasRange(3, 14, 3, 15);
  }

  @Test
  void testRuleWithGap() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func f() {
        print("string literal")
        print("string literal")
        print("string literal")
      }""",
      baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1192");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1192");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Define a constant instead of duplicating this literal \"string literal\" 3 times.");
    TextRangeAssert.assertThat(location.textRange()).hasRange(3, 8, 3, 24);
    assertThat(issue.gap()).isEqualTo(2.0);
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void testCommentedCode() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func main() {
      // func foo() { if (true) {print("string literal");}}
      print (1 == 1);
      print(b);
      // a b c ...
      foo();
      // Coefficients of polynomial
      b = DoubleArray(n); // linear
      c = DoubleArray(n + 1); // quadratic
      d = DoubleArray(n); // cubic
      }""", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S125");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Remove this commented out code.");
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void testNosonarCommentedCode() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func main() {
        // func foo() { if (true) {print("string literal");}} NOSONAR
        print (1 == 1);
        print(b);
        // a b c ...
        foo();
        // Coefficients of polynomial
        b = DoubleArray(n); // linear
        c = DoubleArray(n + 1); // quadratic
        d = DoubleArray(n); // cubic
      }""", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void testSimpleFile() {
    InputFile inputFile = createInputFile("file1.go",
      """
        package main
        func main(x int) {
          print (1 == 1)
          print("abc")
        }
        type A struct {}""", baseDir);
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    assertThat(context.highlightingTypeAt(inputFile.key(), 2, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 2, 4)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(6);
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero();
    assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    assertThat(context.cpdTokens(inputFile.key()).get(1).getValue()).isEqualTo("funcmain(xint){");
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2);

    assertThat(logTester.logs()).contains("1 folder (1 file) to be analyzed");
  }

  @Test
  void testSuppressIssuesInClass() {
    InputFile inputFile = createInputFile("file1.go", """
      @Suppress("slang:S1764")
      class { fun main() {
      print (1 == 1);} }""", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void testSuppressIssuesInVar() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func bar() {
        b = (1 == 1);  // NOSONAR
        c = (1 == 1);
      }
      """, baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    IssueLocation location = issue.primaryLocation();
    TextRangeAssert.assertThat(location.textRange()).hasRange(5, 22, 5, 23);
  }

  @Test
  void testFailInput() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.go", "", baseDir);
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.go");
    assertThat(analysisError.location()).isEqualTo(new DefaultTextPointer(1, 0));

    assertThat(logTester.logs()).isNotEmpty().anyMatch(l -> l.startsWith("Unable to parse directory '"));
  }

  @Test
  void testFailInputWithFailFast() throws IOException {
    context.settings().setProperty("sonar.internal.analysis.failFast", true);
    InputFile inputFile = createInputFile("fakeFile.go", "", baseDir);
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    var sensor = sensor(checkFactory);
    assertThatThrownBy(() -> sensor.execute(context))
      .hasMessage("Cannot read 'fakeFile.go': null")
      .isInstanceOf(ParseException.class);
  }

  @Test
  void testFailParsing() {
    InputFile inputFile = createInputFile("file1.go",
      """
         class A {
         fun x() {}
         fun y() {}\
        """, baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    sensor(checkFactory).execute(context);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S2260");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("A parsing error occurred in this file.");
    TextRangeAssert.assertThat(location.textRange()).hasRange(1, 0, 1, 10);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.go");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isEqualTo(new DefaultTextPointer(1, 2));

    assertThat(logTester.logs()).contains("Unable to parse file: file1.go. file1.go:1:2: expected 'package', found class");
  }

  @Test
  void testFailParsingShouldThrowAnExceptionWhenFailFastIsEnabled() {
    InputFile inputFile = createInputFile("file1.go",
      """
         class A {
         fun x() {}
         fun y() {}\
        """, baseDir);
    context.settings().setProperty("sonar.internal.analysis.failFast", true);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    var sensor = sensor(checkFactory);
    assertThatThrownBy(() -> sensor.execute(context))
      .hasMessage("Exception when analyzing files. See logs above for details.")
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testFailParsingShouldNotThrowAnExceptionWhenFailFastIsDisable() {
    InputFile inputFile = createInputFile("file1.go",
      """
         class A {
         fun x() {}
         fun y() {}\
        """, baseDir);
    context.settings().setProperty("sonar.internal.analysis.failFast", false);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    var sensor = sensor(checkFactory);
    sensor.execute(context);
    assertThat(logTester.logs(Level.WARN)).contains("Unable to parse file: file1.go. file1.go:1:2: expected 'package', found class");
  }

  @Test
  void shouldOnlyLogErrorWithHardFailureWithoutFailFast() {
    var sensor = sensor(checkFactory());
    context.setFileSystem(null);
    sensor.execute(context);

    assertThat(logTester.logs(Level.ERROR)).contains("An error occurred during the analysis of the Go language:");
  }

  @Test
  void testFailParsingWithoutParsingErrorRuleActivated() {
    InputFile inputFile = createInputFile("file1.go", "{", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).hasSize(1);
  }

  @Test
  void testEmptyFile() {
    InputFile inputFile = createInputFile("empty.go", "\t\t  \r\n  \n ", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).isEmpty();
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void testFailureInCheck() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func f() {}""", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = mock(CheckFactory.class);
    var checks = mock(Checks.class);
    GoCheck failingCheck = init -> init.register(TopLevelTree.class, (ctx, tree) -> {
      throw new IllegalStateException("BOUM");
    });
    when(checks.ruleKey(failingCheck)).thenReturn(RuleKey.of(GoRulesDefinition.REPOSITORY_KEY, "failing"));
    // The two following calls are called by "GoChecks".
    when(checkFactory.create(GoRulesDefinition.REPOSITORY_KEY)).thenReturn(checks);
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(failingCheck));
    sensor(checkFactory).execute(context);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(logTester.logs()).contains("Cannot analyse 'file1.go': BOUM");
  }

  @Test
  void testCancellation() {
    InputFile inputFile = createInputFile("file1.go",
      "fun main() {\nprint (1 == 1);}", baseDir);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    context.setCancelled(true);
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void testSonarlintContext() {
    SensorContextTester goContext = SensorContextTester.create(baseDir);
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func main(x int) {
        print (1 == 1)
        print("abc")
      }
      type A struct {}""", baseDir);
    goContext.fileSystem().add(inputFile);
    goContext.setRuntime(SONAR_LINT_RUNTIME);
    sensor(checkFactory("S1764")).execute(goContext);

    assertThat(goContext.allIssues()).hasSize(1);

    // No CPD, highlighting and metrics in SonarLint
    assertThat(goContext.highlightingTypeAt(inputFile.key(), 1, 0)).isEmpty();
    assertThat(goContext.measure(inputFile.key(), CoreMetrics.NCLOC)).isNull();
    assertThat(goContext.cpdTokens(inputFile.key())).isNull();

    assertThat(logTester.logs()).contains("1 folder (1 file) to be analyzed");
  }

  @Test
  void testSensorDescriptorDoesNotProcessFilesIndependently() {
    var sensor = sensor(checkFactory());
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.isProcessesFilesIndependently()).isFalse();
  }

  @Test
  void testSensorLogsWhenUnchangedFilesCanBeSkipped() {
    // Enable PR context
    sensorContext.setCanSkipUnchangedFiles(true);
    sensorContext.setCacheEnabled(true);
    // Execute sensor
    var sensor = sensor(checkFactory());
    sensor.execute(sensorContext);
    assertThat(logTester.logs(Level.INFO)).contains(
      "The Go analyzer is running in a context where unchanged files can be skipped.");
  }

  @Test
  void shouldSkipExecutionIfGoConverterNotInitialized() {
    var goConverterMock = mock(GoConverter.class);
    when(goConverterMock.isInitialized()).thenReturn(false);
    var sensor = new GoSensor(checkFactory(), fileLinesContextFactory, new DefaultNoSonarFilter(), new GoLanguage(new MapSettings().asConfig()), goConverterMock);
    context.setRuntime(SONAR_LINT_RUNTIME);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(Level.INFO))
      .contains("Skipping the Go analysis, parsing is not possible with uninitialized Go converter.");
  }

  @Test
  void shouldLogInfoMessageWhenTestPropertiesAreNotSet() {
    var mainFile = createInputFile("main.go", "package main\nfunc main() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.INFO)).contains(
      """
        The properties "sonar.tests" and "sonar.test.inclusions" are not set. To improve the analysis accuracy, we categorize a file as a test file when the filename has suffix: "_test.go"
          It is highly recommended to set those properties, e.g.: for the Go projects it is usually: "sonar.tests=." and "sonar.test.inclusions=**/*_test.go\"""");
  }

  @Test
  void shouldLogDebugMessageWhenSonarTestsPropertyIsSet() {
    context.settings().setProperty("sonar.tests", ".");

    var mainFile = createInputFile("main.go", "package main\nfunc main() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.DEBUG)).contains("""
      The properties "sonar.tests" and "sonar.test.inclusions" are set: "sonar.tests=." and "sonar.test.inclusions=\"""");
  }

  @Test
  void shouldLogDebugMessageWhenSonarTestInclusionsPropertyIsSet() {
    context.settings().setProperty("sonar.test.inclusions", "**/*_test.go");

    var mainFile = createInputFile("app.go", "package main\nfunc app() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.DEBUG)).contains("""
      The properties "sonar.tests" and "sonar.test.inclusions" are set: "sonar.tests=" and "sonar.test.inclusions=**/*_test.go\"""");
  }

  @Test
  void shouldLogDebugMessageWhenBothPropertiesAreSet() {
    context.settings().setProperty("sonar.tests", ".");
    context.settings().setProperty("sonar.test.inclusions", "**/*_test.go");

    var mainFile = createInputFile("code.go", "package main\nfunc code() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.DEBUG)).contains("""
      The properties "sonar.tests" and "sonar.test.inclusions" are set: "sonar.tests=." and "sonar.test.inclusions=**/*_test.go\"""");
  }

  @Test
  void shouldHandleEmptyStringPropertiesAsSameAsNotSet() {
    context.settings().setProperty("sonar.tests", "");
    context.settings().setProperty("sonar.test.inclusions", "");

    var mainFile = createInputFile("main.go", "package main\nfunc main() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.INFO)).contains(
      """
        The properties "sonar.tests" and "sonar.test.inclusions" are not set. To improve the analysis accuracy, we categorize a file as a test file when the filename has suffix: "_test.go"
          It is highly recommended to set those properties, e.g.: for the Go projects it is usually: "sonar.tests=." and "sonar.test.inclusions=**/*_test.go\"""");
  }

  @Test
  void shouldHandleWhitespaceOnlyPropertiesAsSameAsNotSet() {
    context.settings().setProperty("sonar.tests", "   ");
    context.settings().setProperty("sonar.test.inclusions", "\t");

    var mainFile = createInputFile("main.go", "package main\nfunc main() {}", baseDir);
    context.fileSystem().add(mainFile);

    sensor(checkFactory()).execute(context);

    assertThat(logTester.logs(Level.INFO)).contains(
      """
        The properties "sonar.tests" and "sonar.test.inclusions" are not set. To improve the analysis accuracy, we categorize a file as a test file when the filename has suffix: "_test.go"
          It is highly recommended to set those properties, e.g.: for the Go projects it is usually: "sonar.tests=." and "sonar.test.inclusions=**/*_test.go\"""");
  }

  private GoSensor sensor(List<Class<?>> checks) {
    ActiveRulesBuilder rulesBuilder = new ActiveRulesBuilder();

    for (Class<?> check : checks) {
      RuleKey ruleKey = RuleKey.of(GoRulesDefinition.REPOSITORY_KEY, ((Rule) check.getAnnotations()[0]).key());
      NewActiveRule.Builder newActiveRuleBuilder = new NewActiveRule.Builder()
        .setRuleKey(ruleKey);
      rulesBuilder.addRule(newActiveRuleBuilder.build());
    }

    ActiveRules activeRules = rulesBuilder.build();
    CheckFactory checkFactory = new CheckFactory(activeRules);
    Checks<GoCheck> instantiatedChecks = checkFactory.create(GoRulesDefinition.REPOSITORY_KEY);
    instantiatedChecks.addAnnotatedChecks(checks);
    return new GoSensor(new GoTestChecks(instantiatedChecks), fileLinesContextFactory, new DefaultNoSonarFilter(),
      new GoLanguage(new MapSettings().asConfig()), singleInstanceGoConverter);
  }

  private GoSensor sensor(String... ruleKeys) {
    var checkFactory = checkFactory(ruleKeys);
    return sensor(checkFactory);
  }

  private GoSensor sensor(CheckFactory checkFactory) {
    return new GoSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(),
      new GoLanguage(new MapSettings().asConfig()), singleInstanceGoConverter);
  }

  protected CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      NewActiveRule newRule = new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(GoRulesDefinition.REPOSITORY_KEY, ruleKey))
        .setName(ruleKey)
        .build();
      builder.addRule(newRule);
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  // help classes

  static class FailingToReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return false;
    }
  }

  private static class GoTestChecks extends GoChecks {
    GoTestChecks(Checks<GoCheck> checks) {
      super(null);
      this.checksByRepository.add(checks);
    }
  }

  private static class FileLinesContextTester implements FileLinesContext {
    int saveCount = 0;
    Map<String, Set<String>> metrics = new HashMap<>();

    @Override
    public void setIntValue(String metricKey, int line, int value) {
      setStringValue(metricKey, line, String.valueOf(value));
    }

    @Override
    public void setStringValue(String metricKey, int line, String value) {
      metrics.computeIfAbsent(metricKey, key -> new HashSet<>())
        .add(line + ":" + value);
    }

    @Override
    public void save() {
      saveCount++;
    }
  }
}
