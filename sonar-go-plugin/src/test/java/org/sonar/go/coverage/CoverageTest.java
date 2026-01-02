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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageTest {

  @Test
  void shouldParseCoverage() {
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    Coverage coverage = new Coverage(linuxContext);
    coverage.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 1"));
    coverage.add(CoverageStat.parseLine(3, "main.go:4.2,4.7 1 0"));
    coverage.add(CoverageStat.parseLine(4, "other.go:3.2,4.12 1 1"));

    assertThat(coverage.fileMap.keySet()).containsExactlyInAnyOrder("/home/paul/go/src/main.go", "/home/paul/go/src/other.go");
    List<CoverageStat> coverageStats = coverage.fileMap.get("/home/paul/go/src/main.go");
    FileCoverage fileCoverage = new FileCoverage(coverageStats, null);
    assertThat(fileCoverage.lineMap.keySet()).containsExactlyInAnyOrder(2, 4);
    assertThat(new FileCoverage(coverage.fileMap.get("/home/paul/go/src/other.go"), null).lineMap.keySet()).containsExactlyInAnyOrder(3, 4);
  }

}
