/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationsTest extends TestBase {
  private static final String BASE_DIRECTORY = "projects/duplications/";
  private static final String PROJECT_KEY = "apexDuplication";

  @Test
  public void duplications() {
    ORCHESTRATOR.executeBuild(getSonarScanner(PROJECT_KEY, BASE_DIRECTORY));

    // file1 - 23 duplicated LOC in duplicatedFunction
    // file2 - 23 + 22 (body without signature) duplicated LOC
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_lines")).isEqualTo(68);
    // file1 will report 2 blocks - one for the whole function and one for the body
    // file2 will report 3 blocks - 2 for duplicatedFunction (whole function + body) and 1 for the body of bodyDuplication
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_blocks")).isEqualTo(5);
    assertThat(getMeasureAsInt(PROJECT_KEY, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasure(PROJECT_KEY, "duplicated_lines_density").getValue()).isEqualTo("65.4");
  }
}
