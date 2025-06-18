/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
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
import org.sonar.go.checks.GoCheckList;
import org.sonar.go.converter.GoConverter;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GoSensorTest {

  private static final SonarRuntime SQ_LTS_RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.DEVELOPER);
  private GoConverter singleInstanceGoConverter;
  private Path projectDir;
  private SensorContextTester sensorContext;
  private final FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
  private FileLinesContextTester fileLinesContext;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  void setUp() throws IOException {
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
  void test_description() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    getSensor("S1110").describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Code Quality and Security for Go");
    assertThat(descriptor.languages()).containsOnly("go");
  }

  @Test
  void test_issue() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void test_file_issue() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      "// TODO implement the logic \n package main \n");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_line_issue() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      "package                                                                                                                                                                                                                               main\n");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor("S103");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_failure() throws Exception {
    InputFile failingFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main\s

        func test() {
         pwd := "secret"
        }""");
    failingFile = spy(failingFile);
    doThrow(new IOException("The file is corrupted")).when(failingFile).contents();

    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).contains("Unable to parse file.");
  }

  @Test
  void test_empty_file() {
    InputFile failingFile = createInputFile("lets.go", InputFile.Type.MAIN, "");
    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void metrics() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN, """
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
      }""");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
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
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
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
        """);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
    goSensor.execute(sensorContext);

    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(14);
    assertThat(fileLinesContext.metrics.get(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)).isNull();
  }

  @Test
  void metrics_for_test_file() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.TEST,
      """
        // This is not a line of code
        package main
        import "fmt"
        func main() {
          fmt.Println("Hello")
        }
        """);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC)).isNull();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COMMENT_LINES)).isNull();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.CLASSES)).isNull();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.FUNCTIONS)).isNull();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.STATEMENTS)).isNull();
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COGNITIVE_COMPLEXITY)).isNull();

    assertThat(fileLinesContext.saveCount).isZero();
    assertThat(fileLinesContext.metrics.keySet()).isEmpty();
  }

  @Test
  void cognitive_complexity_metric() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
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

        """);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
    goSensor.execute(sensorContext);
    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.COGNITIVE_COMPLEXITY).value()).isEqualTo(4);
  }

  @Test
  void always_use_the_same_ast_converter() {
    GoSensor goSensor = getSensor();
    assertThat(goSensor.astConverter(sensorContext)).isSameAs(singleInstanceGoConverter);
    assertThat(goSensor.astConverter(sensorContext)).isSameAs(singleInstanceGoConverter);
  }

  @Test
  void highlighting() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        //abc
        /*x*/
        package main
        import "fmt"
        type class1 struct { }
        func fun2(x string) int {
          return 42
        }
        """);
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
    goSensor.execute(sensorContext);

    String componentKey = "module:lets.go";
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
    assertThat(getSensor().repositoryKey()).isEqualTo("go");
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
    InputFile goModFile = createInputFile("go.mod", InputFile.Type.MAIN,
      """
        module myModule

        go 1.23.4
        """);

    InputFile goFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main
        var a int
        """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = getSensorWithCustomChecks(Set.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void versionShouldNotBeDetectedOnMissingVersion() {
    InputFile goModFile = createInputFile("go.mod", InputFile.Type.MAIN,
      """
        module myModule
        """);

    InputFile goFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main
        var a int
        """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = getSensorWithCustomChecks(Set.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void versionShouldNotBeDetectedOnMissingGoModFile() {
    InputFile goFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main
        var a int
        """);

    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = getSensorWithCustomChecks(Set.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void versionShouldNotBeDetectedInSonarQubeIDEContext() {
    sensorContext.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(10, 18)));
    InputFile goModFile = createInputFile("go.mod", InputFile.Type.MAIN,
      """
        module myModule

        go 1.23.4
        """);

    InputFile goFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main
        var a int
        """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goFile);
    GoSensor goSensor = getSensorWithCustomChecks(Set.of(GoVersionCheck.class));
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).isEmpty();
  }

  @Test
  void shouldRaiseIssueOnConverterLogValidation() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""");
    sensorContext.fileSystem().add(inputFile);
    sensorContext.settings().setProperty("sonar.go.converter.validation", "log");
    GoSensor goSensor = getSensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void shouldRaiseIssueOnWhenConverterPropertyIsInvalid() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main\s

        func test() {
         x := ((2 + 3))
        }""");
    sensorContext.fileSystem().add(inputFile);
    sensorContext.settings().setProperty("sonar.go.converter.validation", "invalid");
    GoSensor goSensor = getSensor("S1110");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).contains("Unsupported mode for converter validation: 'invalid', falling back to no validation");
  }

  @ParameterizedTest
  @CsvSource({
    GoSensor.FAIL_FAST_PROPERTY_NAME + ",true,true",
    GoSensor.FAIL_FAST_PROPERTY_NAME + ",false,false",
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

    GoSensor sensor = getSensorWithCustomChecks(Set.of(CheckRegisteringOnLeave1.class, CheckRegisteringOnLeave2.class));
    List<GoCheck> allChecks = sensor.checks().all();
    assertThat(allChecks).hasSize(2);
    ((CheckRegisteringOnLeave) allChecks.get(0)).setConsumerToRegister(consumer1);
    ((CheckRegisteringOnLeave) allChecks.get(1)).setConsumerToRegister(consumer2);

    InputFile goFile = createInputFile("lets.go", InputFile.Type.MAIN,
      """
        package main
        var a int
        """);
    sensorContext.fileSystem().add(goFile);

    sensor.execute(sensorContext);

    Mockito.verify(consumer1, Mockito.times(1)).accept(any(), any());
    Mockito.verify(consumer2, Mockito.times(1)).accept(any(), any());
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

  private GoSensor getSensor(String... activeRuleArray) {
    Set<String> activeRuleSet = new HashSet<>(Arrays.asList(activeRuleArray));
    List<Class<?>> ruleClasses = GoCheckList.checks();
    List<String> allKeys = ruleClasses.stream().map(ruleClass -> ((org.sonar.check.Rule) ruleClass.getAnnotations()[0]).key()).toList();
    ActiveRulesBuilder rulesBuilder = new ActiveRulesBuilder();
    allKeys.forEach(key -> {
      if (activeRuleSet.contains(key)) {
        NewActiveRule.Builder newActiveRuleBuilder = new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(GoRulesDefinition.REPOSITORY_KEY, key));
        if (key.equals("S1451")) {
          newActiveRuleBuilder.setParam("headerFormat", "some header format");
        }
        rulesBuilder.addRule(newActiveRuleBuilder.build());
      }
    });
    ActiveRules activeRules = rulesBuilder.build();
    CheckFactory checkFactory = new CheckFactory(activeRules);
    Checks<GoCheck> checks = checkFactory.create(GoRulesDefinition.REPOSITORY_KEY);
    checks.addAnnotatedChecks(ruleClasses);
    return new GoSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(),
      new GoLanguage(new MapSettings().asConfig()), singleInstanceGoConverter);
  }

  private GoSensor getSensorWithCustomChecks(Set<Class<?>> checks) {
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
    return new GoSensor(new GoChecksTest(instantiatedChecks), fileLinesContextFactory, new DefaultNoSonarFilter(),
      new GoLanguage(new MapSettings().asConfig()), singleInstanceGoConverter);
  }

  private InputFile createInputFile(String filename, InputFile.Type type, String content) {
    Path filePath = projectDir.resolve(filename);
    return TestInputFileBuilder.create("module", projectDir.toFile(), filePath.toFile())
      .setCharset(UTF_8)
      .setLanguage(GoLanguage.KEY)
      .setContents(content)
      .setType(type)
      .build();
  }

  private static class GoChecksTest extends GoChecks {
    GoChecksTest(Checks<GoCheck> checks) {
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
