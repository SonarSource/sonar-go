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
package org.sonarsource.slang;

import com.sonar.orchestrator.build.SonarScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CoverageTest extends TestBase {

  private static final Path BASE_DIRECTORY = Paths.get("projects", "measures");

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void go_coverage() {
    final String projectKey = "goCoverage";
    SonarScanner goScanner = getSonarScanner(projectKey, BASE_DIRECTORY.toString(), "go");
    goScanner.setProperty("sonar.go.coverage.reportPaths", "coverage.out");

    ORCHESTRATOR.executeBuild(goScanner);

    String componentKey = projectKey + ":pivot.go";
    assertThat(getMeasureAsInt(componentKey, "lines_to_cover")).isEqualTo(16);
    assertThat(getMeasureAsInt(componentKey, "uncovered_lines")).isEqualTo(4);
    assertThat(getMeasureAsInt(componentKey, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(componentKey, "uncovered_conditions")).isNull();
  }
}
