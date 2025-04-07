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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.go.utils.TreeUtils;
import org.sonar.go.visitors.TreeContext;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S4144")
public class DuplicatedFunctionImplementationCheck implements GoCheck {

  private static final String MESSAGE = "Update this function so that its implementation is not identical to \"%s\" on line %s.";
  private static final String MESSAGE_NO_NAME = "Update this function so that its implementation is not identical to the one on line %s.";
  private static final int MINIMUM_STATEMENTS_COUNT = 2;

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      Map<Tree, List<FunctionDeclarationTree>> functionsByParents = new HashMap<>();
      TreeVisitor<TreeContext> functionVisitor = new TreeVisitor<>();
      functionVisitor.register(FunctionDeclarationTree.class, (functionCtx, functionDeclarationTree) -> {
        functionsByParents
          .computeIfAbsent(functionCtx.ancestors().peek(), key -> new ArrayList<>())
          .add(functionDeclarationTree);
      });
      functionVisitor.scan(new TreeContext(), tree);

      for (Map.Entry<Tree, List<FunctionDeclarationTree>> entry : functionsByParents.entrySet()) {
        check(ctx, entry.getValue());
      }
    });
  }

  private static void check(CheckContext ctx, List<FunctionDeclarationTree> functionDeclarations) {
    Set<FunctionDeclarationTree> reportedDuplicates = new HashSet<>();
    IntStream.range(0, functionDeclarations.size()).forEach(i -> {
      FunctionDeclarationTree original = functionDeclarations.get(i);
      functionDeclarations.stream()
        .skip(i + 1L)
        .filter(f -> !reportedDuplicates.contains(f))
        .filter(DuplicatedFunctionImplementationCheck::hasMinimumSize)
        .filter(f -> areDuplicatedImplementation(original, f))
        .forEach(duplicate -> {
          reportDuplicate(ctx, original, duplicate);
          reportedDuplicates.add(duplicate);
        });
    });

  }

  private static boolean hasMinimumSize(FunctionDeclarationTree function) {
    BlockTree functionBody = function.body();
    if (functionBody == null) {
      return false;
    }
    return functionBody.statementOrExpressions().stream().filter(TreeUtils.IS_NOT_SEMICOLON).count() >= MINIMUM_STATEMENTS_COUNT;
  }

  private static boolean areDuplicatedImplementation(FunctionDeclarationTree original, FunctionDeclarationTree possibleDuplicate) {
    return areEquivalent(original.receiver(), possibleDuplicate.receiver())
      && areEquivalent(original.formalParameters(), possibleDuplicate.formalParameters())
      && areEquivalent(original.typeParameters(), possibleDuplicate.typeParameters())
      && areEquivalent(original.body(), possibleDuplicate.body());
  }

  private static void reportDuplicate(CheckContext ctx, FunctionDeclarationTree original, FunctionDeclarationTree duplicate) {
    IdentifierTree identifier = original.name();
    int line = original.metaData().textRange().start().line();
    String message;
    Tree secondaryTree;
    if (identifier != null) {
      secondaryTree = identifier;
      message = String.format(MESSAGE, identifier.name(), line);
    } else {
      secondaryTree = original;
      message = String.format(MESSAGE_NO_NAME, line);
    }
    SecondaryLocation secondaryLocation = new SecondaryLocation(secondaryTree, "original implementation");
    IdentifierTree duplicateIdentifier = duplicate.name();
    Tree primaryTree = duplicateIdentifier != null ? duplicateIdentifier : duplicate;
    ctx.reportIssue(primaryTree, message, secondaryLocation);
  }

}
