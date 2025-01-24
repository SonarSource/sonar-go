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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.checks.CheckContext;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;

@Rule(key = "S108")
public class EmptyBlockCheck implements GoCheck {

  private static final String MESSAGE = "Either remove or fill this block of code.";

  @Override
  public void initialize(InitContext init) {
    init.register(BlockTree.class, (ctx, blockTree) -> {
      Tree parent = ctx.parent();
      if (isValidBlock(parent) && blockTree.statementOrExpressions().isEmpty()) {
        checkComments(ctx, blockTree);
      }
    });

    init.register(MatchTree.class, (ctx, matchTree) -> {
      if (matchTree.cases().isEmpty()) {
        checkComments(ctx, matchTree);
      }
    });
  }

  private static boolean isValidBlock(@Nullable Tree parent) {
    return !(parent instanceof FunctionDeclarationTree)
      && !(parent instanceof NativeTree)
      && !isWhileLoop(parent);
  }

  private static boolean isWhileLoop(@Nullable Tree parent) {
    return parent instanceof LoopTree loopTree && loopTree.kind() == LoopTree.LoopKind.WHILE;
  }

  private static void checkComments(CheckContext ctx, Tree tree) {
    if (tree.metaData().commentsInside().isEmpty()) {
      ctx.reportIssue(tree, MESSAGE);
    }
  }

}
