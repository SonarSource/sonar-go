/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S5382")
public class DMLStatementInsideLoopCheck implements SlangCheck {

  private static final String MESSAGE = "Move this DML statement out of the loop or process sObjects by batch.";

  @Override
  public void initialize(InitContext init) {
    init.register(LoopTree.class, (ctx, tree) -> findFirstDescendantQuery(tree.body())
      .ifPresent(query -> ctx.reportIssue(query, MESSAGE,
        new SecondaryLocation(tree.keyword().textRange(), "loop"))));
  }

  private static Optional<Tree> findFirstDescendantQuery(Tree tree) {
    if (tree instanceof LoopTree) {
      // intentionally do not recurse into tree.body() to prevent duplicated issues
      return Optional.ofNullable(((LoopTree) tree).condition())
        .map(DMLStatementInsideLoopCheck::findFirstDescendantQuery)
        .orElse(Optional.empty());
    } else if (ExpressionUtils.isDMLQuery(tree)) {
      return Optional.of(tree);
    }
    return tree.children().stream()
      .map(DMLStatementInsideLoopCheck::findFirstDescendantQuery)
      .filter(Optional::isPresent)
      .findFirst()
      .orElse(Optional.empty());
  }

}
