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
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S1186")
public class EmptyFunctionCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      BlockTree body = tree.body();
      if (body != null && body.statementOrExpressions().isEmpty() && !hasComment(body, ctx.parent().metaData())) {
        ctx.reportIssue(body, "Add a nested comment explaining why this function is empty or complete the implementation.");
      }
    });
  }

  private static boolean hasComment(BlockTree body, TreeMetaData parentMetaData) {
    if (!body.metaData().commentsInside().isEmpty()) {
      return true;
    }

    int emptyBodyEndLine = body.textRange().end().line();
    return parentMetaData.commentsInside().stream()
      .anyMatch(comment -> comment.contentRange().start().line() == emptyBodyEndLine);
  }

}
