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
package org.sonar.go.checks.utils;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.ExceptionHandlingTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IfTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchCaseTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.ParenthesizedExpressionTree;
import org.sonar.go.api.PlaceHolderTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.UnaryExpressionTree;

import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;

public class ExpressionUtils {
  private static final String TRUE_LITERAL = "true";
  private static final String FALSE_LITERAL = "false";
  private static final List<String> BOOLEAN_LITERALS = Arrays.asList(TRUE_LITERAL, FALSE_LITERAL);

  private ExpressionUtils() {
  }

  public static boolean isBooleanLiteral(Tree tree) {
    return tree instanceof LiteralTree literalTree && BOOLEAN_LITERALS.contains(literalTree.value());
  }

  public static boolean isFalseValueLiteral(Tree originalTree) {
    Tree tree = skipParentheses(originalTree);
    return (tree instanceof LiteralTree literalTree && FALSE_LITERAL.equals(literalTree.value()))
      || (isNegation(tree) && isTrueValueLiteral(((UnaryExpressionTree) tree).operand()));
  }

  public static boolean isTrueValueLiteral(Tree originalTree) {
    Tree tree = skipParentheses(originalTree);
    return (tree instanceof LiteralTree literalTree && TRUE_LITERAL.equals(literalTree.value()))
      || (isNegation(tree) && isFalseValueLiteral(((UnaryExpressionTree) tree).operand()));
  }

  public static boolean isNegation(Tree tree) {
    return tree instanceof UnaryExpressionTree unary && unary.operator() == UnaryExpressionTree.Operator.NEGATE;
  }

  public static boolean isBinaryOperation(Tree tree, BinaryExpressionTree.Operator operator) {
    return tree instanceof BinaryExpressionTree binaryExpressionTree && binaryExpressionTree.operator() == operator;
  }

  public static boolean isLogicalBinaryExpression(Tree tree) {
    return isBinaryOperation(tree, CONDITIONAL_AND) || isBinaryOperation(tree, CONDITIONAL_OR);
  }

  public static Tree skipParentheses(Tree tree) {
    Tree result = tree;
    while (result instanceof ParenthesizedExpressionTree parenthesizedExpressionTree) {
      result = parenthesizedExpressionTree.expression();
    }
    return result;
  }

  public static boolean containsPlaceHolder(Tree tree) {
    return tree.descendants().anyMatch(PlaceHolderTree.class::isInstance);
  }

  public static boolean isTernaryOperator(Deque<Tree> ancestors, Tree tree) {
    if (!isIfWithElse(tree)) {
      return false;
    }
    Tree child = tree;
    for (Tree ancestor : ancestors) {
      if (ancestor instanceof BlockTree || ancestor instanceof ExceptionHandlingTree || ancestor instanceof TopLevelTree ||
        isBranchOfLoopOrCaseOrIfWithoutElse(ancestor, child)) {
        break;
      }
      if (!isBranchOfIf(ancestor, child)) {
        return tree.descendants().noneMatch(BlockTree.class::isInstance);
      }
      child = ancestor;
    }
    return false;
  }

  private static boolean isIfWithElse(Tree tree) {
    return tree instanceof IfTree ifTree && ifTree.elseBranch() != null;
  }

  private static boolean isBranchOfLoopOrCaseOrIfWithoutElse(Tree parent, Tree child) {
    return (parent instanceof LoopTree loopTree && child == loopTree.body()) ||
      (parent instanceof MatchCaseTree matchCaseTree && child == matchCaseTree.body()) ||
      (isBranchOfIf(parent, child) && ((IfTree) parent).elseBranch() == null);
  }

  private static boolean isBranchOfIf(Tree parent, Tree child) {
    if (parent instanceof IfTree ifTree) {
      return child == ifTree.thenBranch() || child == ifTree.elseBranch();
    }
    return false;
  }

  public static Optional<String> getMemberSelectOrIdentifierName(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return Optional.of(identifierTree.name());
    } else if (tree instanceof MemberSelectTree memberSelectTree) {
      return Optional.of(memberSelectTree.identifier().name());
    } else {
      return Optional.empty();
    }
  }

}
