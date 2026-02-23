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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoTestInputFileTest {

  @Test
  void shouldReturnTestType() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.type()).thenReturn(InputFile.Type.MAIN);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.type()).isEqualTo(InputFile.Type.TEST);
  }

  @Test
  void shouldReturnDelegate() {
    InputFile delegate = mock(InputFile.class);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.getDelegate()).isEqualTo(delegate);
  }

  @Test
  void shouldDelegateRelativePath() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.relativePath()).thenReturn("src/test/go/file_test.go");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.relativePath()).isEqualTo("src/test/go/file_test.go");
    verify(delegate).relativePath();
  }

  @Test
  void shouldDelegateAbsolutePath() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.absolutePath()).thenReturn("/absolute/path/file_test.go");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.absolutePath()).isEqualTo("/absolute/path/file_test.go");
    verify(delegate).absolutePath();
  }

  @Test
  void shouldDelegateFile() {
    InputFile delegate = mock(InputFile.class);
    File mockFile = new File("test.go");
    when(delegate.file()).thenReturn(mockFile);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.file()).isEqualTo(mockFile);
    verify(delegate).file();
  }

  @Test
  void shouldDelegatePath() {
    InputFile delegate = mock(InputFile.class);
    Path mockPath = Paths.get("test.go");
    when(delegate.path()).thenReturn(mockPath);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.path()).isEqualTo(mockPath);
    verify(delegate).path();
  }

  @Test
  void shouldDelegateUri() {
    InputFile delegate = mock(InputFile.class);
    URI mockUri = URI.create("file:///test.go");
    when(delegate.uri()).thenReturn(mockUri);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.uri()).isEqualTo(mockUri);
    verify(delegate).uri();
  }

  @Test
  void shouldDelegateFilename() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.filename()).thenReturn("file_test.go");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.filename()).isEqualTo("file_test.go");
    verify(delegate).filename();
  }

  @Test
  void shouldDelegateLanguage() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.language()).thenReturn("go");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.language()).isEqualTo("go");
    verify(delegate).language();
  }

  @Test
  void shouldDelegateInputStream() throws IOException {
    InputFile delegate = mock(InputFile.class);
    InputStream mockInputStream = mock(InputStream.class);
    when(delegate.inputStream()).thenReturn(mockInputStream);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.inputStream()).isEqualTo(mockInputStream);
    verify(delegate).inputStream();
  }

  @Test
  void shouldDelegateContents() throws IOException {
    InputFile delegate = mock(InputFile.class);
    String content = "package main\nfunc TestExample(t *testing.T) {}";
    when(delegate.contents()).thenReturn(content);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.contents()).isEqualTo(content);
    verify(delegate).contents();
  }

  @Test
  void shouldDelegateStatus() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.status()).thenReturn(InputFile.Status.CHANGED);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.status()).isEqualTo(InputFile.Status.CHANGED);
    verify(delegate).status();
  }

  @Test
  void shouldDelegateLines() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.lines()).thenReturn(42);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.lines()).isEqualTo(42);
    verify(delegate).lines();
  }

  @Test
  void shouldDelegateIsEmpty() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.isEmpty()).thenReturn(false);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.isEmpty()).isFalse();
    verify(delegate).isEmpty();
  }

  @Test
  void shouldDelegateNewPointer() {
    InputFile delegate = mock(InputFile.class);
    TextPointer mockPointer = mock(TextPointer.class);
    when(delegate.newPointer(1, 0)).thenReturn(mockPointer);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.newPointer(1, 0)).isEqualTo(mockPointer);
    verify(delegate).newPointer(1, 0);
  }

  @Test
  void shouldDelegateNewRangeWithPointers() {
    InputFile delegate = mock(InputFile.class);
    TextPointer start = mock(TextPointer.class);
    TextPointer end = mock(TextPointer.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.newRange(start, end)).thenReturn(mockRange);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.newRange(start, end)).isEqualTo(mockRange);
    verify(delegate).newRange(start, end);
  }

  @Test
  void shouldDelegateNewRangeWithCoordinates() {
    InputFile delegate = mock(InputFile.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.newRange(1, 0, 2, 5)).thenReturn(mockRange);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.newRange(1, 0, 2, 5)).isEqualTo(mockRange);
    verify(delegate).newRange(1, 0, 2, 5);
  }

  @Test
  void shouldDelegateSelectLine() {
    InputFile delegate = mock(InputFile.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.selectLine(5)).thenReturn(mockRange);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.selectLine(5)).isEqualTo(mockRange);
    verify(delegate).selectLine(5);
  }

  @Test
  void shouldDelegateCharset() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.charset()).thenReturn(StandardCharsets.UTF_8);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    verify(delegate).charset();
  }

  @Test
  void shouldDelegateMd5Hash() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.md5Hash()).thenReturn("abc123def456");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.md5Hash()).isEqualTo("abc123def456");
    verify(delegate).md5Hash();
  }

  @Test
  void shouldDelegateKey() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.key()).thenReturn("project:src/test/go/file_test.go");

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.key()).isEqualTo("project:src/test/go/file_test.go");
    verify(delegate).key();
  }

  @Test
  void shouldDelegateIsFile() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.isFile()).thenReturn(true);

    GoTestInputFile goTestInputFile = new GoTestInputFile(delegate);

    assertThat(goTestInputFile.isFile()).isTrue();
    verify(delegate).isFile();
  }
}
