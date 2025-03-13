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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.UnaryExpressionTree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.UnaryExpressionTreeImpl;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonar.go.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;
import static org.sonar.go.api.BinaryExpressionTree.Operator.EQUAL_TO;
import static org.sonar.go.utils.ExpressionUtils.isBinaryOperation;
import static org.sonar.go.utils.ExpressionUtils.isBooleanLiteral;
import static org.sonar.go.utils.ExpressionUtils.isFalseValueLiteral;
import static org.sonar.go.utils.ExpressionUtils.isLogicalBinaryExpression;
import static org.sonar.go.utils.ExpressionUtils.isNegation;
import static org.sonar.go.utils.ExpressionUtils.isNilLiteral;
import static org.sonar.go.utils.ExpressionUtils.isTrueValueLiteral;
import static org.sonar.go.utils.ExpressionUtils.skipParentheses;

class ExpressionUtilsTest {
  private static final Tree NIL_LITERAL = new LiteralTreeImpl(null, "nil");
  private static final Tree TRUE_LITERAL = new LiteralTreeImpl(null, "true");
  private static final Tree FALSE_LITERAL = new LiteralTreeImpl(null, "false");
  private static final Tree NUMBER_LITERAL = new LiteralTreeImpl(null, "34");
  private static final Tree TRUE_NEGATED = new UnaryExpressionTreeImpl(null, UnaryExpressionTree.Operator.NEGATE, TRUE_LITERAL);
  private static final Tree FALSE_NEGATED = new UnaryExpressionTreeImpl(null, UnaryExpressionTree.Operator.NEGATE, FALSE_LITERAL);

  @Test
  void testNilLiteral() {
    assertThat(isNilLiteral(NIL_LITERAL)).isTrue();
    assertThat(isNilLiteral(TRUE_LITERAL)).isFalse();
    assertThat(isNilLiteral(FALSE_LITERAL)).isFalse();
    assertThat(isNilLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isNilLiteral(TRUE_NEGATED)).isFalse();
  }

  @Test
  void test_boolean_literal() {
    assertThat(isBooleanLiteral(NIL_LITERAL)).isFalse();
    assertThat(isBooleanLiteral(TRUE_LITERAL)).isTrue();
    assertThat(isBooleanLiteral(FALSE_LITERAL)).isTrue();
    assertThat(isBooleanLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isBooleanLiteral(TRUE_NEGATED)).isFalse();
  }

  @Test
  void test_false_literal_value() {
    assertThat(isFalseValueLiteral(NIL_LITERAL)).isFalse();
    assertThat(isFalseValueLiteral(TRUE_LITERAL)).isFalse();
    assertThat(isFalseValueLiteral(FALSE_LITERAL)).isTrue();
    assertThat(isFalseValueLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isFalseValueLiteral(TRUE_NEGATED)).isTrue();
    assertThat(isFalseValueLiteral(FALSE_NEGATED)).isFalse();
  }

  @Test
  void test_true_literal_value() {
    assertThat(isTrueValueLiteral(NIL_LITERAL)).isFalse();
    assertThat(isTrueValueLiteral(TRUE_LITERAL)).isTrue();
    assertThat(isTrueValueLiteral(FALSE_LITERAL)).isFalse();
    assertThat(isTrueValueLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isTrueValueLiteral(TRUE_NEGATED)).isFalse();
    assertThat(isTrueValueLiteral(FALSE_NEGATED)).isTrue();
  }

  @Test
  void test_negation() {
    assertThat(isNegation(NIL_LITERAL)).isFalse();
    assertThat(isNegation(FALSE_LITERAL)).isFalse();
    assertThat(isNegation(NUMBER_LITERAL)).isFalse();
    assertThat(isNegation(TRUE_NEGATED)).isTrue();
  }

  @Test
  void test_binary_operation() {
    Tree binaryAnd = new BinaryExpressionTreeImpl(null, CONDITIONAL_AND, null, TRUE_LITERAL, FALSE_LITERAL);

    assertThat(isBinaryOperation(binaryAnd, CONDITIONAL_AND)).isTrue();
    assertThat(isBinaryOperation(binaryAnd, CONDITIONAL_OR)).isFalse();
  }

  @Test
  void test_logical_binary_operation() {
    Tree binaryAnd = new BinaryExpressionTreeImpl(null, CONDITIONAL_AND, null, TRUE_LITERAL, FALSE_LITERAL);
    Tree binaryOr = new BinaryExpressionTreeImpl(null, CONDITIONAL_OR, null, TRUE_LITERAL, FALSE_LITERAL);
    Tree binaryEqual = new BinaryExpressionTreeImpl(null, EQUAL_TO, null, TRUE_LITERAL, FALSE_LITERAL);

    assertThat(isLogicalBinaryExpression(binaryAnd)).isTrue();
    assertThat(isLogicalBinaryExpression(binaryOr)).isTrue();
    assertThat(isLogicalBinaryExpression(binaryEqual)).isFalse();
    assertThat(isLogicalBinaryExpression(TRUE_NEGATED)).isFalse();
  }

