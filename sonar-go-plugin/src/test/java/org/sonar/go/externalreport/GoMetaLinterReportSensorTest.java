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
import static org.sonar.go.externalreport.AbstractReportSensor.GENERIC_ISSUE_KEY;
import static org.sonar.go.externalreport.ExternalLinterSensorHelper.REPORT_BASE_PATH;

class GoMetaLinterReportSensorTest {

  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    goMetaLinterReportSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of GoMetaLinter issues");
    assertThat(sensorDescriptor.languages()).containsOnly("go");
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.gometalinter.reportPaths", REPORT_BASE_PATH.resolve("gometalinter-report.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goMetaLinterReportSensor(), context);
    assertThat(externalIssues).hasSize(3);

    org.sonar.api.batch.sensor.issue.ExternalIssue first = externalIssues.get(0);
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.ruleKey().repository()).isEqualTo("external_govet");
    assertThat(first.ruleKey().rule()).isEqualTo("assign");
    assertThat(first.primaryLocation().message()).isEqualTo("self-assignment of name to name");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(1);

    org.sonar.api.batch.sensor.issue.ExternalIssue second = externalIssues.get(1);
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.ruleKey().repository()).isEqualTo("external_interfacer");
    assertThat(second.ruleKey().rule()).isEqualTo("issue");
    assertThat(second.primaryLocation().message()).isEqualTo("other (declaration) of ascii_allowed");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(2);

    org.sonar.api.batch.sensor.issue.ExternalIssue third = externalIssues.get(2);
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.ruleKey().repository()).isEqualTo("external_golint");
    assertThat(third.ruleKey().rule()).isEqualTo("Exported");
    assertThat(third.primaryLocation().message()).isEqualTo("exported type User should have comment or be unexported");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void no_issues_with_invalid_report_line() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.gometalinter.reportPaths", REPORT_BASE_PATH.resolve("gometalinter-report-with-error.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goMetaLinterReportSensor(), context);
    assertThat(externalIssues).hasSize(1);
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).hasSize(1);
    assertThat(logTester.logs(Level.DEBUG).get(0)).startsWith("GoMetaLinterReportSensor: Unexpected line: invalid-invalid-invalid");
  }

  @Test
  void should_parse_gometalinter_report_warning_line() {
    String line = "SelfAssignement.go:4:6:warning: exported type User should have comment or be unexported (golint)";
    org.sonar.go.externalreport.ExternalIssue issue = goMetaLinterReportSensor().parse(line);
    assertThat(issue).isNotNull();
    assertThat(issue.linter()).isEqualTo("golint");
    assertThat(issue.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(issue.ruleKey()).isEqualTo("Exported");
    assertThat(issue.filename()).isEqualTo("SelfAssignement.go");
    assertThat(issue.lineNumber()).isEqualTo(4);
    assertThat(issue.message()).isEqualTo("exported type User should have comment or be unexported");
  }

  @Test
  void should_parse_gometalinter_report_error_line() {
    String line = "duplication/pivot.go:14:5:error: ascii_allowed redeclared in this block (gotype)";
    org.sonar.go.externalreport.ExternalIssue issue = goMetaLinterReportSensor().parse(line);
    assertThat(issue).isNotNull();
    assertThat(issue.linter()).isEqualTo("gotype");
    assertThat(issue.type()).isEqualTo(RuleType.BUG);
    assertThat(issue.ruleKey()).isEqualTo(GENERIC_ISSUE_KEY);
    assertThat(issue.filename()).isEqualTo("duplication/pivot.go");
    assertThat(issue.lineNumber()).isEqualTo(14);
    assertThat(issue.message()).isEqualTo("ascii_allowed redeclared in this block");
  }

  @Test
  void should_parse_gometalinter_report_error_line_with_rulekey() {
    String line = "SelfAssignement.go:6:19:warning: func (*User).rename is unused (U1000) (megacheck)";
    org.sonar.go.externalreport.ExternalIssue issue = goMetaLinterReportSensor().parse(line);
    assertThat(issue).isNotNull();
    assertThat(issue.linter()).isEqualTo("megacheck");
    assertThat(issue.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(issue.ruleKey()).isEqualTo("U1000");
    assertThat(issue.filename()).isEqualTo("SelfAssignement.go");
    assertThat(issue.lineNumber()).isEqualTo(6);
    assertThat(issue.message()).isEqualTo("func (*User).rename is unused");
  }

  @Test
  void should_parse_gometalinter_report_error_line_with_invalid_rulekey() {
    String line = "SelfAssignement.go:6:19:warning: func (*User).rename is unused (Not a rule key) (megacheck)";
    org.sonar.go.externalreport.ExternalIssue issue = goMetaLinterReportSensor().parse(line);
    assertThat(issue).isNotNull();
    assertThat(issue.linter()).isEqualTo("megacheck");
    assertThat(issue.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(issue.ruleKey()).isEqualTo(GENERIC_ISSUE_KEY);
    assertThat(issue.filename()).isEqualTo("SelfAssignement.go");
    assertThat(issue.lineNumber()).isEqualTo(6);
    assertThat(issue.message()).isEqualTo("func (*User).rename is unused (Not a rule key)");
  }

  private GoMetaLinterReportSensor goMetaLinterReportSensor() {
    return new GoMetaLinterReportSensor(analysisWarnings::add);
  }
}
