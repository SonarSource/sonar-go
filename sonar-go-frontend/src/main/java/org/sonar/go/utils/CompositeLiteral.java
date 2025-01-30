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
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;

public record CompositeLiteral(Tree type, List<Tree> elements) {

  public static Optional<CompositeLiteral> of(NativeTree nativeTree) {
    List<Tree> children = nativeTree.children();
    if (NativeKinds.isCompositeLit(nativeTree)) {
      return Optional.of(new CompositeLiteral(children.get(0), getCompositeElements(nativeTree)));
    }
    return Optional.empty();
  }

  private static List<Tree> getCompositeElements(NativeTree nativeTree) {
    return nativeTree.children().stream()
      .filter(n -> NativeKinds.isStringNativeKind(n, t -> t.contains("Elts")))
      .flatMap(n -> n.children().stream())
      .toList();
  }

  public Stream<KeyValue> getKeyValuesElements() {
    return elements.stream()
      .filter(NativeTree.class::isInstance)
      .map(NativeTree.class::cast)
      .flatMap(element -> KeyValue.of(element).stream());
  }
}
