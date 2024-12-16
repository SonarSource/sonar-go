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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoSonarTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/nosonar/";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String RULE_KEY = "S1145";

  @Test
  public void test_go_nosonar() {
    String projectKey = "goNoSonar";
    String language = "go";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, language, NO_SONAR_PROFILE_NAME));

    assertThat(getMeasureAsInt(projectKey, "files")).isEqualTo(1);
    assertThat(getIssuesForRule(projectKey, language + ":" + RULE_KEY)).hasSize(1);
  }
}
