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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.slang.testing.ThreadLocalLogTester;
import org.sonar.go.converter.GoConverter;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GoSensorTest {

  private Path workDir;
  private GoConverter singleInstanceGoConverter;
  private Path projectDir;
  private SensorContextTester sensorContext;
  private FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
  private FileLinesContextTester fileLinesContext;

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @BeforeEach
  void setUp() throws IOException {
    workDir = Files.createTempDirectory("gotest");
    workDir.toFile().deleteOnExit();
    singleInstanceGoConverter = new GoConverter(workDir.toFile());
    projectDir = Files.createTempDirectory("gotestProject");
    projectDir.toFile().deleteOnExit();
    sensorContext = SensorContextTester.create(workDir);
    sensorContext.fileSystem().setWorkDir(workDir);
    sensorContext.settings().setProperty("sonar.slang.converter.validation", "throw");
    fileLinesContext = new FileLinesContextTester();
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
  }

  @Test
  void test_description() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    getSensor("S2068").describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Code Quality and Security for Go");
    assertThat(descriptor.languages()).containsOnly("go");
  }

  @Test
  void test_issue() throws IOException {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      "package main \n" +
        "\n" +
        "func test() {\n" +
        " pwd := \"secret\"\n" +
        "}");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor("S2068");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_file_issue() throws IOException {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      "// TODO implement the logic \n package main \n");
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(sensorContext.allIssues()).hasSize(1);
  }

  @Test
  void test_line_issue() throws IOException {
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
      "package main \n" +
        "\n" +
        "func test() {\n" +
        " pwd := \"secret\"\n" +
        "}");
    failingFile = spy(failingFile);
    doThrow(new IOException("The file is corrupted")).when(failingFile).contents();

    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).contains("Cannot read 'lets.go': The file is corrupted");
  }

  @Test
  void test_empty_file() throws Exception {
    InputFile failingFile = createInputFile("lets.go", InputFile.Type.MAIN, "");
    sensorContext.fileSystem().add(failingFile);
    GoSensor goSensor = getSensor("S1135");
    goSensor.execute(sensorContext);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void metrics() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      /* 01 */"// This is not a line of code\n" +
      /* 02 */"package main\n" +
      /* 03 */"import \"fmt\"\n" +
      /* 04 */"type class1 struct { x, y int }\n" +
      /* 05 */"type class2 struct { a, b string }\n" +
      /* 06 */"type anyObject interface {}\n" +
      /* 07 */"func fun1() {\n" +
      /* 08 */"  fmt.Println(\"Statement 1\")\n" +
      /* 09 */"}\n" +
      /* 10 */"func fun2(i int) {\n" +
      /* 11 */"  switch i { // Statement 2\n" +
      /* 12 */"  case 2:\n" +
      /* 13 */"    fmt.Println(\n" +
      /* 14 */"      \"Not a Statement 3\",\n" +
      /* 15 */"    )\n" +
      /* 16 */"  }\n" +
      /* 17 */"}\n" +
      /* 18 */"func fun3(x interface{}) int {\n" +
      /* 19 */"  return 42 // Statement 4\n" +
      /* 20 */"}\n");
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
      "package awesomeProject\n" +
        "const a = \"a\"\n" +
        "var myVar int = 12\n" +
        "var i, j int = 1, 2\n" +
        "var c, c1, c2, c3, c4 int\n" +
        "const (\n" +
        "\tUpdate = \"update\"\n" +
        "\tDelete = \"delete\"\n" +
        ")\n" +
        "type Message struct {\n" +
        "}\n" +
        "type (\n" +
        "\tRrsType string\n" +
        ")\n"
    );
    sensorContext.fileSystem().add(inputFile);
    GoSensor goSensor = getSensor();
    goSensor.execute(sensorContext);

    assertThat(sensorContext.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(14);
    assertThat(fileLinesContext.metrics.get(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)).isNull();
  }

  @Test
  void metrics_for_test_file() {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.TEST,
      "// This is not a line of code\n" +
        "package main\n" +
        "import \"fmt\"\n" +
        "func main() {\n" +
        "  fmt.Println(\"Hello\")\n" +
        "}\n");
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
        "package main\n" +
        "import \"fmt\"\n" +
        "func fun1(i int) int {\n" +
        "  if i < 0 { // +1\n" +
        "    i++\n" +
        "  }\n" +
        "  return i\n" +
        "}\n" +
        "func fun2(i int) int {\n" +
        "  if i < 0 { // +1\n" +
        "    i--\n" +
        "  }\n" +
        "  f := func(int) int {\n" +
        "    if i < 0 { // +2 (incl 1 for nesting)\n" +
        "      i++\n" +
        "    }\n" +
        "    return i\n" +
        "  }\n" +
        "  return i + f(i)\n" +
        "}\n" +
        "\n");
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
  void highlighting() throws Exception {
    InputFile inputFile = createInputFile("lets.go", InputFile.Type.MAIN,
      "//abc\n" +
        "/*x*/\n" +
        "package main\n" +
        "import \"fmt\"\n" +
        "type class1 struct { }\n" +
        "func fun2(x string) int {\n" +
        "  return 42\n" +
        "}\n");
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
    List<String> allKeys = ruleClasses.stream().map(ruleClass -> ((org.sonar.check.Rule) ruleClass.getAnnotations()[0]).key()).collect(Collectors.toList());
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
    Checks<SlangCheck> checks = checkFactory.create(GoRulesDefinition.REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable) ruleClasses);
    return new GoSensor(AbstractSensorTest.SQ_LTS_RUNTIME, checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(),
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
