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
package org.sonar.go.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.go.converter.GoExecutableExtractor.getExecutableForCurrentOS;

class GoExecutableExtractorTest {

  @TempDir
  File tempDir;

  @ParameterizedTest
  @CsvSource(textBlock = """
    Linux, x86_64, sonar-go-to-slang-linux-amd64
    Linux, aarch64, sonar-go-to-slang-linux-arm64
    Linux, arm64, sonar-go-to-slang-linux-arm64
    Linux, armv8, sonar-go-to-slang-linux-arm64
    Linux, amd64, sonar-go-to-slang-linux-amd64
    Linux, x64, sonar-go-to-slang-linux-amd64
    Windows 10, x86_64, sonar-go-to-slang-windows-amd64.exe
    Mac OS X, x86_64, sonar-go-to-slang-darwin-amd64
    Mac OS X, aarch64, sonar-go-to-slang-darwin-arm64
    """)
  void shouldReturnCorrectExecutableForCurrentOs(String osName, String arch, String expectedExecutable) {
    assertThat(getExecutableForCurrentOS(osName, arch)).isEqualTo(expectedExecutable);
  }

  @Test
  void shouldThrowForUnsupportedPlatform() {
    assertThatThrownBy(() -> getExecutableForCurrentOS("linux", "ppc64"))
      .isInstanceOf(InitializationException.class)
      .hasMessage("Unsupported OS/architecture: linux/ppc64");
  }

  @Test
  void shouldThrowExceptionOnInvalidExecutablePath() {
    assertThatThrownBy(() -> GoExecutableExtractor.getBytesFromResource("invalid-exe-path"))
      .isInstanceOf(InitializationException.class)
      .hasMessage("invalid-exe-path binary not found on class path");
  }

  @Test
  void shouldExtractTheExecutableOnceUnlessItWasDeleted() throws IOException {
    var executable = new File(GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo()));
    assertThat(executable).exists();
    assertThat(executable.canExecute()).isTrue();
    var extracted = Files.readAllBytes(executable.toPath());

    // Not read again by this JVM, which extracted it already
    Files.write(executable.toPath(), new byte[] {1, 2, 3});
    assertThat(GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo())).isEqualTo(executable.getAbsolutePath());
    assertThat(Files.readAllBytes(executable.toPath())).containsExactly(1, 2, 3);

    Files.delete(executable.toPath());
    assertThat(GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo())).isEqualTo(executable.getAbsolutePath());
    assertThat(Files.readAllBytes(executable.toPath())).as("a deleted executable is extracted again").isEqualTo(extracted);
  }

  @Test
  void shouldReplaceAnOutdatedExecutable() throws IOException {
    var executable = new File(tempDir, GoExecutableExtractor.getExecutableForCurrentOS(System.getProperty("os.name"), System.getProperty("os.arch")));
    Files.write(executable.toPath(), new byte[] {1, 2, 3});

    GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo());

    assertThat(executable.length()).as("an executable left by another version is replaced").isGreaterThan(3);
  }

  @Test
  void shouldFailOnTheArchitectureOfAnUnsupportedSystem() {
    var currentArch = System.getProperty("os.arch");
    try {
      System.setProperty("os.arch", "invalid-arch");
      var systemPlatform = new SystemPlatformInfo();
      assertThatThrownBy(() -> GoExecutableExtractor.extract(tempDir, systemPlatform))
        .isInstanceOf(InitializationException.class)
        .hasMessageMatching("Unsupported OS/architecture: .+/invalid-arch");
    } finally {
      System.setProperty("os.arch", currentArch);
    }
  }

  @Test
  void shouldFailOnUnsupportedPlatform() {
    var unsupportedPlatform = new TestPlatformInfo("unsupported-os", "unsupported-arch");

    assertThatThrownBy(() -> GoExecutableExtractor.extract(tempDir, unsupportedPlatform))
      .isInstanceOf(InitializationException.class)
      .hasMessage("Unsupported OS/architecture: unsupported-os/unsupported-arch");
  }
}
