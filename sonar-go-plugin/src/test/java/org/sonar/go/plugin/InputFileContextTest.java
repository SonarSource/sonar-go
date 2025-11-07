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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.go.impl.TextRanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputFileContextTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void shouldReturnNullWhenLocationIsWrong() {
    var inputFile = mock(InputFile.class);
    when(inputFile.newRange(anyInt(), anyInt(), anyInt(), anyInt())).thenThrow(new IllegalArgumentException("boom"));
    when(inputFile.lines()).thenReturn(5);
    when(inputFile.toString()).thenReturn("foo/bar.go");
    var sensorContext = mock(SensorContext.class);
    when(sensorContext.config()).thenReturn(mock(Configuration.class));
    var inputFileContext = new InputFileContext(sensorContext, inputFile);
    var range = TextRanges.range(10, 1, 10, 5);

    var actual = inputFileContext.textRange(range);

    assertThat(actual).isNull();
    assertThat(logTester.logs(Level.DEBUG)).contains("Invalid TextRange[10, 1, 10, 5], for file: foo/bar.go, number of lines: 5");
  }

  @Test
  void shouldThrowExceptionWhenLocationIsWrongAndFailFastIsEnabled() {
    var inputFile = mock(InputFile.class);
    when(inputFile.newRange(anyInt(), anyInt(), anyInt(), anyInt())).thenThrow(new IllegalArgumentException("boom"));
    when(inputFile.lines()).thenReturn(5);
    when(inputFile.toString()).thenReturn("foo/bar.go");
    var configMock = mock(Configuration.class);
    when(configMock.getBoolean(GoSensor.FAIL_FAST_PROPERTY_NAME)).thenReturn(Optional.of(true));
    var sensorContext = mock(SensorContext.class);
    when(sensorContext.config()).thenReturn(configMock);
    var inputFileContext = new InputFileContext(sensorContext, inputFile);
    var range = TextRanges.range(10, 1, 10, 5);

    assertThatThrownBy(() -> inputFileContext.textRange(range))
      .hasMessage("Invalid TextRange[10, 1, 10, 5], for file: foo/bar.go, number of lines: 5")
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldReportAnalysisErrorForDefinedLocation() {
    SensorContext sensorContext = mock(SensorContext.class);
    var analysisError = mock(NewAnalysisError.class);
    when(analysisError.message(anyString())).thenReturn(analysisError);
    when(sensorContext.newAnalysisError()).thenReturn(analysisError);
    var inputFile = mock(InputFile.class);
    var defaultTextPointer = new DefaultTextPointer(1, 2);
    when(inputFile.newPointer(1, 2)).thenReturn(defaultTextPointer);
    var inputFileContext = new InputFileContext(sensorContext, inputFile);
    var textPointer = new TextPointerImpl(1, 2);

    inputFileContext.reportAnalysisError("msg", textPointer);

    verify(analysisError).at(defaultTextPointer);
  }

  @Test
  void shouldReportAnalysisErrorWithoutLocationWhenProvidedPointerIsErroneous() {
    SensorContext sensorContext = mock(SensorContext.class);
    var analysisError = mock(NewAnalysisError.class);
    when(analysisError.message(anyString())).thenReturn(analysisError);
    when(sensorContext.newAnalysisError()).thenReturn(analysisError);
    var inputFile = mock(InputFile.class);
    var defaultTextPointer = new DefaultTextPointer(1, 2);
    when(inputFile.newPointer(1, 2)).thenThrow(new IllegalArgumentException());
    var inputFileContext = new InputFileContext(sensorContext, inputFile);
    var textPointer = new TextPointerImpl(1, 2);

    inputFileContext.reportAnalysisError("msg", textPointer);

    verify(analysisError, Mockito.never()).at(defaultTextPointer);
    assertThat(logTester.logs(Level.DEBUG)).isNotEmpty().anyMatch(l -> l.startsWith("Invalid location"));
  }

  @Test
  void shouldHandleUnsafeLineSelect() {
    var inputFile = mock(InputFile.class);
    when(inputFile.selectLine(42)).thenThrow(new IllegalArgumentException());
    assertThat(InputFileContext.safeExtractSelectLine(inputFile, 42)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isNotEmpty().anyMatch(l -> l.startsWith("Invalid line '42' for file"));
  }

  @Test
  void shouldHandleSafeLineSelect() {
    var inputFile = mock(InputFile.class);
    var textRange = mock(TextRange.class);
    when(inputFile.selectLine(42)).thenReturn(textRange);
    assertThat(InputFileContext.safeExtractSelectLine(inputFile, 42)).contains(textRange);
  }
}
