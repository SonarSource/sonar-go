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
package org.sonar.go.utils;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.go.api.ArrayTypeTree;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.ExceptionHandlingTree;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IfTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchCaseTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.ParenthesizedExpressionTree;
import org.sonar.go.api.PlaceHolderTree;
import org.sonar.go.api.StarExpressionTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.UnaryExpressionTree;

import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;
import static org.sonar.go.utils.TreeUtils.getIdentifierName;

public class ExpressionUtils {
  private static final String NIL_LITERAL = "nil";
  private static final String TRUE_LITERAL = "true";
  private static final String FALSE_LITERAL = "false";
  private static final List<String> BOOLEAN_LITERALS = Arrays.asList(TRUE_LITERAL, FALSE_LITERAL);

  private ExpressionUtils() {
  }

  public static boolean isNilLiteral(@Nullable Tree tree) {
    return tree instanceof LiteralTree literalTree && NIL_LITERAL.equals(literalTree.value());
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

  public static Tree skipParentheses(@Nullable Tree tree) {
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

  public static boolean isOfType(MemberSelectTree memberSelectTree, String expectedPackageName, String expectedIdentifierName) {
    return memberSelectTree.expression() instanceof IdentifierTree firstIdentifier && expectedPackageName.equals(firstIdentifier.packageName())
      && expectedIdentifierName.equals(memberSelectTree.identifier().name());
  }

  public static boolean isIdentifier(Tree tree, String name) {
    return tree instanceof IdentifierTree identifierTree && name.equals(identifierTree.name());
  }

  public static String getTypeOf(ParameterTree parameter) {
    var type = parameter.type();
    if (type == null) {
      return "";
    }
    if (type instanceof StarExpressionTree starExpressionTree) {
      type = starExpressionTree.operand();
    }
    return Optional.of(type)
      .filter(IdentifierTree.class::isInstance)
      .map(it -> ((IdentifierTree) it).name())
      .orElse(getIdentifierName(type.children()));
  }

  /**
   * Retrieves the type of an expression that creates a struct or a pointer. There are several possible cases:
   * <ul>
   *   <li>{@code CompositeLit{}}</li>
   *   <li>{@code &CompositeLit{}}</li>
   *   <li>{@code new(Type)}</li>
   * </ul>
   *
   * @param initializer a RHS of an assignment expression
   * @return a base type of the expression ({@code &} is removed if present)
   */
  public static Optional<String> getTypeOfStructOrPointerInitializer(@Nullable Tree initializer) {
    if (initializer == null) {
      return Optional.empty();
    }
    if (initializer instanceof FunctionInvocationTree invocation
      && getMemberSelectOrIdentifierName(invocation.memberSelect()).filter("new"::equals).isPresent()) {
      return getTypeOfNewExpression(invocation);
    }
    if (getUnaryOperandOrTree(initializer) instanceof CompositeLiteralTree compositeLiteralTree) {
      return getTypeOfMemberSelectOrIdentifier(compositeLiteralTree.type());
    }

    return Optional.empty();
  }

  public static Tree getUnaryOperandOrTree(Tree tree) {
    if (tree instanceof UnaryExpressionTree unaryExpression
      && unaryExpression.operator() == UnaryExpressionTree.Operator.ADDRESS_OF) {
      return unaryExpression.operand();
    }
    return tree;
  }

  private static Optional<String> getTypeOfNewExpression(FunctionInvocationTree newInvocation) {
    List<Tree> arguments = newInvocation.arguments();
    if (arguments.isEmpty()) {
      return Optional.empty();
    }
    return getTypeOfMemberSelectOrIdentifier(arguments.get(0));
  }

  private static Optional<String> getTypeOfMemberSelectOrIdentifier(@Nullable Tree tree) {
    if (tree instanceof MemberSelectTree memberSelectTree) {
      return Optional.ofNullable(memberSelectTree.identifier().type());
    } else if (tree instanceof IdentifierTree identifierTree) {
      return Optional.ofNullable(identifierTree.type());
    }
    return Optional.empty();
  }

  /**
   * Ensure the provided tree is a function invocation with a byte array as member select, and if so return the argument tree.
   * <pre>
   * {@code
   * []byte("salt")
   *        ^^^^^^
   * }
   * </pre>
   */
  public static Optional<Tree> retrieveByteArrayCallArg(@Nullable Tree tree) {
    if (tree instanceof FunctionInvocationTree functionInvocation && !functionInvocation.arguments().isEmpty()
      && isByteArray(functionInvocation.memberSelect())) {
      return Optional.of(functionInvocation.arguments().get(0));
    }
    return Optional.empty();
  }

  public static boolean isByteArray(Tree tree) {
    return tree instanceof ArrayTypeTree arrayType && arrayType.element() instanceof IdentifierTree identifier && "byte".equals(identifier.name());
  }

  /**
   * Detects casts to a pointer type: (*T)(x).
   * According to <a href="https://tip.golang.org/ref/spec#Conversions">golang specification</a>
   * , to avoid ambiguity, type will be parenthesized
   * in case of pointer type cast.
   */
  public static boolean isPointerTypeCast(Tree tree, Predicate<Tree> typePredicate) {
    return tree instanceof FunctionInvocationTree functionInvocation &&
      functionInvocation.memberSelect() instanceof ParenthesizedExpressionTree parenthesizedExpression &&
      parenthesizedExpression.expression() instanceof StarExpressionTree starExpressionTree &&
      typePredicate.test(starExpressionTree.operand()) &&
      functionInvocation.arguments().size() == 1;
  }

  /**
   * Ensure the provided tree is a function invocation to 'make' with first parameter as a byte array and return the second parameter.
   * <pre>
   * {@code
   * make([]byte, 16)
   *              ^^
   * }
   * </pre>
   */
  public static Optional<Tree> retrieveByteArrayMakeSizeTree(@Nullable Tree tree) {
    if (tree instanceof FunctionInvocationTree functionInvocation && functionInvocation.arguments().size() >= 2
      && functionInvocation.memberSelect() instanceof IdentifierTree functionName && "make".equals(functionName.name())
      && isByteArray(functionInvocation.arguments().get(0))) {
      return Optional.of(functionInvocation.arguments().get(1));
    }
    return Optional.empty();
  }
}
