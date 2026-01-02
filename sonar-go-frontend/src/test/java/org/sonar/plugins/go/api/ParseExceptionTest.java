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
package org.sonar.plugins.go.api;

import org.junit.jupiter.api.Test;
import org.sonar.go.impl.TextPointerImpl;

import static org.assertj.core.api.Assertions.assertThat;

class ParseExceptionTest {

  @Test
  void shouldCreateExceptionFromMessage() {
    ParseException exception = new ParseException("message");

    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getPosition()).isNull();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getInputFilePath()).isNull();
  }

  @Test
  void shouldCreateExceptionFromMessageAndTextPointer() {
    ParseException exception = new ParseException("message", new TextPointerImpl(1, 2));

    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getPosition().line()).isEqualTo(1);
    assertThat(exception.getPosition().lineOffset()).isEqualTo(2);
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getInputFilePath()).isNull();
  }

  @Test
  void shouldCreateExceptionFromMessageAndTextPointerAndCause() {
    ParseException exception = new ParseException("message", new TextPointerImpl(1, 2), new RuntimeException("cause"));

    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getPosition().line()).isEqualTo(1);
    assertThat(exception.getPosition().lineOffset()).isEqualTo(2);
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    assertThat(exception.getInputFilePath()).isNull();
  }

  @Test
  void shouldCreateExceptionFromMessageAndTextPointerAndCauseAndInputFilePath() {
    ParseException exception = new ParseException("message", new TextPointerImpl(1, 2), new RuntimeException("cause"), "path/to/file.go");

    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getPosition().line()).isEqualTo(1);
    assertThat(exception.getPosition().lineOffset()).isEqualTo(2);
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    assertThat(exception.getInputFilePath()).isEqualTo("path/to/file.go");
  }
}
