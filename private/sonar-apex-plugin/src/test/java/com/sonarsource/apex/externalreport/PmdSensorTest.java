/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.externalreport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rules.RuleType;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class PmdSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "pmd");
  private static final String PROJECT_ID = "pmd-test";

  private PmdSensor sensor;
  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
    sensor = new PmdSensor(analysisWarnings::add);
  }

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of PMD issues");
    MapSettings settings = new MapSettings();
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isFalse();
    settings.setProperty(PmdSensor.REPORT_PROPERTY_KEY, "report.xml");
    assertThat(sensorDescriptor.configurationPredicate().test(settings.asConfig())).isTrue();
  }

  @Test
  void no_report_path_set() throws IOException {
    List<ExternalIssue> externalIssues = execute(null);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    List<String> warnings = logTester.logs(Level.WARN);
    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0))
      .startsWith("Unable to import PMD report file(s):")
      .contains("invalid-path.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.apex.pmd.reportPaths' is correctly configured.");
    assertThat(analysisWarnings).hasSize(1);
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 1 PMD report file(s).")
      .endsWith("Please check that property 'sonar.apex.pmd.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void not_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = execute("hello.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(Level.ERROR).get(0)).matches("Can't read PMD XML report: .*hello.txt");
  }

  @Test
  void skip_issue_on_invalid_priority() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-severity.xml");
    assertThat(externalIssues).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0))
      .contains("Can't import issue at line 8")
      .contains("invalid-severity.xml");
    assertThat(logTester.logs(Level.WARN).get(1))
      .contains("Can't import issue at line 9")
      .contains("invalid-severity.xml");
  }

  @Test
  void invalid_text_range() throws IOException {
    List<ExternalIssue> externalIssues = execute("invalid-text-range.xml");
    assertThat(externalIssues).hasSize(2);
    TextRange secondIssueRange = externalIssues.get(1).primaryLocation().textRange();
    assertThat(secondIssueRange).isNotNull();
    assertThat(secondIssueRange.start().line()).isEqualTo(1);
    assertThat(secondIssueRange.end().line()).isEqualTo(1);
    assertThat(logTester.logs(Level.WARN).get(0))
      .contains("Can't import issue at line 9")
      .contains("invalid-text-range.xml");
  }

  @Test
  void issues() throws IOException {
    List<ExternalIssue> externalIssues = execute("pmd-report.xml");
    assertThat(externalIssues).hasSize(7);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file1.cls");
    assertThat(first.ruleKey().rule()).isEqualTo("AvoidGlobalModifier");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Avoid using global modifier");
    assertThat(first.primaryLocation().textRange()).isEqualTo(
      new DefaultTextRange(new DefaultTextPointer(1, 0), new DefaultTextPointer(1, 19)));
    assertThat(first.remediationEffort()).isEqualTo(5);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file1.cls");
    assertThat(third.ruleKey().rule()).isEqualTo("EmptyStatementBlock");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.primaryLocation().message()).isEqualTo("Avoid empty block statements.");
    assertThat(third.primaryLocation().textRange()).isEqualTo(
      new DefaultTextRange(new DefaultTextPointer(2, 19), new DefaultTextPointer(2, 22)));
    assertThat(third.remediationEffort()).isEqualTo(5);

    ExternalIssue last = externalIssues.get(6);
    assertThat(last.primaryLocation().inputComponent().key()).isEqualTo(PROJECT_ID + ":file2.cls");
    assertThat(last.ruleKey().rule()).isEqualTo("OneDeclarationPerLine");
    assertThat(last.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(last.severity()).isEqualTo(Severity.MAJOR);
    assertThat(last.primaryLocation().message()).isEqualTo("Use one statement for each line, it enhances code readability.");
    assertThat(last.primaryLocation().textRange()).isEqualTo(
      new DefaultTextRange(new DefaultTextPointer(4, 13), new DefaultTextPointer(4, 29)));
    assertThat(last.remediationEffort()).isEqualTo(5);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).containsExactly("No input file found for unknown-file.cls. No PMD issue will be imported on this file.");
  }

  private List<ExternalIssue> execute(@Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty(PmdSensor.REPORT_PROPERTY_KEY, path);
    }
    sensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  public static SensorContextTester createContext(Path projectDir) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);
    Files.list(projectDir)
      .filter(file -> !Files.isDirectory(file))
      .forEach(file -> addFileToContext(context, projectDir, file));
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      context.fileSystem().add(TestInputFileBuilder.create(PROJECT_ID, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(language(file))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String language(Path file) {
    String path = file.toString();
    return path.substring(path.lastIndexOf('.') + 1);
  }
}
