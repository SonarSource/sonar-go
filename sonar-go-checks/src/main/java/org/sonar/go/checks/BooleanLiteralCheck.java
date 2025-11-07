/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.go.utils.ExpressionUtils;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S1125")
public class BooleanLiteralCheck implements GoCheck {
  private static final List<BinaryExpressionTree.Operator> CONDITIONAL_BINARY_OPERATORS = Arrays.asList(
    BinaryExpressionTree.Operator.CONDITIONAL_AND,
    BinaryExpressionTree.Operator.CONDITIONAL_OR);

  private static final String MESSAGE = "Remove the unnecessary Boolean literal.";

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, binaryExprTree) -> {
      if (CONDITIONAL_BINARY_OPERATORS.contains(binaryExprTree.operator())) {
        getBooleanLiteral(binaryExprTree.leftOperand(), binaryExprTree.rightOperand())
          .ifPresent(booleanLiteral -> ctx.reportIssue(booleanLiteral, MESSAGE));
      }
    });

    init.register(UnaryExpressionTree.class, (ctx, unaryExprTree) -> {
      if (UnaryExpressionTree.Operator.NEGATE.equals(unaryExprTree.operator())) {
        getBooleanLiteral(unaryExprTree.operand())
          .ifPresent(booleanLiteral -> ctx.reportIssue(booleanLiteral, MESSAGE));
      }
    });
  }

  private static Optional<Tree> getBooleanLiteral(Tree... trees) {
    return Arrays.stream(trees)
      .map(ExpressionUtils::skipParentheses)
      .filter(ExpressionUtils::isBooleanLiteral)
      .findFirst();
  }
}
