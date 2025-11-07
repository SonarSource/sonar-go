/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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
package org.sonar.go.converter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonar.go.converter.DefaultCommand.getExecutableForCurrentOS;

class DefaultCommandTest {

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
  void shouldThrowExceptionForInvalidPlatform() {
    var currentArch = System.getProperty("os.arch");
    try {
      System.setProperty("os.arch", "invalid-arch");
      assertThatThrownBy(() -> new DefaultCommand(tempDir))
        .isInstanceOf(InitializationException.class)
        .hasMessageMatching("Unsupported OS/architecture: .+/invalid-arch");
    } finally {
      System.setProperty("os.arch", currentArch);
    }
  }

  @Test
  void shouldFailOnInvalidCommand() {
    var command = new DefaultCommand(tempDir);
    command.getCommand().set(0, "invalid-command");
    var e = assertThrows(IOException.class,
      () -> command.executeCommand(Map.of("foo.go", "package main\nfunc foo() {}")));
    assertThat(e).hasMessageContaining("Cannot run program \"invalid-command\"");
  }
}
