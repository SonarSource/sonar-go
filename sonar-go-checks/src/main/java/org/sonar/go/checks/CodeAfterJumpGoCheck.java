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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.go.utils.NativeKinds;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.HasKeyword;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.ReturnTree;
import org.sonar.plugins.go.api.ThrowTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.go.utils.NativeKinds.LABEL;
import static org.sonar.go.utils.NativeKinds.SEMICOLON;

@Rule(key = "S1763")
public class CodeAfterJumpGoCheck implements GoCheck {
  private static final String MESSAGE = "Refactor this piece of code to not have any dead code after this \"%s\".";

  @Override
  public void initialize(InitContext init) {
    init.register(BlockTree.class, (ctx, blockTree) -> checkStatements(ctx, blockTree.statementOrExpressions()));
  }

  private static void checkStatements(CheckContext ctx, List<Tree> statementsOrExpressions) {
    if (statementsOrExpressions.size() < 2) {
      return;
    }

    int index = 0;
    while (index < statementsOrExpressions.size() - 1) {
      Tree current = statementsOrExpressions.get(index);
      index++;

      Tree next = statementsOrExpressions.get(index);
      while (index < statementsOrExpressions.size() && shouldIgnore(next)) {
        next = statementsOrExpressions.get(index);
        index++;
      }

      if (isJump(current) &&
        !shouldIgnore(next) &&
        !isValidAfterJump(next)) {
        ctx.reportIssue(current, String.format(MESSAGE, ((HasKeyword) current).keyword().text()));
      }
    }
  }

  private static boolean isJump(Tree tree) {
    return tree instanceof JumpTree || tree instanceof ReturnTree || tree instanceof ThrowTree;
  }

  private static boolean isValidAfterJump(Tree tree) {
    return NativeKinds.isStringNativeKind(tree, str -> str.contains(LABEL));
  }

  private static boolean shouldIgnore(Tree tree) {
    return NativeKinds.isStringNativeKindOfType(tree, SEMICOLON);
  }
}
