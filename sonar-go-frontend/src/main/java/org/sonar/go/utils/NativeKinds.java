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

import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

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

  private NativeKinds() {
  }

  public static boolean isStringNativeKind(@Nullable Tree tree, Predicate<String> predicate) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && predicate.test(stringNativeKind.toString());
  }

  public static boolean isStringNativeKindOfType(Tree tree, String type) {
    return isStringNativeKind(tree, type::equals);
  }

  public static boolean isCompositeLit(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().contains("CompositeLit");
  }

  public static boolean isKeyValueExpr(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().contains("KeyValueExpr");
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
  public static boolean isMethodReceiverTreeIdentifier(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().endsWith(METHOD_RECEIVER_SUFFIX);
  }
}
