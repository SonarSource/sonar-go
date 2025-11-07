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

import org.sonar.check.Rule;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;
import static org.sonar.plugins.go.api.AssignmentExpressionTree.Operator.EQUAL;

@Rule(key = "S1656")
public class SelfAssignmentCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() == EQUAL && areEquivalent(tree.leftHandSide(), tree.statementOrExpression())) {
        ctx.reportIssue(tree, "Remove or correct this useless self-assignment.");
      }
    });
  }
}
