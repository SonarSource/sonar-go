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
import java.nio.charset.Charset;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * The implementation of {@link InputFile} that always returns {@link Type#TEST}.
 * The rest of the methods are delegated to the original implementation passed by constructor.
 */
public class GoTestInputFile implements InputFile {

  private final InputFile delegate;

  public GoTestInputFile(InputFile inputFile) {
    this.delegate = inputFile;
  }

  @Override
  public Type type() {
    return Type.TEST;
  }

  public InputFile getDelegate() {
    return delegate;
  }

  @Override
  public String relativePath() {
    return delegate.relativePath();
  }

  @Override
  public String absolutePath() {
    return delegate.absolutePath();
  }

  @Override
  public File file() {
    return delegate.file();
  }

  @Override
  public Path path() {
    return delegate.path();
  }

  @Override
  public URI uri() {
    return delegate.uri();
  }

  @Override
  public String filename() {
    return delegate.filename();
  }

  @Nullable
  @Override
  public String language() {
    return delegate.language();
  }

  @Override
  public InputStream inputStream() throws IOException {
    return delegate.inputStream();
  }

  @Override
  public String contents() throws IOException {
    return delegate.contents();
  }

  @Override
  public Status status() {
    return delegate.status();
  }

  @Override
  public int lines() {
    return delegate.lines();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public TextPointer newPointer(int line, int lineOffset) {
    return delegate.newPointer(line, lineOffset);
  }

  @Override
  public TextRange newRange(TextPointer start, TextPointer end) {
    return delegate.newRange(start, end);
  }

  @Override
  public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    return delegate.newRange(startLine, startLineOffset, endLine, endLineOffset);
  }

  @Override
  public TextRange selectLine(int line) {
    return delegate.selectLine(line);
  }

  @Override
  public Charset charset() {
    return delegate.charset();
  }

  @Override
  public String md5Hash() {
    return delegate.md5Hash();
  }

  @Override
  public String key() {
    return delegate.key();
  }

  @Override
  public boolean isFile() {
    return delegate.isFile();
  }
}
