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
import java.nio.charset.Charset;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * It is kind of wrapper or Anti-Corruption Layer around {@link InputFile} to use internally in this analyzer.
 * It doesn't implement {@link InputFile} but contains the same set of methods.
 * Only the {@link #type()} has different implementation and {@link #getDelegate()} is added to get original {@link InputFile}
 * that needs to be passed to plugin-api methods.
 * The {@link InputFile} can't be implemented on analyzer side, because in SQS the {@link DefaultInputFile} is used as
 * implementation and will cause {@link ClassCastException}.
 */
public class GoInputFile {

  private final InputFile delegate;
  private final boolean testFile;

  public GoInputFile(InputFile inputFile) {
    this(inputFile, false);
  }

  public GoInputFile(InputFile inputFile, boolean testFile) {
    this.delegate = inputFile;
    this.testFile = testFile;
  }

  public InputFile.Type type() {
    if (testFile) {
      return InputFile.Type.TEST;
    } else {
      return delegate.type();
    }
  }

  public InputFile getDelegate() {
    return delegate;
  }

  public String relativePath() {
    return delegate.relativePath();
  }

  public String absolutePath() {
    return delegate.absolutePath();
  }

  public File file() {
    return delegate.file();
  }

  public Path path() {
    return delegate.path();
  }

  public URI uri() {
    return delegate.uri();
  }

  public String filename() {
    return delegate.filename();
  }

  @Nullable

  public String language() {
    return delegate.language();
  }

  public InputStream inputStream() throws IOException {
    return delegate.inputStream();
  }

  public String contents() throws IOException {
    return delegate.contents();
  }

  public InputFile.Status status() {
    return delegate.status();
  }

  public int lines() {
    return delegate.lines();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public TextPointer newPointer(int line, int lineOffset) {
    return delegate.newPointer(line, lineOffset);
  }

  public TextRange newRange(TextPointer start, TextPointer end) {
    return delegate.newRange(start, end);
  }

  public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    return delegate.newRange(startLine, startLineOffset, endLine, endLineOffset);
  }

  public TextRange selectLine(int line) {
    return delegate.selectLine(line);
  }

  public Charset charset() {
    return delegate.charset();
  }

  public String md5Hash() {
    return delegate.md5Hash();
  }

  public String key() {
    return delegate.key();
  }

  public boolean isFile() {
    return delegate.isFile();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
