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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

public record MethodCall(String methodFqn, List<Tree> args) {
  private static final Predicate<String> IS_CALL_EXPR = Pattern.compile("\\[\\d++]\\(CallExpr\\)").asMatchPredicate().or("X(CallExpr)"::equals);

  @CheckForNull
  public static MethodCall of(NativeTree nativeTree) {
    if (NativeKinds.isStringNativeKind(nativeTree, IS_CALL_EXPR)) {
      var methodCall = nativeTree.children().get(0);
      var fqnMethod = treeToString(methodCall);
      var args = extractArgs(nativeTree);
      return new MethodCall(fqnMethod, args);
    }
    return null;
  }

  private static List<Tree> extractArgs(NativeTree nativeTree) {
    // Check if we have four elements: the method name, opening parenthesis, arguments, and closing parenthesis.
    if (nativeTree.children().size() == 4) {
      var args = (NativeTree) nativeTree.children().get(2);
      return args.children().stream()
        .filter(arg -> !(arg instanceof NativeTree nativeTree1 && nativeTree1.nativeKind() instanceof StringNativeKind stringNativeKind && stringNativeKind.toString().isEmpty()))
        .toList();
    }
    return Collections.emptyList();
  }

  private static String treeToString(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return identifierTree.name();
    } else if (tree instanceof MemberSelectTree memberSelectTree) {
      return treeToString(memberSelectTree.expression()) + "." + memberSelectTree.identifier().name();
    }
    return "";
  }

  public boolean is(String methodFqn) {
    return this.methodFqn.equals(methodFqn);
  }

  public boolean is(String methodFqn, String... args) {
    for (int i = 0; i < args.length; i++) {
      if (this.args.size() <= i || !treeToString(this.args.get(i)).equals(args[i])) {
        return false;
      }
    }
    return this.methodFqn.equals(methodFqn);
  }

  @Nullable
  public Tree getArg(int index) {
    if (this.args.size() > index) {
      return this.args.get(index);
    }
    return null;
  }
}
