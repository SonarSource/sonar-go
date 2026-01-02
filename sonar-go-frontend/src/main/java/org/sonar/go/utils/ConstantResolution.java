/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.math.BigInteger;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.go.symbols.Symbol;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.HasSymbol;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.ParenthesizedExpressionTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.Tree;

public class ConstantResolution {

  public static final String PLACEHOLDER = "_?_";
  private static final int MAX_IDENTIFIER_RESOLUTION = 20;

  private ConstantResolution() {
    // Utility class
  }

  /**
   * Try to resolve the tree as a String constant.
   * For any tree that cannot be resolved to a constant, null is returned.
   */
  @CheckForNull
  public static String resolveAsStringConstant(@Nullable Tree tree) {
    var resolvedConstant = resolveAsPartialStringConstant(tree);
    if (resolvedConstant.contains(PLACEHOLDER)) {
      return null;
    }
    return resolvedConstant;
  }

  /**
   * @return true when the tree represents a static string constant, false otherwise.
   */
  public static boolean isConstantString(Tree tree) {
    return !resolveAsPartialStringConstant(tree).contains(PLACEHOLDER);
  }

  /**
   * Try to resolve this tree as a string constant.
   * If there are nodes that cannot be resolved, they are replaced by {@link #PLACEHOLDER}.
   * This way, the result can still be used to detect certain patterns involving string concatenation, e.g.
   * {@code "/tmp/" + fileName} will be resolved to {@code "/tmp/_?_"}, and will contain information that the string describes a temporary file.
   * To avoid infinite recursion, we call the method {@link #resolveAsPartialStringConstant(Tree, int)} where the
   * second parameter is a counter set initially to {@link #MAX_IDENTIFIER_RESOLUTION}. Everytime it resolve an identifier through
   * {@link #resolveIdentifierAsStringConstant(IdentifierTree, int)}, the counter is decremented. If it reaches 0, it stop the resolution and
   * return {@link #PLACEHOLDER}.
   */
  @Nonnull
  public static String resolveAsPartialStringConstant(@Nullable Tree tree) {
    return resolveAsPartialStringConstant(tree, MAX_IDENTIFIER_RESOLUTION);
  }

  @Nonnull
  private static String resolveAsPartialStringConstant(@Nullable Tree tree, int remainingIdentifierResolution) {
    if (tree == null || remainingIdentifierResolution == 0) {
      return PLACEHOLDER;
    }
    if (tree instanceof StringLiteralTree stringLiteral) {
      return stringLiteral.content();
    } else if (tree instanceof BinaryExpressionTree binaryExpressionTree && binaryExpressionTree.operator() == BinaryExpressionTree.Operator.PLUS) {
      return resolveAsPartialStringConstant(binaryExpressionTree.leftOperand(), remainingIdentifierResolution)
        + resolveAsPartialStringConstant(binaryExpressionTree.rightOperand(), remainingIdentifierResolution);
    } else if (tree instanceof ParenthesizedExpressionTree parenthesizedExpression) {
      return resolveAsPartialStringConstant(parenthesizedExpression.expression(), remainingIdentifierResolution);
    } else if (tree instanceof IdentifierTree identifier) {
      return resolveIdentifierAsStringConstant(identifier, remainingIdentifierResolution);
    }
    return PLACEHOLDER;
  }

  private static String resolveIdentifierAsStringConstant(IdentifierTree identifier, int remainingIdentifierResolution) {
    Symbol symbol = identifier.symbol();
    if (symbol == null) {
      return PLACEHOLDER;
    }
    return Optional.ofNullable(symbol.getSafeValue())
      .map(value -> resolveAsPartialStringConstant(value, remainingIdentifierResolution - 1))
      .orElse(PLACEHOLDER);
  }

  @CheckForNull
  public static BigInteger evaluateArithmeticExpression(Tree tree) {
    if (tree instanceof IntegerLiteralTree integerLiteral) {
      return integerLiteral.getIntegerValue();
    } else if (tree instanceof ParenthesizedExpressionTree parenthesizedExpression) {
      return evaluateArithmeticExpression(parenthesizedExpression.expression());
    } else if (tree instanceof HasSymbol hasSymbol && hasSymbol.symbol() != null) {
      var safeValue = hasSymbol.symbol().getSafeValue();
      if (safeValue != null) {
        return evaluateArithmeticExpression(safeValue);
      }
    } else if (tree instanceof BinaryExpressionTree binaryExpression) {
      // Go parser already produces the tree with the correct order of operations, so we can simply evaluate operands recursively
      var left = evaluateArithmeticExpression(binaryExpression.leftOperand());
      var right = evaluateArithmeticExpression(binaryExpression.rightOperand());
      if (left == null || right == null) {
        return null;
      }
      return switch (binaryExpression.operator()) {
        case PLUS -> left.add(right);
        case MINUS -> left.subtract(right);
        case TIMES -> left.multiply(right);
        case DIVIDED_BY -> right.equals(BigInteger.ZERO) ? null : left.divide(right);
        default -> null;
      };
    }
    return null;
  }
}
