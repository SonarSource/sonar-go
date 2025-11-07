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
import org.sonar.plugins.go.api.ParenthesizedExpressionTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

@Rule(key = "S1110")
public class RedundantParenthesesCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(ParenthesizedExpressionTree.class, (ctx, tree) -> {
      if (ctx.parent() instanceof ParenthesizedExpressionTree) {
        var secondaryLocation = new SecondaryLocation(tree.rightParenthesis().textRange(), null);
        ctx.reportIssue(tree.leftParenthesis(), "Remove these useless parentheses.", secondaryLocation);
      }
    });
  }
}
