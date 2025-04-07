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

import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.ParenthesizedExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.go.utils.ExpressionUtils.isLogicalBinaryExpression;
import static org.sonar.go.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1067")
public class TooComplexExpressionCheck implements GoCheck {

  private static final int DEFAULT_MAX_COMPLEXITY = 3;

  @RuleProperty(key = "max",
    description = "Maximum number of allowed conditional operators in an expression",
    defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
  public int max = DEFAULT_MAX_COMPLEXITY;

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (isParentExpression(ctx)) {
        var complexity = computeExpressionComplexity(tree);
        if (complexity > max) {
          var message = "Reduce the number of conditional operators (%s) used in the expression (maximum allowed %s)."
            .formatted(complexity, max);
          var gap = (double) complexity - max;
          ctx.reportIssue(tree, message, Collections.emptyList(), gap);
        }
      }
    });
  }

  private static boolean isParentExpression(CheckContext ctx) {
    var iterator = ctx.ancestors().iterator();
    while (iterator.hasNext()) {
      var parentExpression = iterator.next();
      if (parentExpression instanceof BinaryExpressionTree) {
        return false;
      } else if (!(parentExpression instanceof UnaryExpressionTree) || !(parentExpression instanceof ParenthesizedExpressionTree)) {
        return true;
      }
    }
    return true;
  }

  private static int computeExpressionComplexity(Tree originalTree) {
    var tree = skipParentheses(originalTree);
    if (tree instanceof BinaryExpressionTree binary) {
      var complexity = isLogicalBinaryExpression(tree) ? 1 : 0;
      return complexity
        + computeExpressionComplexity(binary.leftOperand())
        + computeExpressionComplexity(binary.rightOperand());
    } else if (tree instanceof UnaryExpressionTree unaryExpressionTree) {
      return computeExpressionComplexity(unaryExpressionTree.operand());
    } else {
      return 0;
    }
  }
}
