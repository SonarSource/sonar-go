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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.go.utils.ExpressionUtils;
import org.sonar.go.utils.SyntacticEquivalence;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.MatchCaseTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

import static org.sonar.go.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1862")
public class IdenticalConditionsCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, tree) -> checkConditions(ctx, collectConditions(tree)));
    init.register(IfTree.class, (ctx, tree) -> {
      if (!(ctx.parent() instanceof IfTree)) {
        checkConditions(ctx, collectConditions(tree, new ArrayList<>()));
      }
    });
  }

  private static List<Tree> collectConditions(MatchTree matchTree) {
    return matchTree.cases().stream()
      .map(MatchCaseTree::expression)
      .filter(Objects::nonNull)
      .map(ExpressionUtils::skipParentheses)
      .toList();
  }

  private static List<Tree> collectConditions(IfTree ifTree, List<Tree> list) {
    list.add(skipParentheses(ifTree.condition()));
    var elseBranch = ifTree.elseBranch();
    if (elseBranch instanceof IfTree elseIfBranch) {
      return collectConditions(elseIfBranch, list);
    }
    return list;
  }

  private static void checkConditions(CheckContext ctx, List<Tree> conditions) {
    for (var group : SyntacticEquivalence.findDuplicatedGroups(conditions)) {
      var original = group.get(0);
      group.stream().skip(1)
        .forEach(duplicated -> {
          var originalRange = original.metaData().textRange();
          ctx.reportIssue(
            duplicated,
            "This condition duplicates the one on line " + originalRange.start().line() + ".",
            new SecondaryLocation(originalRange, "Original"));
        });
    }
  }
}
