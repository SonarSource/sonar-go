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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.JumpTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.ModifierTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.UnaryExpressionTree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.visitors.TreePrinter;

public class SyntacticEquivalence {

  private SyntacticEquivalence() {
  }

  public static boolean areEquivalent(@Nullable List<? extends Tree> first, @Nullable List<? extends Tree> second) {
    if (first == second) {
      return true;
    }

    if (first == null || second == null || first.size() != second.size()) {
      return false;
    }

    for (int i = 0; i < first.size(); i++) {
      if (!areEquivalent(first.get(i), second.get(i))) {
        return false;
      }
    }

    return true;
  }

  public static boolean areEquivalent(@Nullable Tree first, @Nullable Tree second) {
    if (first == second) {
      return true;
    }

    if (first == null || second == null || !first.getClass().equals(second.getClass())) {
      return false;
    }

    if (first instanceof IdentifierTree) {
      return getUniqueIdentifier((IdentifierTree) first).equals(getUniqueIdentifier((IdentifierTree) second));
    } else if (first instanceof LiteralTree) {
      return ((LiteralTree) first).value().equals(((LiteralTree) second).value());
    } else if (hasDifferentFields(first, second)) {
      return false;
    } else if (first instanceof NativeTree && first.children().isEmpty()) {
      return areEquivalentTokenText(first.metaData().tokens(), second.metaData().tokens());
    }
    return areEquivalent(first.children(), second.children());
  }

  public static String getUniqueIdentifier(IdentifierTree identifier) {
    return identifier.identifier();
  }

  private static boolean areEquivalentTokenText(List<Token> firstList, List<Token> secondList) {
    if (firstList.size() != secondList.size()) {
      return false;
    }
    for (int i = 0; i < firstList.size(); i++) {
      if (!firstList.get(i).text().equals(secondList.get(i).text())) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasDifferentFields(Tree first, Tree second) {
    boolean nativeTreeCheck = (first instanceof NativeTree) && (!((NativeTree) first).nativeKind().equals(((NativeTree) second).nativeKind()));
    boolean unaryTreeCheck = (first instanceof UnaryExpressionTree) && ((UnaryExpressionTree) first).operator() != ((UnaryExpressionTree) second).operator();
    boolean binaryTreeCheck = (first instanceof BinaryExpressionTree) && (((BinaryExpressionTree) first).operator() != ((BinaryExpressionTree) second).operator());
    boolean assignTreeCheck = (first instanceof AssignmentExpressionTree) && (((AssignmentExpressionTree) first).operator() != ((AssignmentExpressionTree) second).operator());
    boolean vardeclTreeCheck = (first instanceof VariableDeclarationTree) && (((VariableDeclarationTree) first).isVal() != ((VariableDeclarationTree) second).isVal());
    boolean loopTreeCheck = (first instanceof LoopTree)
      && ((((LoopTree) first).kind() != ((LoopTree) second).kind()) || !(((LoopTree) first).keyword().text().equals(((LoopTree) second).keyword().text())));
    boolean modifierTreeCheck = (first instanceof ModifierTree) && (((ModifierTree) first).kind() != ((ModifierTree) second).kind());
    boolean jumpTreeCheck = (first instanceof JumpTree) && (((JumpTree) first).kind() != ((JumpTree) second).kind());
    return nativeTreeCheck || unaryTreeCheck || binaryTreeCheck || assignTreeCheck || vardeclTreeCheck || loopTreeCheck || modifierTreeCheck || jumpTreeCheck;
  }

  public static List<List<Tree>> findDuplicatedGroups(List<Tree> list) {
    return list.stream()
      .collect(Collectors.groupingBy(ComparableTree::new, LinkedHashMap::new, Collectors.toList()))
      .values().stream()
      .filter(group -> group.size() > 1)
      .toList();
  }

  static class ComparableTree {

    private final Tree tree;
    private final int hash;

    ComparableTree(Tree tree) {
      this.tree = tree;
      hash = computeHash(tree);
    }

    private static int computeHash(@Nullable Tree tree) {
      if (tree == null) {
        return 0;
      }
      return TreePrinter.tree2string(tree).hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ComparableTree)) {
        return false;
      }
      ComparableTree that = (ComparableTree) other;
      return hash == that.hash && areEquivalent(tree, ((ComparableTree) other).tree);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }
}
