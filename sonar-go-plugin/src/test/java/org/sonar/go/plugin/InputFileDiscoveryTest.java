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
package org.sonar.go.plugin;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.go.testing.TestInputFileCreator.createInputFile;

class InputFileDiscoveryTest {

  private File baseDir;
  private SensorContextTester context;

  @BeforeEach
  void setUp(@TempDir File tmpBaseDir) {
    baseDir = tmpBaseDir;
    context = SensorContextTester.create(baseDir);
  }

  @Test
  void shouldScanAllGoFilesWhenTestPropertiesAreNotSet() {
    var content = """
      package main
      func main() {
        print (1 == 1)
      }""";
    var inputFile1 = createInputFile("main.go", content, baseDir);
    // when no sonar.tests & sonar.test.inclusions are set then all files are indexed as MAIN
    var inputFile2 = createInputFile("main_test.go", content, baseDir, null, InputFile.Type.MAIN);
    context.fileSystem().add(inputFile1);
    context.fileSystem().add(inputFile2);

    var files = new InputFileDiscovery(new GoLanguage(new MapSettings().asConfig())).findAllInputFiles(context);

    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main.go"))
      .map(f -> f.inputFile().type())
      .containsOnly(InputFile.Type.MAIN);
    // The InputFile.type() returns MAIN as it is not possible to override the type(), it is computed by scanner
    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main_test.go"))
      .map(f -> f.inputFile().type())
      .containsOnly(InputFile.Type.MAIN);
    // But InputFileContext stores information that it is a test file
    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main_test.go"))
      .map(InputFileContext::isTestFile)
      .containsOnly(true);
  }

  @Test
  void shouldScanAllGoFilesWhenTestPropertiesAreSetToDefaults() {
    var content = """
      package main
      func main() {
        print (1 == 1)
      }""";
    var inputFile1 = createInputFile("main.go", content, baseDir);
    var inputFile2 = createInputFile("main_test.go", content, baseDir, null, InputFile.Type.TEST);
    context.fileSystem().add(inputFile1);
    context.fileSystem().add(inputFile2);
    context.settings().setProperty("sonar.tests", ".");
    context.settings().setProperty("sonar.test.inclusions", "**/*_test.go");

    var files = new InputFileDiscovery(new GoLanguage(new MapSettings().asConfig())).findAllInputFiles(context);

    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main.go"))
      .map(f -> f.inputFile().type())
      .containsOnly(InputFile.Type.MAIN);
    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main_test.go"))
      .map(f -> f.inputFile().type())
      .containsOnly(InputFile.Type.TEST);
  }

  @Test
  void shouldScanAllGoFilesWhenTestPropertiesAreSetToCustom() {
    var content = """
      package main
      func main() {
        print (1 == 1)
      }""";
    var inputFile1 = createInputFile("main.go", content, baseDir);
    var inputFile2 = createInputFile("main_foo.go", content, baseDir, null, InputFile.Type.TEST);
    context.fileSystem().add(inputFile1);
    context.fileSystem().add(inputFile2);
    context.settings().setProperty("sonar.tests", ".");
    context.settings().setProperty("sonar.test.inclusions", "**/*_foo.go");

    var files = new InputFileDiscovery(new GoLanguage(new MapSettings().asConfig())).findAllInputFiles(context);

    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main.go"))
      .map(f -> f.inputFile().type())
      .containsOnly(InputFile.Type.MAIN);
    assertThat(files).filteredOn(f -> f.inputFile().filename().equals("main_foo.go"))
      .extracting("inputFile.indexedFile.type", "testFile")
      .containsOnly(tuple(InputFile.Type.TEST, true));
  }

  @Test
  void shouldGroupFilesByDirectory() {
    InputFileContext ctx1 = mockInputFileContext("dir1/file1.go");
    InputFileContext ctx2 = mockInputFileContext("dir1/file2.go");
    InputFileContext ctx3 = mockInputFileContext("dir2/file3.go");

    var goFolders = InputFileDiscovery.groupFilesByDirectory(List.of(ctx1, ctx2, ctx3));

    assertThat(goFolders).containsExactlyInAnyOrder(
      new GoFolder(new File("dir1").toURI().getPath(), List.of(ctx1, ctx2)),
      new GoFolder(new File("dir2").toURI().getPath(), List.of(ctx3)));
  }

  private InputFileContext mockInputFileContext(String path) {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.uri()).thenReturn(new File(path).toURI());
    return new InputFileContext(context, inputFile);
  }
}
