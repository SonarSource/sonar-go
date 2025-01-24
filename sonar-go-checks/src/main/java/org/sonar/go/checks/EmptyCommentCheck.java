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
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;

@Rule(key = "S4663")
public class EmptyCommentCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> tree.allComments().stream()
      .filter(comment -> comment.contentText().trim().isEmpty() && !comment.contentRange().end().equals(comment.textRange().end()))
      .forEach(comment -> ctx.reportIssue(comment, "Remove this comment, it is empty.")));
  }

}
