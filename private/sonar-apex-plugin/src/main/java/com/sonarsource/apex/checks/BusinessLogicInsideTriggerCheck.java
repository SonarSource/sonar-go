/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isTrigger;

@Rule(key = "S5384")
public class BusinessLogicInsideTriggerCheck implements SlangCheck {
  @Override
  public void initialize(InitContext init) {
    init.register(NativeTree.class, (ctx, tree) -> {
      if (isTrigger(tree)) {
        tree.descendants()
          .filter(BusinessLogicInsideTriggerCheck::isComplex)
          .findFirst()
          .ifPresent(logic -> ctx.reportIssue(
            tree.children().get(0),
            "Move the business logic to a separate trigger handler class",
            new SecondaryLocation(logic.metaData().tokens().get(0).textRange(), "business logic")));
      }
    });
  }

  private static boolean isComplex(Tree tree) {
    return tree instanceof LoopTree ||
      tree instanceof MatchTree ||
      tree instanceof ExceptionHandlingTree ||
      ExpressionUtils.isQuery(tree);
  }

}
