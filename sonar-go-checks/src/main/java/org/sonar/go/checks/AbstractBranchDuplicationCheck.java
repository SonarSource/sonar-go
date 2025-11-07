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

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.MatchCaseTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

public abstract class AbstractBranchDuplicationCheck implements GoCheck {

  protected abstract void checkDuplicatedBranches(CheckContext ctx, Tree tree, List<Tree> branches);

  protected abstract void onAllIdenticalBranches(CheckContext ctx, Tree tree);

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, tree) -> {
      Tree parent = ctx.parent();
      if (!(parent instanceof IfTree ifTree) || tree == ifTree.thenBranch()) {
        checkConditionalStructure(ctx, tree, new ConditionalStructure(tree));
      }
    });
    init.register(MatchTree.class, (ctx, tree) -> checkConditionalStructure(ctx, tree, new ConditionalStructure(tree)));
  }

  protected void checkConditionalStructure(CheckContext ctx, Tree tree, ConditionalStructure conditional) {
    if (conditional.allBranchesArePresent && conditional.allBranchesAreIdentical()) {
      onAllIdenticalBranches(ctx, tree);
    } else {
      checkDuplicatedBranches(ctx, tree, conditional.branches);
    }
  }

  public static class ConditionalStructure {

    private boolean allBranchesArePresent = false;

    private final List<Tree> branches = new ArrayList<>();

    private ConditionalStructure(IfTree ifTree) {
      branches.add(ifTree.thenBranch());
      Tree elseBranch = ifTree.elseBranch();
      while (elseBranch != null) {
        if (elseBranch instanceof IfTree elseIf) {
          branches.add(elseIf.thenBranch());
          elseBranch = elseIf.elseBranch();
        } else {
          branches.add(elseBranch);
          allBranchesArePresent = true;
          elseBranch = null;
        }
      }
    }

    private ConditionalStructure(MatchTree tree) {
      for (MatchCaseTree caseTree : tree.cases()) {
        branches.add(caseTree.body());
        if (caseTree.expression() == null) {
          allBranchesArePresent = true;
        }
      }
    }

    private boolean allBranchesAreIdentical() {
      return branches.size() > 1 &&
        branches.stream()
          .skip(1)
          .allMatch(branch -> areEquivalent(branches.get(0), branch));
    }
  }

}
