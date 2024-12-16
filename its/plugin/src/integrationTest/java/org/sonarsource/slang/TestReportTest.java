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
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReportTest extends TestBase {

  private static final Path BASE_DIRECTORY = Paths.get("projects", "measures");

  @Test
  public void go_test_report() {
    final String projectKey = "goTestReport";
    SonarScanner goScanner = getSonarScanner(projectKey, BASE_DIRECTORY.toString(), "go");
    goScanner.setProperty("sonar.tests", ".");
    goScanner.setProperty("sonar.test.inclusions", "**/*_test.go");
    goScanner.setProperty("sonar.go.tests.reportPaths", "go-test-report.out");

    ORCHESTRATOR.executeBuild(goScanner);

    assertThat(getMeasureAsInt(projectKey, "tests")).isEqualTo(4);
    assertThat(getMeasureAsInt(projectKey, "test_failures")).isEqualTo(2);
    assertThat(getMeasureAsInt(projectKey, "test_errors")).isNull();
    assertThat(getMeasureAsInt(projectKey, "skipped_tests")).isEqualTo(1);
    assertThat(getMeasureAsInt(projectKey, "test_execution_time")).isEqualTo(4);

    final String componentKey = projectKey + ":pivot_test.go";
    assertThat(getMeasureAsInt(componentKey, "tests")).isEqualTo(4);
    assertThat(getMeasureAsInt(componentKey, "test_failures")).isEqualTo(2);
    assertThat(getMeasureAsInt(componentKey, "test_errors")).isEqualTo(0);
    assertThat(getMeasureAsInt(componentKey, "skipped_tests")).isEqualTo(1);
    assertThat(getMeasureAsInt(componentKey, "test_execution_time")).isEqualTo(4);
  }

}
