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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ImportDeclarationTree;
import org.sonar.go.api.ImportSpecificationTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

public class TreeUtils {
  private TreeUtils() {
    // empty, util class
  }

  public static final Predicate<Tree> IS_NOT_SEMICOLON = Predicate.not(tree -> tree instanceof NativeTree nativeTree
    && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
    && stringNativeKind.toString().equals("Semicolon"));

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

  public static Set<String> getImportsAsStrings(TopLevelTree file) {
    return file.declarations().stream()
      .filter(ImportDeclarationTree.class::isInstance)
      .flatMap(it -> it.children().stream())
      .filter(ImportSpecificationTree.class::isInstance)
      .map(ImportSpecificationTree.class::cast)
      // Imports with aliases are not supported currently, they are filtered-out to avoid false positives
      .filter(spec -> spec.name() == null)
      .map(spec -> spec.path().content())
      .collect(Collectors.toSet());
  }

  public static String treeToString(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return identifierTree.name();
    } else if (tree instanceof MemberSelectTree memberSelectTree) {
      return treeToString(memberSelectTree.expression()) + "." + memberSelectTree.identifier().name();
    }
    return "";
  }

  public static String methodFnq(FunctionInvocationTree tree) {
    return treeToString(tree.memberSelect());
  }
}
