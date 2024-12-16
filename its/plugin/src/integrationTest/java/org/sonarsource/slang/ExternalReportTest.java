/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.slang;

import com.sonar.orchestrator.build.SonarScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ExternalReportTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/externalreport/";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void govet() {
    final String projectKey = "govet";

    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "govet");
    sonarScanner.setProperty("sonar.go.govet.reportPaths", "go-vet.out");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(2);
    assertThat(formatIssues(issues)).isEqualTo(
      "SelfAssignement.go|external_govet:assign|MAJOR|5min|line:7|self-assignment of name to name\n" +
        "SelfAssignement.go|external_govet:assign|MAJOR|5min|line:9|self-assignment of user.name to user.name");
  }

  @Test
  public void golint() {
    final String projectKey = "golint";

    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "golint");
    sonarScanner.setProperty("sonar.go.golint.reportPaths", "golint.out");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(11);
    assertThat(formatIssues(issues)).isEqualTo(
      "SelfAssignement.go|external_golint:Exported|MAJOR|5min|line:4|exported type User should have comment or be unexported\n" +
        "SelfAssignement.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "TabCharacter.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "TodoTagPresence.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "TooLongLine.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "TooManyParameters.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "pivot.go|external_golint:Names|MAJOR|5min|line:10|don't use underscores in Go names; var ascii_uppercase should be asciiUppercase\n" +
        "pivot.go|external_golint:Names|MAJOR|5min|line:11|don't use underscores in Go names; var ascii_lowercase should be asciiLowercase\n" +
        "pivot.go|external_golint:Names|MAJOR|5min|line:12|don't use underscores in Go names; var ascii_uppercase_len should be asciiUppercaseLen\n" +
        "pivot.go|external_golint:Names|MAJOR|5min|line:13|don't use underscores in Go names; var ascii_lowercase_len should be asciiLowercaseLen\n" +
        "pivot.go|external_golint:Names|MAJOR|5min|line:14|don't use underscores in Go names; var ascii_allowed should be asciiAllowed");
  }

  @Test
  public void gometalinter() {
    final String projectKey = "gometalinter";

    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "gometalinter");
    sonarScanner.setProperty("sonar.go.gometalinter.reportPaths", "gometalinter.out");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(8);
    assertThat(formatIssues(issues)).isEqualTo(
      "SelfAssignement.go|external_golint:Exported|MAJOR|5min|line:4|exported type User should have comment or be unexported\n" +
        "SelfAssignement.go|external_golint:PackageComment|MAJOR|5min|line:1|package comment should be of the form \"Package samples ...\"\n" +
        "SelfAssignement.go|external_govet:assign|MAJOR|5min|line:7|self-assignment of name to name\n" +
        "SelfAssignement.go|external_govet:assign|MAJOR|5min|line:9|self-assignment of user.name to user.name\n" +
        "SelfAssignement.go|external_megacheck:SA4018|MAJOR|5min|line:7|self-assignment of name to name\n" +
        "SelfAssignement.go|external_megacheck:SA4018|MAJOR|5min|line:9|self-assignment of user.name to user.name\n" +
        "SelfAssignement.go|external_megacheck:U1000|MAJOR|5min|line:4|field name is unused\n" +
        "SelfAssignement.go|external_megacheck:U1000|MAJOR|5min|line:6|func (*User).rename is unused");
  }

  @Test
  public void golangcilint() {
    final String projectKey = "golangcilint";

    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY, "golangci-lint");
    sonarScanner.setProperty("sonar.go.golangci-lint.reportPaths", "golangci-lint-checkstyle.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(4);
    assertThat(formatIssues(issues)).isEqualTo(
      "SelfAssignement.go|external_golangci-lint:govet.bug.major|MAJOR|5min|line:7|assign: self-assignment of name to name\n" +
        "SelfAssignement.go|external_golangci-lint:govet.bug.major|MAJOR|5min|line:9|assign: self-assignment of user.name to user.name\n" +
        "SelfAssignement.go|external_golangci-lint:unused.bug.major|MAJOR|5min|line:4|U1000: field `name` is unused\n" +
        "SelfAssignement.go|external_golangci-lint:unused.bug.major|MAJOR|5min|line:6|U1000: func `(*User).rename` is unused");
  }

  private List<Issue> getExternalIssues(String componentKey) {
    return newWsClient().issues().search(new SearchRequest().setComponentKeys(Collections.singletonList(componentKey)))
      .getIssuesList().stream()
      .filter(issue -> issue.getRule().startsWith("external_"))
      .collect(Collectors.toList());
  }

  private Path createTemporaryReportFromTemplate(Path sourceReportPath, String placeHolder, String newValue) throws IOException {
    String reportContent = new String(Files.readAllBytes(sourceReportPath), UTF_8);
    reportContent = reportContent.replace(placeHolder, newValue);
    Path destReportPath = tmpDir.newFile(sourceReportPath.getFileName().toString()).toPath().toRealPath();
    Files.write(destReportPath, reportContent.getBytes(UTF_8));
    return destReportPath;
  }

  private static String formatIssues(List<Issue> issues) {
    return issues.stream()
      .map(issue -> filePath(issue) + "|" +
        issue.getRule() + "|" +
        issue.getSeverity().name() + "|" +
        issue.getDebt() + "|" +
        "line:" + issue.getLine() + "|" +
        issue.getMessage())
      .sorted()
      .collect(Collectors.joining("\n"));
  }

  private static String filePath(Issue issue) {
    return issue.getComponent().substring(issue.getComponent().indexOf(':') + 1);
  }

}
