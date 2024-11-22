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

import java.util.List;
import org.junit.Test;
import org.sonarqube.ws.Issues;

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

    assertThat(getMeasure(componentKey, "ncloc_data").getValue())
      .isEqualTo("1=1;3=1;4=1;5=1;6=1;7=1;8=1;10=1;11=1;12=1;13=1;14=1;16=1;17=1;18=1;20=1;21=1;22=1;23=1;24=1;25=1;" +
        "26=1;27=1;28=1;29=1;30=1;31=1;32=1;33=1;35=1;36=1;37=1;38=1;39=1;40=1;41=1;46=1;47=1;48=1;49=1;50=1");

    assertThat(getMeasureAsInt(componentKey, "functions")).isEqualTo(3);

    assertThat(getMeasure(componentKey, "executable_lines_data").getValue())
      .isEqualTo("32=1;36=1;37=1;38=1;40=1;49=1;22=1;23=1;25=1;26=1;27=1;29=1;30=1");
  }
}
