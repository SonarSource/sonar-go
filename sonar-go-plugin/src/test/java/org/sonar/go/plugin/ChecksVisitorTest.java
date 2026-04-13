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
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChecksVisitorTest {

  @TempDir
  private File tempFolder;
  private ChecksVisitor visitor;
  private SensorContextTester sensorContext;

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempFolder);
    visitor = new ChecksVisitor(new GoChecks(null), mock(DurationStatistics.class), mock(GoModFileDataStore.class), true);
  }

  @MethodSource("limitedCheckVisitorShouldBeApplicableToOnlyMainFiles")
  @ParameterizedTest
  void defaultCheckVisitorShouldBeApplicableToAllFiles(InputFile.Type fileType, boolean detectedAsTestFile) {
    InputFileContext inputFileContext = createInputFileContext("", fileType, detectedAsTestFile);
    assertThat(visitor.isApplicableTo(inputFileContext)).isTrue();
  }

  @MethodSource
  @ParameterizedTest
  void limitedCheckVisitorShouldBeApplicableToOnlyMainFiles(InputFile.Type fileType, boolean detectedAsTestFile, boolean shouldBeApplicable) {
    InputFileContext inputFileContext = createInputFileContext("", fileType, detectedAsTestFile);
    ChecksVisitor customVisitor = new ChecksVisitor(new GoChecks(null), mock(DurationStatistics.class), mock(GoModFileDataStore.class), false);
    assertThat(customVisitor.isApplicableTo(inputFileContext)).isEqualTo(shouldBeApplicable);
  }

  static Stream<Arguments> limitedCheckVisitorShouldBeApplicableToOnlyMainFiles() {
    return Stream.of(
      Arguments.of(InputFile.Type.MAIN, false, true),
      Arguments.of(InputFile.Type.MAIN, true, false),
      Arguments.of(InputFile.Type.TEST, true, false),
      // never achievable in real conditions, visitor only looks for our detection as test files, not the assignment from scanner
      Arguments.of(InputFile.Type.TEST, false, true));
  }

  private InputFileContext createInputFileContext(String code, InputFile.Type fileType, boolean detectedAsTestFile) {
    InputFile inputFile = new TestInputFileBuilder("moduleKey", "myFile.go")
      .setCharset(StandardCharsets.UTF_8)
      .setType(fileType)
      .initMetadata(code)
      .build();
    return new InputFileContext(sensorContext, inputFile, detectedAsTestFile);
  }
}