  @Test
  void test_skip_parentheses() {
    Tree parenthesizedExpression1 = new ParenthesizedExpressionTreeImpl(null, TRUE_LITERAL, null, null);
    Tree parenthesizedExpression2 = new ParenthesizedExpressionTreeImpl(null, parenthesizedExpression1, null, null);

    assertThat(skipParentheses(parenthesizedExpression1)).isEqualTo(TRUE_LITERAL);
    assertThat(skipParentheses(parenthesizedExpression2)).isEqualTo(TRUE_LITERAL);
    assertThat(skipParentheses(TRUE_LITERAL)).isEqualTo(TRUE_LITERAL);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    x int, int
    c Ctx, Ctx
    c *Ctx, Ctx
    c gin.Context, gin.Context
    c *gin.Context, gin.Context
    """)
  void shouldExtractTypeOfParameter(String parameter, String expectedType) {
    var code = """
      package test

      func main(%s) {
      }
      """.formatted(parameter);
    var parameterTree = TestGoConverter.parse(code)
      .descendants()
      .filter(ParameterTree.class::isInstance)
      .map(ParameterTree.class::cast)
      .findFirst()
      .get();

    var type = ExpressionUtils.getTypeOf(parameterTree);

    assertThat(type).isEqualTo(expectedType);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    http.Cookie{},true
    &http.Cookie{},true
    new(http.Cookie),true
    NewCookie(),false
    Cookie{},false
    &Cookie{},false
    new(Cookie),false
    """)
  void shouldExtractTypeOfInitializer(String code, boolean shouldBePresent) {
    code = """
      package test

      func main() {
        _ := %s
      }
      """.formatted(code);

    var type = TestGoConverter.parse(code)
      .descendants()
      .filter(VariableDeclarationTree.class::isInstance)
      .map(VariableDeclarationTree.class::cast)
      .map(VariableDeclarationTree::initializers)
      .flatMap(List::stream)
      .findFirst()
      .flatMap(ExpressionUtils::getTypeOfStructOrPointerInitializer);

    if (shouldBePresent) {
      assertThat(type).get()
        .returns("http", from(it -> ((IdentifierTree) it.expression()).name()))
        .returns("Cookie", from(it -> it.identifier().name()));
    } else {
      assertThat(type).isEmpty();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "[]byte(\"my string\")",
    "[]byte(\"my string\", \"unexpected second arg\")",
  })
  void shouldReturnByteArrayCallArg(String call) {
    var byteArrayCall = parseExpression(call);
    var arg = ExpressionUtils.retrieveByteArrayCallArg(byteArrayCall).orElse(null);
    assertThat(arg)
      .isNotNull()
      .isInstanceOfSatisfying(StringLiteralTree.class, stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("my string"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "[]byte()",
    "byte(\"some string\")",
    "[]val.byte(\"some string\")",
    "[]int(5)",
    "\"some string\""
  })
  void shouldNotReturnByteArrayCallArg(String call) {
    var byteArrayCall = parseExpression(call);
    var arg = ExpressionUtils.retrieveByteArrayCallArg(byteArrayCall).orElse(null);
    assertThat(arg).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "make([]byte, 16)",
    "make([]byte, 16, 32)",
  })
  void shouldReturnByteArrayMakeSize(String call) {
    var byteArrayCall = parseExpression(call);
    var arg = ExpressionUtils.retrieveByteArrayMakeSizeTree(byteArrayCall).orElse(null);
    assertThat(arg)
      .isNotNull()
      .isInstanceOfSatisfying(IntegerLiteralTree.class, integerLiteralTree -> assertThat(integerLiteralTree.getIntegerValue()).isEqualTo(16));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "make([]int, 16)",
    "val.make([]byte, 16)",
    "made([]byte, 16)",
    "make([]byte)",
    "make()",
    "\"some string\""
  })
  void shouldNotReturnByteArrayMakeSize(String call) {
    var byteArrayCall = parseExpression(call);
    var arg = ExpressionUtils.retrieveByteArrayMakeSizeTree(byteArrayCall).orElse(null);
    assertThat(arg).isNull();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    (*T)(x), true
    (T)(x), false
    T(x), false
    *T(x), false
    x, false
    (x), false
    """)
  void shouldDetectPointerTypeCasts(String code, boolean shouldMatch) {
    var tree = parseExpression(code);

    assertThat(ExpressionUtils.isPointerTypeCast(tree, type -> true)).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    (*T)(x), true
    (*T1)(x), false
    (*R)(x), false
    (*int)(x), false
    """)
  void shouldFilterOutPointerTypeCastsByPredicate(String code, boolean shouldMatch) {
    var tree = parseExpression(code);

    assertThat(ExpressionUtils.isPointerTypeCast(tree, type -> ((IdentifierTree) type).name().equals("T"))).isEqualTo(shouldMatch);
  }

  private Tree parseExpression(String expression) {
    var code = """
      package test

      func main() {
        _ := %s
      }
      """.formatted(expression);

    var variableDeclaration = TestGoConverter.parse(code)
      .descendants()
      .filter(VariableDeclarationTree.class::isInstance)
      .map(VariableDeclarationTree.class::cast)
      .findFirst().get();
    return variableDeclaration.initializers().get(0);
  }

}
