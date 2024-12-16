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

public class MeasuresTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/measures/";

  @Test
  public void go_measures() {
    final String projectKey = "goMeasures";
    ORCHESTRATOR.executeBuild(getSonarScanner(projectKey, BASE_DIRECTORY, "go"));

    final String componentKey = projectKey + ":pivot.go";
    assertThat(getMeasureAsInt(componentKey, "ncloc")).isEqualTo(41);
    assertThat(getMeasureAsInt(componentKey, "comment_lines")).isEqualTo(2);

    assertThat(getMeasureAsInt(componentKey, "functions")).isEqualTo(3);
  }
}
