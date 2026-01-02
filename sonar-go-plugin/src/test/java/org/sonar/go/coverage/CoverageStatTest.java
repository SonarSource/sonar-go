/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.coverage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoverageStatTest {

  @Test
  void shouldParseCoverageStat() {
    var coverage = CoverageStat.parseLine(2, "_/my-app/my-app.go:3.10,4.5 2 234");
    assertThat(coverage.filePath).isEqualTo("_/my-app/my-app.go");
    assertThat(coverage.startLine).isEqualTo(3);
    assertThat(coverage.startCol).isEqualTo(10);
    assertThat(coverage.endLine).isEqualTo(4);
    assertThat(coverage.endCol).isEqualTo(5);
    // numStmt is not parsed because not required.
    assertThat(coverage.count).isEqualTo(234);
  }

  @Test
  void shouldThrowExceptionWhenParseInvalidLine() {
    assertThatThrownBy(() -> CoverageStat.parseLine(42, "invalid"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid go coverage at line 42");
  }

  @Test
  void shouldNotParseNumStatement() {
    var coverage = CoverageStat.parseLine(2, "main.go:2.2,2.5 2650701153000000 0");
    // Num statement is not used ^^^^^^^^^^^^^^^^
    assertThat(coverage.startLine).isEqualTo(2);
  }
}
