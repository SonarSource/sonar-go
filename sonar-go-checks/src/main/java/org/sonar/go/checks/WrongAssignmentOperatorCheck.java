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

import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.UnaryExpressionTree.Operator;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextRanges;

import static java.util.Arrays.asList;
import static org.sonarsource.slang.api.AssignmentExpressionTree.Operator.EQUAL;

@Rule(key = "S2757")
public class WrongAssignmentOperatorCheck implements SlangCheck {

  private static final List<Operator> SUSPICIOUS_UNARY_OPERATORS = List.of(Operator.NEGATE, Operator.PLUS, Operator.MINUS);

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, assignment) -> {
      var rightHandSide = assignment.statementOrExpression();
      if (assignment.operator() != EQUAL || !isSuspiciousUnaryExpression(rightHandSide)) {
        return;
      }

      var leftHandSideTokens = assignment.leftHandSide().metaData().tokens();
      var variableLastToken = leftHandSideTokens.get(leftHandSideTokens.size() - 1);

      var allTokens = assignment.metaData().tokens();
      var operatorToken = allTokens.get(allTokens.indexOf(variableLastToken) + 1);

      var expressionFirstToken = rightHandSide.metaData().tokens().get(0);

      if (!hasSpacingBetween(operatorToken, expressionFirstToken) && hasSpacingBetween(variableLastToken, operatorToken)) {
        var range = TextRanges.merge(asList(operatorToken.textRange(), expressionFirstToken.textRange()));
        ctx.reportIssue(range, getMessage(expressionFirstToken, ctx));
      }
    });
  }

  private static String getMessage(Token expressionFirstToken, CheckContext aeTree) {
    if (isSingleNegationAssignment(expressionFirstToken, aeTree)) {
      // For expressions such as "a = b =! c" we want to display the other message
      return "Add a space between \"=\" and \"!\" to avoid confusion.";
    }
    return "Was \"" + expressionFirstToken.text() + "=\" meant instead?";
  }

  private static boolean isSingleNegationAssignment(Token firstToken, CheckContext aeTree) {
    return "!".equals(firstToken.text()) && !(aeTree.parent() instanceof AssignmentExpressionTree);
  }

  private static boolean hasSpacingBetween(Token firstToken, Token secondToken) {
    return firstToken.textRange().end().line() != secondToken.textRange().start().line()
      || firstToken.textRange().end().lineOffset() != secondToken.textRange().start().lineOffset();
  }

  private static boolean isSuspiciousUnaryExpression(Tree tree) {
    return tree instanceof UnaryExpressionTree unary && SUSPICIOUS_UNARY_OPERATORS.contains(unary.operator());
  }

}
