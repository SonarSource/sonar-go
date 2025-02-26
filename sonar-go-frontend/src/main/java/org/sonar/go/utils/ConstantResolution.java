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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ParenthesizedExpressionTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.Tree;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;

public class ConstantResolution {

  public static final String PLACEHOLDER = "_?_";

  private ConstantResolution() {
    // Utility class
  }

  /**
   * Try to resolve the tree as a String constant.
   * For any tree that cannot be resolved to a constant, a placeholder "_?_" will be introduced instead.
   */
  public static String resolveAsStringConstant(@Nullable Tree tree) {
    return resolveAsStringConstant(tree, true);
  }

  /**
   * @return true when the tree represents a static string constant, false otherwise.
   */
  public static boolean isConstantString(Tree tree) {
    return !resolveAsStringConstant(tree).contains(PLACEHOLDER);
  }

  /**
   * In order to avoid any risk of infinite recursion, we only follow identifier value once, depending on "followIdentifier" argument.
   * This approximation seems reasonable because we want to support the obvious cases (global constant, local variable used as constant),
   * and not trying to build a complex constant folding logic.
   */
  private static String resolveAsStringConstant(@Nullable Tree tree, boolean followIdentifier) {
    if (tree == null) {
      return PLACEHOLDER;
    }
    if (tree instanceof StringLiteralTree stringLiteral) {
      return stringLiteral.content();
    } else if (tree instanceof BinaryExpressionTree binaryExpressionTree && binaryExpressionTree.operator() == BinaryExpressionTree.Operator.PLUS) {
      return resolveAsStringConstant(binaryExpressionTree.leftOperand(), followIdentifier) + resolveAsStringConstant(binaryExpressionTree.rightOperand(), followIdentifier);
    } else if (tree instanceof ParenthesizedExpressionTree parenthesizedExpression) {
      return resolveAsStringConstant(parenthesizedExpression.expression(), followIdentifier);
    } else if (tree instanceof IdentifierTree identifier && followIdentifier) {
      return resolveIdentifierAsStringConstant(identifier);
    }
    return PLACEHOLDER;
  }

  private static String resolveIdentifierAsStringConstant(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (symbol == null) {
      return PLACEHOLDER;
    }
    return getEffectivelyFinalUsage(symbol)
      .map(Usage::value)
      .map(v -> resolveAsStringConstant(v, false))
      .orElse(PLACEHOLDER);
  }

  /**
   * An identifier is effectively final if it is never reassigned.
   */
  public static Optional<Usage> getEffectivelyFinalUsage(Symbol symbol) {
    Usage effectivelyFinalUsage = null;
    for (Usage usage : symbol.getUsages()) {
      if (usage.type() == Usage.UsageType.DECLARATION) {
        // An identifier with multiple declarations should never happen, but if it ever does, we don't consider it as effectively final.
        if (effectivelyFinalUsage != null) {
          return Optional.empty();
        }
        effectivelyFinalUsage = usage;
      } else if (usage.type() == Usage.UsageType.ASSIGNMENT) {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(effectivelyFinalUsage);
  }
}
