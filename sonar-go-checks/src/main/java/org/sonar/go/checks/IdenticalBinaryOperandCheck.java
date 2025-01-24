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

import org.sonar.check.Rule;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;
import org.sonar.go.api.checks.SecondaryLocation;

import static org.sonar.go.checks.utils.ExpressionUtils.containsPlaceHolder;
import static org.sonar.go.checks.utils.ExpressionUtils.skipParentheses;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1764")
public class IdenticalBinaryOperandCheck implements GoCheck {

  public static final String MESSAGE = "Correct one of the identical sub-expressions on both sides this operator";

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() != BinaryExpressionTree.Operator.PLUS
        && tree.operator() != BinaryExpressionTree.Operator.TIMES
        && !containsPlaceHolder(tree)
        && areEquivalent(skipParentheses(tree.leftOperand()), skipParentheses(tree.rightOperand()))) {
        ctx.reportIssue(tree.rightOperand(), MESSAGE, new SecondaryLocation(tree.leftOperand()));
      }
    });
  }

}
