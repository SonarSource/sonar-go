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
package org.sonar.go.checks.complexity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.go.impl.JumpTreeImpl;
import org.sonar.go.visitors.TreeContext;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;

import static org.sonar.go.utils.ExpressionUtils.isLogicalBinaryExpression;

public class CognitiveComplexity {

  private List<Increment> increments = new ArrayList<>();

  public CognitiveComplexity(Tree root) {
    CognitiveComplexityVisitor visitor = new CognitiveComplexityVisitor();
    visitor.scan(new TreeContext(), root);
  }

  public int value() {
    int total = 0;
    for (Increment increment : increments) {
      total += increment.nestingLevel + 1;
    }
    return total;
  }

  public List<Increment> increments() {
    return increments;
  }

  public static class Increment {

    private final Token token;
    private final int nestingLevel;

    private Increment(Token token, int nestingLevel) {
      this.token = token;
      this.nestingLevel = nestingLevel;
    }

    public Token token() {
      return token;
    }

    public int nestingLevel() {
      return nestingLevel;
    }
  }

  private class CognitiveComplexityVisitor extends TreeVisitor<TreeContext> {

    private Set<Token> alreadyConsideredOperators = new HashSet<>();

    private CognitiveComplexityVisitor() {

      // TODO "break" or "continue" with label

      register(LoopTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(MatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(CatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(JumpTreeImpl.class, (ctx, tree) -> {
        if (tree.label() != null) {
          incrementWithoutNesting(tree.keyword());
        }
      });

      register(IfTree.class, (ctx, tree) -> {
        Tree parent = ctx.ancestors().peek();
        boolean isElseIf = parent instanceof IfTree ifTree && tree == ifTree.elseBranch();
        if (!isElseIf) {
          incrementWithNesting(tree.ifKeyword(), ctx);
        }
        Token elseKeyword = tree.elseKeyword();
        if (elseKeyword != null) {
          incrementWithoutNesting(elseKeyword);
        }
      });

      register(BinaryExpressionTree.class, (ctx, tree) -> handleBinaryExpressions(tree));
    }

    private void handleBinaryExpressions(BinaryExpressionTree tree) {
      if (!isLogicalBinaryExpression(tree) || alreadyConsideredOperators.contains(tree.operatorToken())) {
        return;
      }

      List<Token> operators = new ArrayList<>();
      flattenOperators(tree, operators);

      Token previous = null;
      for (Token operator : operators) {
        if (previous == null || !previous.text().equals(operator.text())) {
          incrementWithoutNesting(operator);
        }
        previous = operator;
        alreadyConsideredOperators.add(operator);
      }
    }

    private void flattenOperators(BinaryExpressionTree tree, List<Token> operators) {
      if (isLogicalBinaryExpression(tree.leftOperand())) {
        flattenOperators((BinaryExpressionTree) tree.leftOperand(), operators);
      }

      operators.add(tree.operatorToken());

      if (isLogicalBinaryExpression(tree.rightOperand())) {
        flattenOperators((BinaryExpressionTree) tree.rightOperand(), operators);
      }
    }

    private void incrementWithNesting(Token token, TreeContext ctx) {
      increment(token, nestingLevel(ctx));
    }

    private void incrementWithoutNesting(Token token) {
      increment(token, 0);
    }

    private void increment(Token token, int nestingLevel) {
      increments.add(new Increment(token, nestingLevel));
    }

    private int nestingLevel(TreeContext ctx) {
      int nestingLevel = 0;
      boolean isInsideFunction = false;
      Iterator<Tree> ancestors = ctx.ancestors().descendingIterator();
      Tree parent = null;
      while (ancestors.hasNext()) {
        Tree t = ancestors.next();
        if (t instanceof FunctionDeclarationTree) {
          if (isInsideFunction || nestingLevel > 0) {
            nestingLevel++;
          }
          isInsideFunction = true;
        } else if ((t instanceof IfTree && !isElseIfBranch(parent, t)) || t instanceof MatchTree || t instanceof LoopTree || t instanceof CatchTree) {
          nestingLevel++;
        } else if (t instanceof ClassDeclarationTree) {
          nestingLevel = 0;
          isInsideFunction = false;
        }
        parent = t;
      }
      return nestingLevel;
    }

    private boolean isElseIfBranch(@Nullable Tree parent, Tree tree) {
      return parent instanceof IfTree ifTree && ifTree.elseBranch() == tree;
    }

  }

}
