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

class GoInputFileTest {

  @Test
  void shouldReturnTestTypeWhenTestFileIsTrue() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.type()).thenReturn(InputFile.Type.MAIN);

    GoInputFile goInputFile = new GoInputFile(delegate, true);

    assertThat(goInputFile.type()).isEqualTo(InputFile.Type.TEST);
  }

  @Test
  void shouldReturnDelegateTypeWhenTestFileIsFalse() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.type()).thenReturn(InputFile.Type.MAIN);

    GoInputFile goInputFile = new GoInputFile(delegate, false);

    assertThat(goInputFile.type()).isEqualTo(InputFile.Type.MAIN);
    verify(delegate).type();
  }

  @Test
  void shouldReturnDelegateTypeWhenUsingSingleArgConstructor() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.type()).thenReturn(InputFile.Type.MAIN);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.type()).isEqualTo(InputFile.Type.MAIN);
    verify(delegate).type();
  }

  @Test
  void shouldReturnDelegate() {
    InputFile delegate = mock(InputFile.class);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.getDelegate()).isEqualTo(delegate);
  }

  @Test
  void shouldDelegateRelativePath() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.relativePath()).thenReturn("src/test/go/file_test.go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.relativePath()).isEqualTo("src/test/go/file_test.go");
    verify(delegate).relativePath();
  }

  @Test
  void shouldDelegateAbsolutePath() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.absolutePath()).thenReturn("/absolute/path/file_test.go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.absolutePath()).isEqualTo("/absolute/path/file_test.go");
    verify(delegate).absolutePath();
  }

  @Test
  void shouldDelegateFile() {
    InputFile delegate = mock(InputFile.class);
    File mockFile = new File("test.go");
    when(delegate.file()).thenReturn(mockFile);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.file()).isEqualTo(mockFile);
    verify(delegate).file();
  }

  @Test
  void shouldDelegatePath() {
    InputFile delegate = mock(InputFile.class);
    Path mockPath = Paths.get("test.go");
    when(delegate.path()).thenReturn(mockPath);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.path()).isEqualTo(mockPath);
    verify(delegate).path();
  }

  @Test
  void shouldDelegateUri() {
    InputFile delegate = mock(InputFile.class);
    URI mockUri = URI.create("file:///test.go");
    when(delegate.uri()).thenReturn(mockUri);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.uri()).isEqualTo(mockUri);
    verify(delegate).uri();
  }

  @Test
  void shouldDelegateFilename() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.filename()).thenReturn("file_test.go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.filename()).isEqualTo("file_test.go");
    verify(delegate).filename();
  }

  @Test
  void shouldDelegateLanguage() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.language()).thenReturn("go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.language()).isEqualTo("go");
    verify(delegate).language();
  }

  @Test
  void shouldDelegateInputStream() throws IOException {
    InputFile delegate = mock(InputFile.class);
    InputStream mockInputStream = mock(InputStream.class);
    when(delegate.inputStream()).thenReturn(mockInputStream);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.inputStream()).isEqualTo(mockInputStream);
    verify(delegate).inputStream();
  }

  @Test
  void shouldDelegateContents() throws IOException {
    InputFile delegate = mock(InputFile.class);
    String content = "package main\nfunc TestExample(t *testing.T) {}";
    when(delegate.contents()).thenReturn(content);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.contents()).isEqualTo(content);
    verify(delegate).contents();
  }

  @Test
  void shouldDelegateStatus() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.status()).thenReturn(InputFile.Status.CHANGED);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.status()).isEqualTo(InputFile.Status.CHANGED);
    verify(delegate).status();
  }

  @Test
  void shouldDelegateLines() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.lines()).thenReturn(42);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.lines()).isEqualTo(42);
    verify(delegate).lines();
  }

  @Test
  void shouldDelegateIsEmpty() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.isEmpty()).thenReturn(false);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.isEmpty()).isFalse();
    verify(delegate).isEmpty();
  }

  @Test
  void shouldDelegateNewPointer() {
    InputFile delegate = mock(InputFile.class);
    TextPointer mockPointer = mock(TextPointer.class);
    when(delegate.newPointer(1, 0)).thenReturn(mockPointer);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.newPointer(1, 0)).isEqualTo(mockPointer);
    verify(delegate).newPointer(1, 0);
  }

  @Test
  void shouldDelegateNewRangeWithPointers() {
    InputFile delegate = mock(InputFile.class);
    TextPointer start = mock(TextPointer.class);
    TextPointer end = mock(TextPointer.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.newRange(start, end)).thenReturn(mockRange);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.newRange(start, end)).isEqualTo(mockRange);
    verify(delegate).newRange(start, end);
  }

  @Test
  void shouldDelegateNewRangeWithCoordinates() {
    InputFile delegate = mock(InputFile.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.newRange(1, 0, 2, 5)).thenReturn(mockRange);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.newRange(1, 0, 2, 5)).isEqualTo(mockRange);
    verify(delegate).newRange(1, 0, 2, 5);
  }

  @Test
  void shouldDelegateSelectLine() {
    InputFile delegate = mock(InputFile.class);
    TextRange mockRange = mock(TextRange.class);
    when(delegate.selectLine(5)).thenReturn(mockRange);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.selectLine(5)).isEqualTo(mockRange);
    verify(delegate).selectLine(5);
  }

  @Test
  void shouldDelegateCharset() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.charset()).thenReturn(StandardCharsets.UTF_8);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    verify(delegate).charset();
  }

  @Test
  void shouldDelegateMd5Hash() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.md5Hash()).thenReturn("abc123def456");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.md5Hash()).isEqualTo("abc123def456");
    verify(delegate).md5Hash();
  }

  @Test
  void shouldDelegateKey() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.key()).thenReturn("project:src/test/go/file_test.go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.key()).isEqualTo("project:src/test/go/file_test.go");
    verify(delegate).key();
  }

  @Test
  void shouldDelegateIsFile() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.isFile()).thenReturn(true);

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile.isFile()).isTrue();
    verify(delegate).isFile();
  }

  @Test
  void shouldDelegateToString() {
    InputFile delegate = mock(InputFile.class);
    when(delegate.toString()).thenReturn("foo/bar.go");

    GoInputFile goInputFile = new GoInputFile(delegate);

    assertThat(goInputFile).hasToString("foo/bar.go");
  }
}
