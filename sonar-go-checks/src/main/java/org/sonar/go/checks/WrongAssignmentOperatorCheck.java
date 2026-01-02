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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.go.impl.TextRanges;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static java.util.Arrays.asList;
import static org.sonar.plugins.go.api.AssignmentExpressionTree.Operator.EQUAL;
import static org.sonar.plugins.go.api.UnaryExpressionTree.Operator;

@Rule(key = "S2757")
public class WrongAssignmentOperatorCheck implements GoCheck {

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
        ctx.reportIssue(range, getMessage(expressionFirstToken));
      }
    });
  }

  private static String getMessage(Token expressionFirstToken) {
    if ("!".equals(expressionFirstToken.text())) {
      // Assignments are statements in go, we cannot have code such as "a = b =! c" to be confused with "a = b != c".
      return "Add a space between \"=\" and \"!\" to avoid confusion.";
    }
    return "Was \"" + expressionFirstToken.text() + "=\" meant instead?";
  }

  private static boolean hasSpacingBetween(Token firstToken, Token secondToken) {
    return firstToken.textRange().end().line() != secondToken.textRange().start().line()
      || firstToken.textRange().end().lineOffset() != secondToken.textRange().start().lineOffset();
  }

  private static boolean isSuspiciousUnaryExpression(Tree tree) {
    // A tree is suspicious if a compound assignment operator exists for the current unary operator (ex. "=+" and "+=").
    // Currently, the rule only covers "+", "-" and "!" operators, even if there are more cases that could be confusing.
    // Note 1: "++" and "--" are unary statements in Go, they cannot appear at this point of the logic.
    // Note 2: not only we want to cover only the three operator listed above, only these three are actually map to the ast.
    // It means that the condition will never be false in the current state. We still keep it to support future changes.
    return tree instanceof UnaryExpressionTree unary && SUSPICIOUS_UNARY_OPERATORS.contains(unary.operator());
  }

}
