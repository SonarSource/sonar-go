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

import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.ParenthesizedExpressionTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.Tree;

public class ConstantResolution {

  public static final String PLACEHOLDER = "_?_";

  private ConstantResolution() {
    // Utility class
  }

  /**
   * Try to resolve the tree as a String constant.
   * For any tree that cannot be resolved to a constant, a placeholder "_?_" will be introduced instead.
   */
  public static String resolveAsStringConstant(Tree tree) {
    if (tree instanceof StringLiteralTree stringLiteral) {
      return stringLiteral.content();
    } else if (tree instanceof BinaryExpressionTree binaryExpressionTree && binaryExpressionTree.operator() == BinaryExpressionTree.Operator.PLUS) {
      return resolveAsStringConstant(binaryExpressionTree.leftOperand()) + resolveAsStringConstant(binaryExpressionTree.rightOperand());
    } else if (tree instanceof ParenthesizedExpressionTree parenthesizedExpression) {
      return resolveAsStringConstant(parenthesizedExpression.expression());
    }
    return PLACEHOLDER;
  }

}
