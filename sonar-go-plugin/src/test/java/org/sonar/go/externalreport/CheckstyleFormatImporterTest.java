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
package org.sonar.go.externalreport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class CheckstyleFormatImporterTest {

  static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "CheckstyleFormatImporterTest");

  static final String LINTER_KEY = "test-linter";

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void shouldImportDetektIssues() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("detekt-checkstyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("CheckstyleFormatImporterTest-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("detekt.EmptyIfBlock");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.impacts()).isEmpty();
    assertThat(first.primaryLocation().message()).isEqualTo("This empty block of code can be removed.");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("CheckstyleFormatImporterTest-project:main.kt");
    assertThat(second.ruleKey().rule()).isEqualTo("detekt.MagicNumber");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.impacts()).isEmpty();
    assertThat(second.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(second.primaryLocation().message()).isEqualTo("This expression contains a magic number. Consider defining it to a well named constant.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("CheckstyleFormatImporterTest-project:A.kt");
    assertThat(third.ruleKey().rule()).isEqualTo("detekt.EqualsWithHashCodeExist");
    assertThat(third.type()).isEqualTo(RuleType.BUG);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.impacts()).isEmpty();
    assertThat(third.primaryLocation().message()).isEqualTo("A class should always override hashCode when overriding equals and the other way around.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void shouldImportGolangCILintIssues() throws IOException {
    List<ExternalIssue> externalIssues = importIssuesTestImporter("golangci-lint-report.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("CheckstyleFormatImporterTest-project:TabCharacter.go");
    assertThat(first.ruleKey().rule()).isEqualTo("deadcode");
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("`three` is unused");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(4);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "invalid-path.txt",
    "not-checkstyle-file.xml",
    "invalid-file.xml",
  })
  void shouldNotImportIssuesWhenReportDoesNotExist(String fileName) throws IOException {
    List<ExternalIssue> externalIssues = importIssues(fileName);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith(fileName + "' can't be read.");
  }

  @Test
  void shouldImportIssuesWhenReportHasErrors() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("detekt-checkstyle-with-errors.xml");
    assertThat(externalIssues).hasSize(2);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("CheckstyleFormatImporterTest-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("detekt.UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.impacts()).isEmpty();
    assertThat(first.primaryLocation().message()).isEqualTo("Error at file level with an unknown rule key.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).containsExactlyInAnyOrder(
      "No input file found for not-existing-file.kt. No test-linter issues will be imported on this file.");
    assertThat(logTester.logs(Level.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected error without any message for rule: 'detekt.EmptyIfBlock'");
  }

  private List<ExternalIssue> importIssues(String fileName) throws IOException {
    SensorContextTester context = createContext();
    CheckstyleFormatImporter importer = new CheckstyleFormatImporter(context, LINTER_KEY);
    importer.importFile(PROJECT_DIR.resolve(fileName).toAbsolutePath().toFile());
    return new ArrayList<>(context.allExternalIssues());
  }

  private List<ExternalIssue> importIssuesTestImporter(String fileName) throws IOException {
    SensorContextTester context = createContext();
    CheckstyleFormatImporter importer = new TestImporter(context, LINTER_KEY);
    importer.importFile(PROJECT_DIR.resolve(fileName).toAbsolutePath().toFile());
    return new ArrayList<>(context.allExternalIssues());
  }

  static SensorContextTester createContext() throws IOException {
    SensorContextTester context = SensorContextTester.create(PROJECT_DIR);
    Files.list(PROJECT_DIR)
      .filter(file -> !Files.isDirectory(file))
      .forEach(file -> addFileToContext(context, PROJECT_DIR, file));
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(file.toString().substring(file.toString().lastIndexOf('.') + 1))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static String onlyOneLogElement(List<String> elements) {
    assertThat(elements).hasSize(1);
    return elements.get(0);
  }

  static class TestImporter extends CheckstyleFormatImporter {

    public TestImporter(SensorContext context, String linterKey) {
      super(context, linterKey);
    }
  }
}
