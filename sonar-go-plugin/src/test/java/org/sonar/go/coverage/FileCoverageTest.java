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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileCoverageTest {

  static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");

  @Test
  void shouldParseFileCoverage() throws Exception {
    List<CoverageStat> coverageStats = Arrays.asList(
      CoverageStat.parseLine(2, "cover.go:4.11,6.3 1 3"),
      CoverageStat.parseLine(3, "cover.go:6.3,8.3 1 0"));
    FileCoverage file = new FileCoverage(coverageStats, Files.readAllLines(COVERAGE_DIR.resolve("cover.go")));

    assertThat(file.lineMap.keySet()).containsExactlyInAnyOrder(5, 6, 7);
    assertThat(file.lineMap.get(4)).isNull();
    assertThat(file.lineMap.get(5).hits).isEqualTo(3);
    assertThat(file.lineMap.get(6).hits).isZero();
    assertThat(file.lineMap.get(7).hits).isZero();
    assertThat(file.lineMap.get(8)).isNull();
  }

  @Test
  void shouldParseFileCoverageEmptyLines() throws Exception {
    final String fileName = "cover_empty_lines.go";
    List<CoverageStat> coverageStats = Collections.singletonList(CoverageStat.parseLine(2, fileName + ":3.28,9.2 2 1"));
    FileCoverage file = new FileCoverage(coverageStats, Files.readAllLines(COVERAGE_DIR.resolve(fileName)));

    assertThat(file.lineMap.keySet()).containsExactlyInAnyOrder(5, 7);
  }
}
