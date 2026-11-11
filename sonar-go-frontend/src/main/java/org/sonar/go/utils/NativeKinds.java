/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.Tree;

/**
 * Util class to work with native kinds.
 * <p>
 * Now it includes a collection of methods to access and check native kinds of NativeTree objects.
 * Methods {@code is<X>} check if the native kind starts with the given string.
 * Methods {@code isFrom<X>} check if the native kind contains the given string in parentheses.
 */
public final class NativeKinds {
  public static final String LABEL = "LabeledStmt";
  public static final String SEMICOLON = "Semicolon";
  public static final Predicate<String> IS_BINARY_EXPR = Pattern.compile("\\[\\d++]\\(BinaryExpr\\)|[A-Z]\\(BinaryExpr\\)").asMatchPredicate();

  public static final String METHOD_RECEIVER_SUFFIX = "]*Ident)";
  public static final String DEFER_STATEMENT_SUFFIX = "(DeferStmt)";

  private NativeKinds() {
  }

  public static boolean isStringNativeKind(@Nullable Tree tree, Predicate<String> predicate) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && predicate.test(stringNativeKind.kind());
  }

  public static boolean isStringNativeKindOfType(@Nullable Tree tree, String type) {
    return isStringNativeKind(tree, type::equals);
  }

  public static boolean isStringNativeKindOfType(Tree tree, String type, String subtype) {
    return isStringNativeKind(tree, s -> s.startsWith(type + "(") && s.endsWith(subtype + ")"));
  }

  public static boolean isDeferStatement(@Nullable Tree tree) {
    return isStringNativeKind(tree, kind -> kind.endsWith(DEFER_STATEMENT_SUFFIX));
  }

  /**
   * Returns the function invocation deferred by a {@code defer} statement, or empty when the tree is not a {@code defer} statement.
   */
  public static Optional<FunctionInvocationTree> deferredInvocation(@Nullable Tree tree) {
    if (!isDeferStatement(tree)) {
      return Optional.empty();
    }
    return tree.children().stream()
      .filter(FunctionInvocationTree.class::isInstance)
      .map(FunctionInvocationTree.class::cast)
      .findFirst();
  }

  public static boolean isCompositeLit(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.kind().contains("CompositeLit");
  }

  public static boolean isKeyValueExpr(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.kind().contains("KeyValueExpr");
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
  public static boolean isMethodReceiverTreeIdentifier(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.kind().endsWith(METHOD_RECEIVER_SUFFIX);
  }
}
