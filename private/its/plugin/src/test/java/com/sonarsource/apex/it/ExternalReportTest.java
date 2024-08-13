/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalReportTest extends TestBase {

  private static final String BASE_DIRECTORY = "../sources/projects/Cumulus/";
  private static final String PROJECT_KEY = "apexPMD";

  @Test
  public void pmd() {
    ORCHESTRATOR.executeBuild(
      getSonarScanner(PROJECT_KEY, BASE_DIRECTORY, "norule")
        .setProperty("sonar.apex.pmd.reportPaths", "../../../plugin/src/test/resources/pmd-report-cumulus.xml"));

    // according to the ruling, S100 raises normally multiple issues on the "Cumulus" project
    assertThat(getIssuesForRule(PROJECT_KEY, "apex:S100")).isEmpty();

    List<Issue> externalIssues = getPmdIssues();
    assertThat(getMeasureAsInt(PROJECT_KEY, "violations")).isEqualTo(1);
    assertThat(externalIssues).hasSize(1);
    Issue issue = externalIssues.get(0);
    assertThat(issue.getComponent()).isEqualTo(PROJECT_KEY + ":scripts/CRLP_TEST_VALIDATE_ROLLUPS.cls");
    assertThat(issue.getRule()).isEqualTo("external_pmd_apex:ApexSharingViolations");
    assertThat(issue.getLine()).isEqualTo(24);
    assertThat(issue.getMessage()).isEqualTo("Apex classes should declare a sharing model if DML or SOQL is used");
    assertThat(issue.getSeverity().name()).isEqualTo("MAJOR");
    assertThat(issue.getDebt()).isEqualTo("5min");
  }

  private static List<Issue> getPmdIssues() {
    return newWsClient().issues().search(new SearchRequest().setComponentKeys(Collections.singletonList(PROJECT_KEY)))
      .getIssuesList().stream()
      .filter(issue -> issue.getRule().startsWith("external_pmd"))
      .collect(Collectors.toList());
  }
}
