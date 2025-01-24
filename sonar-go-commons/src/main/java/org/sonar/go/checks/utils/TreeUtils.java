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
package org.sonar.go.checks.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

// Probably the sonar-go-frontend is a better place for this class after implementing SONARGO-97
public class TreeUtils {
  private TreeUtils() {
    // empty, util class
  }

  public static final Predicate<Tree> IS_NOT_SEMICOLON = Predicate.not(tree -> tree instanceof NativeTree nativeTree
    && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
    && stringNativeKind.toString().equals("Semicolon"));

  public static List<Tree> getAllChildren(Tree tree) {
    var result = new ArrayList<Tree>();
    var children = tree.children();
    while (!children.isEmpty()) {
      result.addAll(children);
      children = children.stream()
        .map(Tree::children)
        .flatMap(Collection::stream)
        .toList();
    }
    return result;
  }

  public static <T extends Tree> List<String> getIdentifierNames(List<T> trees) {
    return trees.stream().filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .toList();
  }

  public static <T extends Tree> String getIdentifierName(List<T> trees) {
    return trees.stream().filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .collect(Collectors.joining("."));
  }
}
