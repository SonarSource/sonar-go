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
import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.go.checks.utils.TreeUtils;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextRangeImpl;

@Rule(key = "S126")
public class ElseIfWithoutElseCheck implements SlangCheck {

  private static final String MESSAGE = "Add the missing \"else\" clause.";
  private static final Predicate<Tree> IS_IDENTIFIER_PANIC = tree -> {
    if (tree instanceof IdentifierTree identifierTree) {
      return identifierTree.name().equals("panic");
    }
    return false;
  };

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> {
      if (ifTree.elseBranch() == null || !isTopLevelIf(ctx, ifTree)) {
        return;
      }

      IfTree prevTree = ifTree;
      boolean endsWithReturn = endsWithReturnBreakOrPanic(ifTree);
      while (ifTree.elseBranch() instanceof IfTree) {
        prevTree = ifTree;
        ifTree = (IfTree) (ifTree.elseBranch());
        endsWithReturn = endsWithReturn && endsWithReturnBreakOrPanic(ifTree);
      }

      // We raise an issue if
      // - at least one branch does not finish with return/break/throw
      // - no "else" is defined
      if (!endsWithReturn && ifTree.elseBranch() == null) {
        Token elseToken = prevTree.elseKeyword();
        Token ifToken = ifTree.ifKeyword();
        TextRange textRange = new TextRangeImpl(
          elseToken.textRange().start(),
          ifToken.textRange().end());
        ctx.reportIssue(textRange, MESSAGE);
      }

    });
  }

  private static boolean isTopLevelIf(CheckContext ctx, IfTree ifTree) {
    Tree firstAncestor = ctx.ancestors().getFirst();
    if (firstAncestor instanceof IfTree ifTreeAncestor) {
      // if ifTree is different from the else branch of firstAncestor, it means that ifTree is a statement inside
      // firstAncestor and so ifTree is the top level "if"
      return ifTreeAncestor.elseBranch() != ifTree;
    }
    return true;
  }

  private static boolean endsWithReturnBreakOrPanic(IfTree ifTree) {
    Tree thenBranch = ifTree.thenBranch();
    if (thenBranch instanceof BlockTree blockTree) {
      List<Tree> statements = blockTree.statementOrExpressions()
        .stream().filter(TreeUtils.IS_NOT_SEMICOLON)
        .toList();
      if (!statements.isEmpty()) {
        Tree lastStmt = statements.get(statements.size() - 1);
        return isReturnBreakOrPanic(lastStmt);
      }
    }
    // Curly braces can be omitted when there is only one statement inside the "if"
    return isReturnBreakOrPanic(thenBranch);
  }

  private static boolean isReturnBreakOrPanic(Tree tree) {
    return tree instanceof JumpTree || tree instanceof ReturnTree || isPanicCall(tree);
  }

  private static boolean isPanicCall(Tree tree) {
    return Optional.of(tree)
      .map(ElseIfWithoutElseCheck::toNativeTreeChildren)
      .filter(children -> children.size() == 1)
      .map(children -> children.get(0))
      .map(ElseIfWithoutElseCheck::toNativeTreeChildren)
      .filter(children -> !children.isEmpty())
      .map(children -> children.get(0))
      .filter(IS_IDENTIFIER_PANIC)
      .isPresent();
  }

  private static List<Tree> toNativeTreeChildren(Tree optTree) {
    return Optional.of(optTree)
      .filter(NativeTree.class::isInstance)
      .map(NativeTree.class::cast)
      .map(NativeTree::children)
      .orElse(List.of());
  }
}
