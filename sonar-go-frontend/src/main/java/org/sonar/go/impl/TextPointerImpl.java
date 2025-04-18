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
package org.sonar.go.impl;

import java.util.Objects;
import org.sonar.plugins.go.api.TextPointer;

public class TextPointerImpl implements TextPointer {

  private final int line;
  private final int lineOffset;

  public TextPointerImpl(int line, int lineOffset) {
    this.line = line;
    this.lineOffset = lineOffset;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int lineOffset() {
    return lineOffset;
  }

  @Override
  public int compareTo(TextPointer other) {
    int lineCompare = Integer.compare(this.line(), other.line());
    if (lineCompare != 0) {
      return lineCompare;
    }
    return Integer.compare(this.lineOffset(), other.lineOffset());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextPointerImpl that = (TextPointerImpl) o;
    return line == that.line && lineOffset == that.lineOffset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, lineOffset);
  }

}
