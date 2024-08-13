/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoSonarTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/nosonar/";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String RULE_KEY = "S1145";
  private static final String PROJECT_KEY = "apexNoSonar";

  @Test
  public void test_apex_nosonar() {
    ORCHESTRATOR.executeBuild(getSonarScanner(PROJECT_KEY, BASE_DIRECTORY, NO_SONAR_PROFILE_NAME));

    assertThat(getMeasureAsInt(PROJECT_KEY, "files")).isEqualTo(1);
    assertThat(getIssuesForRule(PROJECT_KEY, "apex:" + RULE_KEY)).hasSize(1);
  }

}
