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
package org.sonar.go.externalreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.externalreport.ExternalLinterSensorHelper.REPORT_BASE_PATH;

class GolangCILintReportSensorTest {

  private final List<String> analysisWarnings = new ArrayList<>();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @Test
  void shouldDefineDescriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    golangCILintReportSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of GolangCI-Lint issues");
    assertThat(sensorDescriptor.languages()).containsOnly("go");
  }

  private GolangCILintReportSensor golangCILintReportSensor() {
    return new GolangCILintReportSensor(analysisWarnings::add);
  }

  @Test
  void shouldImportIssuesWithSonarQube() throws IOException {
    var context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.golangci-lint.reportPaths", REPORT_BASE_PATH.resolve("golangci-lint-report.xml").toString());
    var externalIssues = ExternalLinterSensorHelper.executeSensor(golangCILintReportSensor(), context);
    assertThat(externalIssues).hasSize(4);

    var first = externalIssues.get(0);
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(first.ruleKey().rule()).isEqualTo("deadcode.bug.major");
    assertThat(first.primaryLocation().message()).isEqualTo("`three` is unused");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    var second = externalIssues.get(1);
    assertThat(second.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(second.ruleKey().rule()).isEqualTo("gosec");
    assertThat(second.primaryLocation().message()).isEqualTo("G402: TLS InsecureSkipVerify set true.");
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("module:main.go");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(4);

    var third = externalIssues.get(2);
    assertThat(third.type()).isEqualTo(RuleType.BUG);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.ruleKey().repository()).isEqualTo("external_govet");
    assertThat(third.ruleKey().rule()).isEqualTo("assign");
    assertThat(third.primaryLocation().message()).isEqualTo("assign: self-assignment of name to name");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(5);

    var fourth = externalIssues.get(3);
    assertThat(fourth.type()).isEqualTo(RuleType.BUG);
    assertThat(fourth.severity()).isEqualTo(Severity.MAJOR);
    assertThat(fourth.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(fourth.ruleKey().rule()).isEqualTo("govet.bug.major");
    assertThat(fourth.primaryLocation().message()).isEqualTo("Non-conventional message format");
    assertThat(fourth.primaryLocation().textRange().start().line()).isEqualTo(6);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void shouldImportIssuesWithDifferentKeysFromTheSameSource() throws IOException {
    // Check that rules have different key based on the severity
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.golangci-lint.reportPaths", REPORT_BASE_PATH.resolve("checkstyle-different-severity.xml").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(golangCILintReportSensor(), context);
    assertThat(externalIssues).hasSize(6);

    assertThat(externalIssues.get(0).ruleKey().rule()).isEqualTo("source1.bug.major");
    assertThat(externalIssues.get(1).ruleKey().rule()).isEqualTo("source1.code_smell.minor");
    assertThat(externalIssues.get(2).ruleKey().rule()).isEqualTo("source1.code_smell.major");
    assertThat(externalIssues.get(3).ruleKey().rule()).isEqualTo("source1.code_smell.major");
    assertThat(externalIssues.get(4).ruleKey().rule()).isEqualTo("source2.bug.major");
    assertThat(externalIssues.get(5).ruleKey().rule()).isEqualTo("source2.code_smell.major");
  }
}
