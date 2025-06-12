/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.plugins.go.api.checks.GoVersion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GoModFileAnalyzerTest {

  private Path projectDir;
  private SensorContextTester sensorContext;
  private GoModFileAnalyzer goModFileAnalyzer;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  void setUp() throws IOException {
    var workDir = Files.createTempDirectory("gotest");
    workDir.toFile().deleteOnExit();
    projectDir = Files.createTempDirectory("gotestProject");
    projectDir.toFile().deleteOnExit();
    sensorContext = SensorContextTester.create(workDir);
    sensorContext.fileSystem().setWorkDir(workDir);
    goModFileAnalyzer = new GoModFileAnalyzer(sensorContext);
  }

  @ParameterizedTest
  @MethodSource
  void shouldAnalyzeGoModuleNameCorrectly(String addedContent, String expectedModuleName) {
    InputFile goModFile = createInputFile("go.mod", """
      %s
      """.formatted(addedContent));

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.moduleName()).isEqualTo(expectedModuleName);
    if (expectedModuleName.isEmpty()) {
      assertThat(logTester.logs()).contains(
        "Failed to detect a module name in the go.mod file: go.mod");
    } else {
      assertThat(logTester.logs()).contains("Detected go module name in project: " + expectedModuleName);
    }
  }

  static Stream<Arguments> shouldAnalyzeGoModuleNameCorrectly() {
    return Stream.of(
      Arguments.of("module myModule", "myModule"),
      Arguments.of("module myModule   ", "myModule"),
      Arguments.of("module myModule/v2", "myModule/v2"),
      Arguments.of("module \"myModule\"", "myModule"),
      Arguments.of("module \"myModule", "\"myModule"),
      Arguments.of("module \"myMo\\\"dule\"", "myMo\\\"dule"),
      Arguments.of("module `myModule`", "myModule"),
      Arguments.of("module `myMo\\`dule`", "myMo\\`dule"),
      Arguments.of("", ""),
      Arguments.of("go 1.23.4", ""));
  }

  @ParameterizedTest
  @MethodSource
  void shouldAnalyzeGoVersionCorrectly(String addedContent, GoVersion expectedVersion) {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule

      %s
      """.formatted(addedContent));

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(expectedVersion);
    if (expectedVersion == GoVersion.UNKNOWN_VERSION) {
      assertThat(logTester.logs()).contains(
        "Detected go module name in project: myModule",
        "Failed to detect a go version in the go.mod file: go.mod");
    } else {
      assertThat(logTester.logs()).contains("Detected go version in project: " + expectedVersion);
    }
  }

  static Stream<Arguments> shouldAnalyzeGoVersionCorrectly() {
    return Stream.of(
      Arguments.of("go 1.23.4", GoVersion.parse("1.23.4")),
      Arguments.of("go 1.23", GoVersion.parse("1.23")),
      Arguments.of("go 1.23.4rc1", GoVersion.parse("1.23.4")),
      Arguments.of("go 1.23.4beta2", GoVersion.parse("1.23.4")),
      Arguments.of("go 1.23     ", GoVersion.parse("1.23")),
      Arguments.of("", GoVersion.UNKNOWN_VERSION),
      Arguments.of("go", GoVersion.UNKNOWN_VERSION),
      Arguments.of("go somethingElse", GoVersion.UNKNOWN_VERSION),
      Arguments.of("go 0.1.0", GoVersion.UNKNOWN_VERSION),
      Arguments.of("go 1.2.3.4", GoVersion.UNKNOWN_VERSION),

      // taking only the first encountered version
      Arguments.of("go 1.23.4beta2\ngo 1.24", GoVersion.parse("1.23.4")));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "goo.mod",
    "somefile.go",
    "notgo.mod",
  })
  void shouldNotFindGoVersionWhenGoModHasWrongName(String fileName) {
    InputFile goModFile = createInputFile(fileName, """
      module myModule

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.moduleName()).isEmpty();
    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(logTester.logs()).contains(
      "Expected exactly one go.mod file, but found 0 files.",
      "Could not detect the metadata from mod file of the project");
  }

  @Test
  void shouldReturnUnknownModuleNameWhenNotPresent() {
    InputFile goModFile = createInputFile("go.mod", """
      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.moduleName()).isEmpty();
    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(logTester.logs()).contains(
      "Failed to detect a module name in the go.mod file: go.mod",
      "Detected go version in project: 1.23.4");
  }

  @Test
  void shouldReturnUnknownVersionOnIOException() throws IOException {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule

      go 1.23.4
      """);
    InputFile crashingInputFile = spy(goModFile);
    when(crashingInputFile.contents()).thenThrow(new IOException());

    sensorContext.fileSystem().add(crashingInputFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(logTester.logs()).contains("Failed to read go.mod file: " + crashingInputFile, "Could not detect the metadata from mod file of the project");
  }

  @Test
  void shouldNotIdentifyGoVersionInSonarQubeIDEContext() {
    sensorContext.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(10, 18)));
    InputFile goModFile = createInputFile("go.mod", """
      module myModule

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    // As we don't support the feature yet, we don't need to log there
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldFindGoVersionInSrcFolder() {
    InputFile goModFile = createInputFile("src/go.mod", """
      module myModule

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(logTester.logs()).contains("Detected go version in project: 1.23.4");
  }

  @Test
  void shouldNotFindGoVersionWithTwoGoModFiles() {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule

      go 1.23.4
      """);

    InputFile goModFile2 = createInputFile("src/go.mod", """
      module myModule

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goModFile2);
    var goModFileData = goModFileAnalyzer.analyzeGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(logTester.logs()).contains(
      "Expected exactly one go.mod file, but found 2 files.",
      "Could not detect the metadata from mod file of the project");
  }

  private InputFile createInputFile(String filename, String content) {
    Path filePath = projectDir.resolve(filename);
    return TestInputFileBuilder.create("module", projectDir.toFile(), filePath.toFile())
      .setCharset(UTF_8)
      .setLanguage(GoLanguage.KEY)
      .setContents(content)
      .setType(InputFile.Type.MAIN)
      .build();
  }
}
