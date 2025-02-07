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
package org.sonar.go.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.KeyValueTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.Tree;

// TODO SONARGO-255 remove this redundant class and move its methods somwhere else
public record CompositeLiteral(@Nullable Tree type, List<Tree> elements) {

  public static Optional<CompositeLiteral> of(CompositeLiteralTree compositeLiteralTree) {
    return Optional.of(new CompositeLiteral(compositeLiteralTree.type(), compositeLiteralTree.elements()));
  }

  /**
   * Returns a Stream of KeyValueTree elements. A composite literal in Go either has all elements as KeyValueTree (field initialization),
   * or all elements as non-KeyValueTree (value initialization, when field names are omitted). Mixture of value and field initialization
   * is not allowed.
   */
  public Stream<KeyValueTree> getKeyValuesElements() {
    return elements.stream()
      .filter(KeyValueTree.class::isInstance)
      .map(KeyValueTree.class::cast);
  }

  public boolean hasType(String packageName, String typeName) {
    return type instanceof MemberSelectTree memberSelectTree
      && typeName.equals(memberSelectTree.identifier().name())
      && memberSelectTree.expression() instanceof IdentifierTree identifierTree
      && packageName.equals(identifierTree.name());
  }
}
