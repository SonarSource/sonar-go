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

class FileResolutionStatisticsTest {

  @Test
  void shouldInitializeWithZeros() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    assertThat(statistics.absolutePath()).isZero();
    assertThat(statistics.relativeNoModuleInGoModDir()).isZero();
    assertThat(statistics.absoluteNoModuleInReportPath()).isZero();
    assertThat(statistics.relativePath()).isZero();
    assertThat(statistics.relativeNoModuleInReportPath()).isZero();
    assertThat(statistics.relativeSubPaths()).isZero();
    assertThat(statistics.unresolved()).isZero();
  }

  @Test
  void shouldIncrementAbsolutePath() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementAbsolutePath();
    assertThat(statistics.absolutePath()).isEqualTo(1);

    statistics.incrementAbsolutePath();
    assertThat(statistics.absolutePath()).isEqualTo(2);
  }

  @Test
  void shouldIncrementRelativeNoModuleInGoModDir() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementRelativeNoModuleInGoModDir();
    assertThat(statistics.relativeNoModuleInGoModDir()).isEqualTo(1);

    statistics.incrementRelativeNoModuleInGoModDir();
    assertThat(statistics.relativeNoModuleInGoModDir()).isEqualTo(2);
  }

  @Test
  void shouldIncrementAbsoluteNoModuleInReportPath() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementAbsoluteNoModuleInReportPath();
    assertThat(statistics.absoluteNoModuleInReportPath()).isEqualTo(1);

    statistics.incrementAbsoluteNoModuleInReportPath();
    assertThat(statistics.absoluteNoModuleInReportPath()).isEqualTo(2);
  }

  @Test
  void shouldIncrementRelativePath() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementRelativePath();
    assertThat(statistics.relativePath()).isEqualTo(1);

    statistics.incrementRelativePath();
    assertThat(statistics.relativePath()).isEqualTo(2);
  }

  @Test
  void shouldIncrementRelativeNoModuleInReportPath() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementRelativeNoModuleInReportPath();
    assertThat(statistics.relativeNoModuleInReportPath()).isEqualTo(1);

    statistics.incrementRelativeNoModuleInReportPath();
    assertThat(statistics.relativeNoModuleInReportPath()).isEqualTo(2);
  }

  @Test
  void shouldIncrementRelativeSubPaths() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementRelativeSubPaths();
    assertThat(statistics.relativeSubPaths()).isEqualTo(1);

    statistics.incrementRelativeSubPaths();
    assertThat(statistics.relativeSubPaths()).isEqualTo(2);
  }

  @Test
  void shouldIncrementUnresolved() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementUnresolved();
    assertThat(statistics.unresolved()).isEqualTo(1);

    statistics.incrementUnresolved();
    assertThat(statistics.unresolved()).isEqualTo(2);
  }

  @Test
  void shouldIncrementMultipleCounters() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementAbsolutePath();
    statistics.incrementRelativePath();
    statistics.incrementRelativePath();
    statistics.incrementRelativeSubPaths();

    assertThat(statistics.absolutePath()).isEqualTo(1);
    assertThat(statistics.relativeNoModuleInGoModDir()).isZero();
    assertThat(statistics.absoluteNoModuleInReportPath()).isZero();
    assertThat(statistics.relativePath()).isEqualTo(2);
    assertThat(statistics.relativeNoModuleInReportPath()).isZero();
    assertThat(statistics.relativeSubPaths()).isEqualTo(1);
  }
}
