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

class LineCoverageTest {
  @Test
  void shouldParseLineCoverageHits() {
    LineCoverage line = new LineCoverage();
    assertThat(line.hits).isZero();

    line.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 0"));
    assertThat(line.hits).isZero();

    line.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 3"));
    assertThat(line.hits).isEqualTo(3);

    line.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 2"));
    assertThat(line.hits).isEqualTo(5);

    line.add(CoverageStat.parseLine(2, "main.go:2.8,2.10 1 0"));
    assertThat(line.hits).isEqualTo(5);
  }

  @Test
  void shouldParseLineCoverageOverflow() {
    LineCoverage line = new LineCoverage();
    // hits is greater than Integer.MAX_VALUE
    line.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 " + +(((long) Integer.MAX_VALUE) + 1)));
    assertThat(line.hits).isEqualTo(Integer.MAX_VALUE);

    LineCoverage lineWithTwoStats = new LineCoverage();
    // hits is greater than Integer.MAX_VALUE
    lineWithTwoStats.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 " + (Integer.MAX_VALUE - 1)));
    lineWithTwoStats.add(CoverageStat.parseLine(2, "main.go:2.2,2.5 1 2"));
    assertThat(line.hits).isEqualTo(Integer.MAX_VALUE);
  }

}
