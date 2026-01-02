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
package org.sonar.go.checks;

import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.go.utils.ExpressionUtils;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.go.utils.ExpressionUtils.isBinaryOperation;
import static org.sonar.go.utils.ExpressionUtils.isBooleanLiteral;
import static org.sonar.go.utils.ExpressionUtils.isFalseValueLiteral;
import static org.sonar.go.utils.ExpressionUtils.isNegation;
import static org.sonar.go.utils.ExpressionUtils.isTrueValueLiteral;
import static org.sonar.go.utils.ExpressionUtils.skipParentheses;
import static org.sonar.plugins.go.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonar.plugins.go.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;

@Rule(key = "S1145")
public class IfConditionalAlwaysTrueOrFalseCheck implements GoCheck {

  public static final String MESSAGE_TEMPLATE = "Remove this useless \"%s\" statement.";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> {
      var condition = ifTree.condition();
      if (isAlwaysTrueOrFalse(condition)) {
        var message = String.format(MESSAGE_TEMPLATE, ifTree.ifKeyword().text());
        ctx.reportIssue(condition, message);
      }
    });
  }

  private static boolean isAlwaysTrueOrFalse(Tree originalCondition) {
    var condition = skipParentheses(originalCondition);
    return isBooleanLiteral(condition)
      || isTrueValueLiteral(condition)
      || isFalseValueLiteral(condition)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_AND, ExpressionUtils::isFalseValueLiteral)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_OR, ExpressionUtils::isTrueValueLiteral);
  }

  private static boolean isSimpleExpressionWithLiteral(Tree condition, BinaryExpressionTree.Operator operator, Predicate<? super Tree> hasLiteralValue) {
    boolean simpleExpression = isBinaryOperation(condition, operator)
      && condition.descendants()
        .map(ExpressionUtils::skipParentheses)
        .allMatch(tree -> tree instanceof IdentifierTree
          || tree instanceof LiteralTree
          || isNegation(tree)
          || isBinaryOperation(tree, operator));

    return simpleExpression && condition.descendants().anyMatch(hasLiteralValue);
  }

}
