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
package org.sonar.go.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.utils.TempFolder;
import org.sonar.go.converter.GoExecutableExtractor;
import org.sonar.go.converter.GoServerProcess;
import org.sonar.go.converter.SystemPlatformInfo;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstanceScopeGoConverterTest {
  @Test
  void constructor(@TempDir File tempDir) {
    TempFolder tempFolder = mock(TempFolder.class);
    when(tempFolder.newDir()).thenReturn(tempDir);
    try (var goProcess = new GoServerProcess(tempDir)) {
      InstanceScopeGoConverter converter = new InstanceScopeGoConverter(tempFolder, goProcess);
      // As the sensor does
      goProcess.start();

      Tree tree = converter.parse(Map.of("foo.go", "package main\nfunc foo() {}"), "moduleName").get("foo.go").tree();
      assertThat(tree).isInstanceOf(TopLevelTree.class);
    }
    // Windows can keep the exited process image locked briefly. Delete it with a retry before JUnit
    // tries to remove the temporary directory in one pass.
    Path executable = Path.of(GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo()));
    Awaitility.await("the Go executable to be unlocked")
      .atMost(Duration.ofSeconds(10))
      .pollDelay(Duration.ZERO)
      .pollInterval(Duration.ofMillis(50))
      .ignoreExceptionsInstanceOf(IOException.class)
      .until(() -> {
        Files.deleteIfExists(executable);
        return true;
      });
  }
}
