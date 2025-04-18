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

import java.util.EnumSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

import static org.sonar.go.utils.ExpressionUtils.containsPlaceHolder;
import static org.sonar.go.utils.ExpressionUtils.skipParentheses;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;
import static org.sonar.plugins.go.api.BinaryExpressionTree.*;

@Rule(key = "S1764")
public class IdenticalBinaryOperandCheck implements GoCheck {

  public static final String MESSAGE = "Correct one of the identical sub-expressions on both sides of this operator.";
  private static final Set<Operator> EXCEPTIONS = EnumSet.of(Operator.PLUS, Operator.TIMES, Operator.BITWISE_SHL);

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (!EXCEPTIONS.contains(tree.operator())
        && !containsPlaceHolder(tree)
        && areEquivalent(skipParentheses(tree.leftOperand()), skipParentheses(tree.rightOperand()))) {
        ctx.reportIssue(tree.rightOperand(), MESSAGE, new SecondaryLocation(tree.leftOperand()));
      }
    });
  }

}
