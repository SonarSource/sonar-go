/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.ExceptionHandlingTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

@Rule(key = "S134")
public class TooDeeplyNestedStatementsCheck implements GoCheck {
  private static final int DEFAULT_MAX_DEPTH = 4;
  private static final String DEFAULT_MAX_DEPTH_VALUE = "" + DEFAULT_MAX_DEPTH;

  @RuleProperty(
    key = "max",
    description = "Maximum allowed control flow statement nesting depth",
    defaultValue = DEFAULT_MAX_DEPTH_VALUE)
  public int max = DEFAULT_MAX_DEPTH;

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, this::checkNestedDepth);
    init.register(LoopTree.class, this::checkNestedDepth);
    init.register(MatchTree.class, this::checkNestedDepth);
    init.register(ExceptionHandlingTree.class, this::checkNestedDepth);
  }

  private void checkNestedDepth(CheckContext ctx, Tree tree) {
    if (isElseIfStatement(ctx.parent(), tree)) {
      // Ignore 'else-if' statements since the issue would already be raised on the first 'if' statement
      return;
    }

    var iterator = ctx.ancestors().iterator();
    var nestedParentNodes = new LinkedList<Token>();
    var last = tree;

    while (iterator.hasNext()) {
      var parent = iterator.next();
      if (isElseIfStatement(parent, last) && !nestedParentNodes.isEmpty()) {
        // Only the 'if' parent of the chained 'else-if' statements should be highlighted
        nestedParentNodes.removeLast();
      }
      if (parent instanceof LoopTree || parent instanceof ExceptionHandlingTree || parent instanceof IfTree || parent instanceof MatchTree) {
        nestedParentNodes.addLast(getNodeToHighlight(parent));
      }
      if (nestedParentNodes.size() > max) {
        return;
      }
      last = parent;
    }

    if (nestedParentNodes.size() == max) {
      reportIssue(ctx, tree, nestedParentNodes);
    }
  }

  private static boolean isElseIfStatement(@Nullable Tree parent, @Nullable Tree tree) {
    return tree instanceof IfTree && parent instanceof IfTree && tree.equals(((IfTree) parent).elseBranch());
  }

  private void reportIssue(CheckContext ctx, Tree statement, Deque<Token> nestedStatements) {
    var message = "Refactor this code to not nest more than %s control flow statements.".formatted(max);
    var secondaryLocations = new ArrayList<SecondaryLocation>(nestedStatements.size());
    int nestedDepth = 0;

    while (!nestedStatements.isEmpty()) {
      nestedDepth++;
      var secondaryLocationMessage = String.format("Nesting depth %s", nestedDepth);
      secondaryLocations.add(new SecondaryLocation(nestedStatements.removeLast().textRange(), secondaryLocationMessage));
    }

    var nodeToHighlight = getNodeToHighlight(statement);
    ctx.reportIssue(nodeToHighlight, message, secondaryLocations);
  }

  private static Token getNodeToHighlight(Tree tree) {
    if (tree instanceof IfTree ifTree) {
      return ifTree.ifKeyword();
    } else if (tree instanceof MatchTree matchTree) {
      return matchTree.keyword();
    } else {
      return ((LoopTree) tree).keyword();
    }
  }
}
