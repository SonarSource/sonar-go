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
package org.sonar.go.checks;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

/**
 * Util class to work with native kinds.
 * Note: Probably the sonar-go-frontend is a better place for this class after implementing SONARGO-97.
 * <p>
 * Now it includes a collection of methods to access and check native kinds of NativeTree objects.
 * Methods {@code is<X>} check if the native kind starts with the given string.
 * Methods {@code isFrom<X>} check if the native kind contains the given string in parentheses.
 */
public final class NativeKinds {
  public static final String LABEL = "LabeledStmt";
  public static final String SEMICOLON = "Semicolon";
  public static final String FUNCTION_CALL = "X(CallExpr)";
  public static final String ARGUMENTS = "Args([]Expr)";
  public static final Predicate<String> IS_BINARY_EXPR = Pattern.compile("\\[\\d++]\\(BinaryExpr\\)|[A-Z]\\(BinaryExpr\\)").asMatchPredicate();

  private static final String IMPORT_SUFFIX = "](ImportSpec)";
  public static final String METHOD_RECEIVER_SUFFIX = "]*Ident)";

  private NativeKinds() {
  }

  public static boolean isFun(NativeTree tree) {
    return isMainKind("Fun").test(tree);
  }

  public static boolean isX(NativeTree tree) {
    return isMainKind("X").test(tree);
  }

  public static boolean isFromSelectorExpr(NativeTree tree) {
    return isFrom("SelectorExpr").test(tree);
  }

  public static boolean isFromCallExpr(NativeTree tree) {
    return isFrom("CallExpr").test(tree);
  }

  public static boolean isStringNativeKind(@Nullable Tree tree, Predicate<String> predicate) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && predicate.test(stringNativeKind.toString());
  }

  public static boolean isStringNativeKindOfType(Tree tree, String type) {
    return isStringNativeKind(tree, type::equals);
  }

  public static boolean isFunctionCall(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().contains("CallExpr");
  }

  public static boolean isStringNativeKind(NativeKind nativeKind, Predicate<String> predicate) {
    return nativeKind instanceof StringNativeKind stringNativeKind && predicate.test(stringNativeKind.toString());
  }

  private static Predicate<NativeTree> isMainKind(String nativeMainKind) {
    return tree -> tree.nativeKind().toString().startsWith(nativeMainKind + "(");
  }

  public static Predicate<NativeTree> isFrom(String nativeSubKind) {
    return tree -> tree.nativeKind().toString().endsWith("(" + nativeSubKind + ")");
  }

  /**
   * For following Go code:
   * <pre>
   *   {@code
   *   func (ctrl *MyController) users() {}
   *   }
   * </pre>
   * the {@code ctrl} is method receiver
   */
  public static boolean isMethodReceiverTree(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().endsWith(METHOD_RECEIVER_SUFFIX);
  }
}
