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
package org.sonar.go.persistence.conversion;

import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.plugins.go.api.TextPointer;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public final class RangeConverter {

  private RangeConverter() {
  }

  @Nullable
  public static String format(@Nullable TextRange range) {
    if (range == null) {
      return null;
    }
    TextPointer start = range.start();
    TextPointer end = range.end();

    String endLine = start.line() == end.line() ? "" : Integer.toString(end.line());
    return start.line() + ":" + start.lineOffset() + ":" + endLine + ":" + end.lineOffset();
  }

  @Nullable
  public static TextRange parse(@Nullable String value) {
    if (value == null) {
      return null;
    }
    String[] values = value.split(":", 4);
    if (values.length != 4) {
      throw new IllegalArgumentException("Invalid TextRange '" + value + "'");
    }
    int startLine = Integer.parseInt(values[0]);
    int startLineOffset = Integer.parseInt(values[1]);
    int endLine = values[2].isEmpty() ? startLine : Integer.parseInt(values[2]);
    int endLineOffset = Integer.parseInt(values[3]);
    return new TextRangeImpl(startLine, startLineOffset, endLine, endLineOffset);
  }

  @Nullable
  public static String tokenReference(@Nullable Token token) {
    if (token == null) {
      return null;
    }
    return format(token.textRange());
  }

  @Nullable
  public static Token resolveToken(TreeMetaDataProvider metaDataProvider, @Nullable String tokenReference) {
    TextRange range = parse(tokenReference);
    if (range == null) {
      return null;
    }
    return metaDataProvider.firstToken(range)
      .orElseThrow(() -> new NoSuchElementException("Token not found: " + tokenReference));
  }

  public static String metaDataReference(Tree tree) {
    return format(tree.metaData().textRange());
  }

  public static TreeMetaData resolveMetaData(TreeMetaDataProvider metaDataProvider, String metaDataReference) {
    return metaDataProvider.metaData(parse(metaDataReference));
  }

  @Nullable
  public static String treeReference(@Nullable Tree tree) {
    if (tree == null) {
      return null;
    }
    return format(tree.metaData().textRange());
  }

  @Nullable
  public static <T extends Tree> T resolveNullableTree(Tree parent, @Nullable String treeReference, Class<T> childClass) {
    if (treeReference == null) {
      return null;
    }
    TextRange range = parse(treeReference);
    return Stream.concat(Stream.of(parent), parent.descendants())
      .filter(child -> child.textRange().equals(range))
      .filter(childClass::isInstance)
      .map(childClass::cast)
      .findFirst()
      .orElse(null);
  }

}
