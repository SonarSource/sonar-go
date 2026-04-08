/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class GoCoverageStorageImplTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");

  @Test
  void shouldTrackStatisticsCorrectly() {
    FileResolutionStatistics statistics = new FileResolutionStatistics();

    statistics.incrementAbsolutePath();
    statistics.incrementRelativePath();
    statistics.incrementRelativePath();

    assertThat(statistics.absolutePath()).isEqualTo(1);
    assertThat(statistics.relativePath()).isEqualTo(2);
    assertThat(statistics.relativeNoModuleInGoModDir()).isZero();
  }
}
