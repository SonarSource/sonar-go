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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.checks.CheckContext;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;
import org.sonar.go.api.checks.SecondaryLocation;
import org.sonar.go.utils.NativeKinds;

import static org.sonar.go.utils.NativeKinds.SEMICOLON;

@Rule(key = "S122")
public class OneStatementPerLineGoCheck implements GoCheck {
  private static final String MESSAGE = "Reformat the code to have only one statement per line.";

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, topLevelTree) -> checkStatements(ctx, topLevelTree.children()));
    init.register(BlockTree.class, (ctx, blockTree) -> checkStatements(ctx, blockTree.statementOrExpressions()));
  }

  private static void checkStatements(CheckContext ctx, List<Tree> statementsOrExpressions) {
    statementsOrExpressions.stream()
      .filter(tree -> !shouldIgnore(tree))
      .collect(Collectors.groupingBy(OneStatementPerLineGoCheck::getLine))
      .forEach((line, statements) -> {
        if (statements.size() > 1) {
          reportIssue(ctx, statements);
        }
      });
  }

  private static void reportIssue(CheckContext ctx, List<Tree> statements) {
    List<SecondaryLocation> secondaryLocations = statements.stream()
      .skip(2)
      .map(statement -> new SecondaryLocation(statement, null))
      .toList();
    ctx.reportIssue(
      statements.get(1),
      MESSAGE,
      secondaryLocations);
  }

  private static int getLine(Tree statementOrExpression) {
    return statementOrExpression.metaData().textRange().start().line();
  }

  private static boolean shouldIgnore(Tree tree) {
    return NativeKinds.isStringNativeKindOfType(tree, SEMICOLON);
  }
}
