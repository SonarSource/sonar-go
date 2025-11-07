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
package org.sonar.go.plugin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
import org.sonar.plugins.go.api.checks.GoModFileData;
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
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.moduleName()).isEqualTo(expectedModuleName);
    if (expectedModuleName.isEmpty()) {
      assertThat(logTester.logs()).contains(
        "Failed to detect a module name in the go.mod file: go.mod");
    } else {
      assertThat(logTester.logs()).contains("Detected go module name in project from go.mod: " + expectedModuleName);
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
      Arguments.of("module `myModule`", "`myModule`"),
      Arguments.of("module 'myModule'", "'myModule'"),
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
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(expectedVersion);
    if (expectedVersion == GoVersion.UNKNOWN_VERSION) {
      assertThat(logTester.logs()).contains(
        "Detected go module name in project from go.mod: myModule",
        "Failed to detect a go version in the go.mod file: go.mod");
    } else {
      assertThat(logTester.logs()).contains("Detected go version in project from go.mod: " + expectedVersion);
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
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.moduleName()).isEmpty();
    assertThat(goModFileData.replacedModules()).isEmpty();
    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(logTester.logs()).contains("Expected at least one go.mod file, but found none.");
  }

  @Test
  void shouldReturnUnknownModuleNameWhenNotPresent() {
    InputFile goModFile = createInputFile("go.mod", """
      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.moduleName()).isEmpty();
    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(logTester.logs()).contains(
      "Failed to detect a module name in the go.mod file: go.mod",
      "Detected go version in project from go.mod: 1.23.4");
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
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(logTester.logs()).contains("Failed to read go.mod file: " + crashingInputFile);
  }

  @Test
  void shouldNotIdentifyGoVersionInSonarQubeIDEContext() {
    sensorContext.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(10, 18)));
    InputFile goModFile = createInputFile("go.mod", """
      module myModule

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

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
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(logTester.logs()).contains("Detected go version in project from src/go.mod: 1.23.4");
  }

  @Test
  void shouldFindSingleReplace() {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule
      replace example.com/old v1.0 => example.com/new v1.2.3
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.replacedModules())
      .containsExactly(Map.entry(new GoModFileData.ModuleSpec("example.com/old", "v1.0"), new GoModFileData.ModuleSpec("example.com/new", "v1.2.3")));
  }

  @Test
  void shouldFindReplaceWithDotsInPath() {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule
      replace golang.org/x/net v1.2.3 => ./fork/net
      replace golang.org/x/net => ../fork/net
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.replacedModules()).containsExactly(
      Map.entry(new GoModFileData.ModuleSpec("golang.org/x/net", "v1.2.3"), new GoModFileData.ModuleSpec("./fork/net", null)),
      Map.entry(new GoModFileData.ModuleSpec("golang.org/x/net", null), new GoModFileData.ModuleSpec("../fork/net", null)));
  }

  @Test
  void shouldGetRidOfQuotesInReplace() {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule
      replace "example.com/old" v1.0 => example.com/new v1.2.3
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.replacedModules())
      .containsExactly(Map.entry(new GoModFileData.ModuleSpec("example.com/old", "v1.0"), new GoModFileData.ModuleSpec("example.com/new", "v1.2.3")));
  }

  static Stream<Arguments> shouldFindSeveralReplace() {
    return Stream.of(
      Arguments.of("""
        replace example.com/old v1.0 => example.com/new v1.2.3
        replace example.com/old2 => example.com/new2
        """),
      Arguments.of("""
        replace (
          example.com/old v1.0 => example.com/new v1.2.3
          example.com/old2 => example.com/new2
        )
        """),
      Arguments.of("""
        replace example.com/old v1.0 => example.com/new v1.2.3
        replace (
          example.com/old2 => example.com/new2
        )
        """));
  }

  @ParameterizedTest
  @MethodSource
  void shouldFindSeveralReplace(String replaceContent) {
    InputFile goModFile = createInputFile("go.mod", """
      module myModule
      %s
      """.formatted(replaceContent));

    sensorContext.fileSystem().add(goModFile);
    var goModFileData = parseSingleGoModFile();

    assertThat(goModFileData.replacedModules()).containsExactly(
      Map.entry(new GoModFileData.ModuleSpec("example.com/old", "v1.0"), new GoModFileData.ModuleSpec("example.com/new", "v1.2.3")),
      Map.entry(new GoModFileData.ModuleSpec("example.com/old2", null), new GoModFileData.ModuleSpec("example.com/new2", null)));
  }

  @Test
  void shouldParseMultipleGoModFile() {
    InputFile goModFile = createInputFile("src/folder1/go.mod", """
      module myModule1

      go 1.23.4
      """);

    InputFile goModFile2 = createInputFile("src/folder2/go.mod", """
      module myModule2

      go 1.23.2
      """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goModFile2);
    var goModFileDataStore = goModFileAnalyzer.analyzeGoModFiles();

    var goModFileData1 = goModFileDataStore.retrieveClosestGoModFileData(goModFile.uri());
    assertThat(goModFileData1.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(goModFileData1.moduleName()).isEqualTo("myModule1");

    var goModFileData2 = goModFileDataStore.retrieveClosestGoModFileData(goModFile2.uri());
    assertThat(goModFileData2.goVersion()).isEqualTo(GoVersion.parse("1.23.2"));
    assertThat(goModFileData2.moduleName()).isEqualTo("myModule2");

    assertThat(goModFileDataStore.getRootPath()).endsWith("/src");
  }

  @Test
  void shouldFindClosestGoModFileFromSubFolders() {
    InputFile goModFile = createInputFile("src/go.mod", """
      module myModule1

      go 1.23.4
      """);

    InputFile goModFile2 = createInputFile("src/my/sub/folder/go.mod", """
      module myModule2

      go 1.23.2
      """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goModFile2);
    var goModFileDataStore = goModFileAnalyzer.analyzeGoModFiles();

    var folderPath = toInputFileUri("src/my/sub/file.go");
    var goModFileDataRoot = goModFileDataStore.retrieveClosestGoModFileData(folderPath);
    assertThat(goModFileDataRoot.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(goModFileDataRoot.moduleName()).isEqualTo("myModule1");

    var subfolderPath = toInputFileUri("src/my/sub/folder/in/deeper/subfolder/file.go");
    var goModFileDataSubfolder = goModFileDataStore.retrieveClosestGoModFileData(subfolderPath);
    assertThat(goModFileDataSubfolder.goVersion()).isEqualTo(GoVersion.parse("1.23.2"));
    assertThat(goModFileDataSubfolder.moduleName()).isEqualTo("myModule2");

    var otherFolderPath = toInputFileUri("other/path/file.go");
    var goModFileDataEmpty = goModFileDataStore.retrieveClosestGoModFileData(otherFolderPath);
    assertThat(goModFileDataEmpty.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(goModFileDataEmpty.moduleName()).isEmpty();

    assertThat(goModFileDataStore.getRootPath()).endsWith("/src");
  }

  @Test
  void shouldNotFindGoModFileWhenDifferentFolderPath() {
    InputFile goModFile = createInputFile("src/a/very/long/common/path/go.mod", """
      module myModule1

      go 1.23.4
      """);

    InputFile goModFile2 = createInputFile("src/a/very/long/common/path/but/different/here/go.mod", """
      module myModule2

      go 1.23.2
      """);

    sensorContext.fileSystem().add(goModFile);
    sensorContext.fileSystem().add(goModFile2);
    var goModFileDataStore = goModFileAnalyzer.analyzeGoModFiles();

    var folderPath = toInputFileUri("src/a/very/long/common/path/file.go");
    var goModFileDataRoot = goModFileDataStore.retrieveClosestGoModFileData(folderPath);
    assertThat(goModFileDataRoot.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(goModFileDataRoot.moduleName()).isEqualTo("myModule1");

    var subfolderPath = toInputFileUri("src/a/very/long/common/path/but/different/here/file.go");
    var goModFileDataSubfolder = goModFileDataStore.retrieveClosestGoModFileData(subfolderPath);
    assertThat(goModFileDataSubfolder.goVersion()).isEqualTo(GoVersion.parse("1.23.2"));
    assertThat(goModFileDataSubfolder.moduleName()).isEqualTo("myModule2");

    assertThat(goModFileDataStore.getRootPath()).endsWith("/src/a/very/long/common/path");
  }

  @Test
  void shouldComputeLongestCommonPathPossibleAndResolveGoModFile() {
    InputFile goModFile = createInputFile("src/go.mod", """
      module myModule1

      go 1.23.4
      """);

    sensorContext.fileSystem().add(goModFile);
    var goModFileDataStore = goModFileAnalyzer.analyzeGoModFiles();

    var folderPath = toInputFileUri("test/folder/file.go");
    var goModFileDataRoot = goModFileDataStore.retrieveClosestGoModFileData(folderPath);
    assertThat(goModFileDataRoot.goVersion()).isEqualTo(GoVersion.UNKNOWN_VERSION);
    assertThat(goModFileDataRoot.moduleName()).isEmpty();

    assertThat(goModFileDataStore.getRootPath()).endsWith("/src");
  }

  @Test
  void shouldGetModFileWithJustDirectory() {
    InputFile goModFile1 = createInputFile("root/src/go.mod", """
      module myModule1

      go 1.23.4
      """);
    InputFile goModFile2 = createInputFile("root/test/go.mod", """
      module myModule2

      go 1.23.2
      """);

    sensorContext.fileSystem().add(goModFile1);
    sensorContext.fileSystem().add(goModFile2);
    var goModFileDataStore = goModFileAnalyzer.analyzeGoModFiles();

    var folderPath = toInputFileUri("root/src");
    var goModFileDataRoot = goModFileDataStore.retrieveClosestGoModFileData(folderPath);
    assertThat(goModFileDataRoot.goVersion()).isEqualTo(GoVersion.parse("1.23.4"));
    assertThat(goModFileDataRoot.moduleName()).isEqualTo("myModule1");

    assertThat(goModFileDataStore.getRootPath()).endsWith("/root");
  }

  private GoModFileData parseSingleGoModFile() {
    var goModFilesData = goModFileAnalyzer.analyzeGoModFiles();
    var file = sensorContext.fileSystem().files(f -> true).iterator().next();
    return goModFilesData.retrieveClosestGoModFileData(file.toURI());
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

  // Return a URI as computed by sonar InputFile API
  private URI toInputFileUri(String filename) {
    Path filePath = projectDir.resolve(filename);
    return TestInputFileBuilder.create("module", projectDir.toFile(), filePath.toFile())
      .build()
      .uri();
  }
}
