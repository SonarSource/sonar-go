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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.checks.NativeKinds;

public record MethodCall(String methodFqn, List<String> args) {
  private static final Predicate<String> IS_EXPR_STMT = Pattern.compile("\\[\\d++]\\(ExprStmt\\)").asMatchPredicate();
  private static final Predicate<String> IS_SELECTOR_EXPR = Pattern.compile("\\[\\d++]\\(SelectorExpr\\)").asMatchPredicate().or("X(SelectorExpr)"::equals);
  private static final Predicate<String> IS_CALL_EXPR = "X(CallExpr)"::equals;
  private static final Predicate<String> IS_FUN_SELECTOR_EXPR = "Fun(SelectorExpr)"::equals;

  @CheckForNull
  public static MethodCall of(NativeTree nativeTree) {
    if (NativeKinds.isStringNativeKind(nativeTree.nativeKind(), IS_EXPR_STMT)
      && nativeTree.children().get(0) instanceof NativeTree callExprNativeTree && NativeKinds.isStringNativeKind(callExprNativeTree.nativeKind(), IS_CALL_EXPR)) {
      var methodCall = callExprNativeTree.children().get(0);
      var fqnMethod = treeToString(methodCall);
      var args = extractArgs(callExprNativeTree);
      return new MethodCall(fqnMethod, args);
    }
    return null;
  }

  private static List<String> extractArgs(NativeTree nativeTree) {
    // Check if we have four elements: the method name, opening parenthesis, arguments, and closing parenthesis.
    if (nativeTree.children().size() == 4) {
      var args = (NativeTree) nativeTree.children().get(2);
      return args.children().stream()
        .map(MethodCall::treeToString)
        .filter(s -> !s.isEmpty())
        .toList();
    }
    return Collections.emptyList();
  }

  private static String treeToString(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return identifierTree.name();
    } else if (tree instanceof NativeTree nativeTree
      && (NativeKinds.isStringNativeKind(nativeTree.nativeKind(), IS_SELECTOR_EXPR) || NativeKinds.isStringNativeKind(nativeTree.nativeKind(), IS_FUN_SELECTOR_EXPR))) {
        return nativeTreeChildrenToString(nativeTree);
      }
    return "";
  }

  private static String nativeTreeChildrenToString(NativeTree nativeTree) {
    return String.join(".", nativeTree.children().stream()
      .map(MethodCall::treeToString)
      .filter(s -> !s.isEmpty())
      .toList());
  }

  public boolean is(String methodFqn) {
    return this.methodFqn.equals(methodFqn);
  }

  public boolean is(String methodFqn, String... args) {
    for (int i = 0; i < args.length; i++) {
      if (this.args.size() <= i || !this.args.get(i).equals(args[i])) {
        return false;
      }
    }
    return this.methodFqn.equals(methodFqn);
  }
}
