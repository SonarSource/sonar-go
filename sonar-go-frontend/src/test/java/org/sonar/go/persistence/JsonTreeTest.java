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
package org.sonar.go.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.ArrayTypeTreeImpl;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.CatchTreeImpl;
import org.sonar.go.impl.ClassDeclarationTreeImpl;
import org.sonar.go.impl.CompositeLiteralTreeImpl;
import org.sonar.go.impl.EllipsisTreeImpl;
import org.sonar.go.impl.ExceptionHandlingTreeImpl;
import org.sonar.go.impl.FloatLiteralTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IfTreeImpl;
import org.sonar.go.impl.ImaginaryLiteralTreeImpl;
import org.sonar.go.impl.ImportDeclarationTreeImpl;
import org.sonar.go.impl.ImportSpecificationTreeImpl;
import org.sonar.go.impl.IndexExpressionTreeImpl;
import org.sonar.go.impl.IndexListExpressionTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.JumpTreeImpl;
import org.sonar.go.impl.KeyValueTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
import org.sonar.go.impl.MapTypeTreeImpl;
import org.sonar.go.impl.MatchCaseTreeImpl;
import org.sonar.go.impl.MatchTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.impl.ModifierTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PackageDeclarationTreeImpl;
import org.sonar.go.impl.ParameterTreeImpl;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.ReturnTreeImpl;
import org.sonar.go.impl.SliceTreeImpl;
import org.sonar.go.impl.StarExpressionTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.ThrowTreeImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.TypeAssertionExpressionTreeImpl;
import org.sonar.go.impl.TypeImpl;
import org.sonar.go.impl.UnaryExpressionTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.ArrayTypeTree;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.CompositeLiteralTree;
import org.sonar.plugins.go.api.EllipsisTree;
import org.sonar.plugins.go.api.ExceptionHandlingTree;
import org.sonar.plugins.go.api.FloatLiteralTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.ImaginaryLiteralTree;
import org.sonar.plugins.go.api.ImportDeclarationTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.IndexExpressionTree;
import org.sonar.plugins.go.api.IndexListExpressionTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MapTypeTree;
import org.sonar.plugins.go.api.MatchCaseTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.ModifierTree;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.PackageDeclarationTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.ParenthesizedExpressionTree;
import org.sonar.plugins.go.api.PlaceHolderTree;
import org.sonar.plugins.go.api.ReturnTree;
import org.sonar.plugins.go.api.SliceTree;
import org.sonar.plugins.go.api.StarExpressionTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.ThrowTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.TreeOrError;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.VariableDeclarationTree;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.BODY;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CASES;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CATCH_BLOCK;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CATCH_BLOCKS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CATCH_PARAMETER;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CLASS_TREE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CONDITION;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CONTENT;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.DECLARATIONS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELEMENT;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELLIPSIS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELSE_BRANCH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELSE_KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.EXPRESSION;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.EXPRESSIONS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FINALLY_BLOCK;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FIRST_CPD_TOKEN;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FORMAL_PARAMETERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.HIGH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IDENTIFIER;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IDENTIFIERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IF_KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.INDEX;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.INDICES;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.INITIALIZERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IS_VAL;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.KEY;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.KIND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LABEL;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_HAND_SIDE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_OPERAND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_PARENTHESIS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LENGTH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LOW;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.MAX;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.NAME;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.NATIVE_KIND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.OPERAND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.OPERATOR;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.OPERATOR_TOKEN;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.PATH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.PLACE_HOLDER_TOKEN;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RECEIVER;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RETURN_TYPE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RIGHT_OPERAND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RIGHT_PARENTHESIS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.SLICE_3;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.STATEMENT_OR_EXPRESSION;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.STATEMENT_OR_EXPRESSIONS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.THEN_BRANCH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TRY_BLOCK;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TRY_KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TYPE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TYPE_PARAMETERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.VALUE;

class JsonTreeTest extends JsonTestHelper {

  @Test
  void array_type() throws IOException {
    Token tokenBracketOpen = otherToken(1, 0, "[");
    Token tokenIndex = otherToken(1, 1, "1");
    otherToken(1, 2, "]");
    Token tokenType = otherToken(1, 3, "byte");

    Tree integerLiteral = TreeCreationUtils.integerLiteral(metaData(tokenIndex), tokenIndex.text());
    Tree identifier = TreeCreationUtils.identifier(metaData(tokenType), tokenType.text());

    TreeMetaData metaData = metaData(tokenBracketOpen, tokenType);
    ArrayTypeTree arrayTypeTree = new ArrayTypeTreeImpl(metaData, integerLiteral, identifier);

    ArrayTypeTree expression = checkJsonSerializationDeserialization(arrayTypeTree, "array_type.json");
    assertThat(expression.length().textRange()).isEqualTo(tokenIndex.textRange());
    assertThat(expression.element().textRange()).isEqualTo(tokenType.textRange());

    assertThat(methodNames(ArrayTypeTree.class))
      .containsExactlyInAnyOrder(ELEMENT, LENGTH, TYPE);
  }

  @Test
  void assignment_expression() throws IOException {
    Token tokenIdentifier = otherToken(1, 0, "x");
    otherToken(1, 2, "=");
    Token tokenLiteral = otherToken(1, 4, "2");

    Tree identifier = TreeCreationUtils.identifier(metaData(tokenIdentifier), tokenIdentifier.text());
    AssignmentExpressionTree.Operator operator = AssignmentExpressionTree.Operator.EQUAL;
    Tree literal = new IntegerLiteralTreeImpl(metaData(tokenLiteral), tokenLiteral.text());

    TreeMetaData metaData = metaData(tokenIdentifier, tokenLiteral);
    AssignmentExpressionTree initialAssignment = new AssignmentExpressionTreeImpl(metaData, operator, identifier, literal);
    AssignmentExpressionTree assignment = checkJsonSerializationDeserialization(initialAssignment, "assignment_expression.json");
    assertThat(assignment.leftHandSide().textRange()).isEqualTo(tokenIdentifier.textRange());
    assertThat(assignment.operator()).isEqualTo(AssignmentExpressionTree.Operator.EQUAL);
    assertThat(assignment.statementOrExpression().textRange()).isEqualTo(tokenLiteral.textRange());

    assertThat(methodNames(AssignmentExpressionTree.class))
      .containsExactlyInAnyOrder(OPERATOR, LEFT_HAND_SIDE, STATEMENT_OR_EXPRESSION);
  }

  @Test
  void binary_expression() throws IOException {
    Token tokenX = otherToken(1, 0, "x");
    Token tokenLess = otherToken(1, 2, "<");
    Token tokenY = otherToken(1, 4, "y");

    Tree identifierX = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    Tree identifierY = TreeCreationUtils.identifier(metaData(tokenY), tokenY.text());
    TreeMetaData metaData = metaData(tokenX, tokenY);
    BinaryExpressionTree initialExpression = new BinaryExpressionTreeImpl(metaData, BinaryExpressionTree.Operator.LESS_THAN, tokenLess,
      identifierX, identifierY);
    BinaryExpressionTree expression = checkJsonSerializationDeserialization(initialExpression, "binary_expression.json");
    assertThat(expression.leftOperand().textRange()).isEqualTo(tokenX.textRange());
    assertThat(expression.operator()).isEqualTo(BinaryExpressionTree.Operator.LESS_THAN);
    assertThat(expression.operatorToken().text()).isEqualTo("<");
    assertThat(expression.rightOperand().textRange()).isEqualTo(tokenY.textRange());

    assertThat(methodNames(BinaryExpressionTree.class))
      .containsExactlyInAnyOrder(OPERATOR, OPERATOR_TOKEN, LEFT_OPERAND, RIGHT_OPERAND);
  }

  @Test
  void block() throws IOException {
    Token tokenOpen = otherToken(1, 0, "{");
    Token tokenX = otherToken(1, 2, "x");
    Token tokenClose = otherToken(1, 4, "}");
    Tree identifierX = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    TreeMetaData metaData = metaData(tokenOpen, tokenClose);
    BlockTree initialBlock = new BlockTreeImpl(metaData, singletonList(identifierX));
    BlockTree block = checkJsonSerializationDeserialization(initialBlock, "block.json");
    assertThat(block.children()).hasSize(1);
    assertThat(block.children().get(0).textRange()).isEqualTo(identifierX.textRange());
    assertThat(block.textRange()).isEqualTo(metaData.textRange());

    assertThat(methodNames(BlockTree.class))
      .containsExactlyInAnyOrder(STATEMENT_OR_EXPRESSIONS);
  }

  @Test
  void catch_tree() throws IOException {
    Token tokenCatch = keywordToken(1, 0, "catch");
    Token tokenX = otherToken(1, 8, "x");
    Token tokenY = otherToken(1, 10, "y");
    Tree identifierX = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    Tree identifierY = TreeCreationUtils.identifier(metaData(tokenY), tokenY.text());
    TreeMetaData metaData = metaData(tokenCatch, tokenY);
    CatchTree initialCatch = new CatchTreeImpl(metaData, identifierX, identifierY, tokenCatch);
    CatchTree catchTree = checkJsonSerializationDeserialization(initialCatch, "catch_tree.json");
    assertThat(catchTree.catchParameter().textRange()).isEqualTo(identifierX.textRange());
    assertThat(catchTree.catchBlock().textRange()).isEqualTo(identifierY.textRange());
    assertThat(catchTree.keyword().text()).isEqualTo("catch");

    assertThat(methodNames(CatchTree.class))
      .containsExactlyInAnyOrder(CATCH_PARAMETER, CATCH_BLOCK, KEYWORD);
  }

  @Test
  void catch_tree_without_parameter() throws IOException {
    Token tokenCatch = keywordToken(1, 0, "catch");
    Token tokenY = otherToken(1, 10, "y");
    Tree identifierY = TreeCreationUtils.identifier(metaData(tokenY), tokenY.text());
    TreeMetaData metaData = metaData(tokenCatch, tokenY);
    CatchTree initialCatch = new CatchTreeImpl(metaData, null, identifierY, tokenCatch);
    CatchTree catchTree = checkJsonSerializationDeserialization(initialCatch, "catch_tree_without_parameter.json");
    assertThat(catchTree.catchParameter()).isNull();
    assertThat(catchTree.catchBlock().textRange()).isEqualTo(identifierY.textRange());
    assertThat(catchTree.keyword().text()).isEqualTo("catch");
  }

  @Test
  void class_declaration() throws IOException {
    Token tokenClass = keywordToken(1, 0, "class");
    Token tokenA = otherToken(1, 6, "A");
    otherToken(1, 8, "{");
    Token tokenClose = otherToken(1, 10, "}");
    IdentifierTree identifierA = TreeCreationUtils.identifier(metaData(tokenA), tokenA.text());
    NativeTree nativeTree = new NativeTreeImpl(metaData(tokenA, tokenClose), StringNativeKind.of("CLASS"), singletonList(identifierA));
    ClassDeclarationTree initialTree = new ClassDeclarationTreeImpl(metaData(tokenClass, tokenClose), identifierA, nativeTree);
    ClassDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "class_declaration.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:11 - class A { }");
    assertThat(tokens(tree.identifier())).isEqualTo("1:6:1:7 - A");
    assertThat(tokens(tree.classTree())).isEqualTo("1:6:1:11 - A { }");

    assertThat(methodNames(ClassDeclarationTree.class))
      .containsExactlyInAnyOrder(IDENTIFIER, CLASS_TREE);
  }

  @Test
  void class_declaration_anonymous() throws IOException {
    Token tokenClass = keywordToken(1, 0, "class");
    Token tokenOpen = otherToken(1, 8, "{");
    Token tokenClose = otherToken(1, 10, "}");
    NativeTree nativeTree = new NativeTreeImpl(metaData(tokenOpen, tokenClose), StringNativeKind.of("CLASS"), emptyList());
    ClassDeclarationTree initialTree = new ClassDeclarationTreeImpl(metaData(tokenClass, tokenClose), null, nativeTree);
    ClassDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "class_declaration_anonymous.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:11 - class { }");
    assertThat(tree.identifier()).isNull();
    assertThat(tokens(tree.classTree())).isEqualTo("1:8:1:11 - { }");
  }

  @Test
  void composite_literal() throws IOException {
    Token tokenType = otherToken(1, 0, "MyType");
    otherToken(1, 9, "{");
    Token tokenClose = otherToken(1, 11, "}");
    CompositeLiteralTree compositeLiteralTree = new CompositeLiteralTreeImpl(metaData(tokenType, tokenClose),
      TreeCreationUtils.identifier(metaData(tokenType), tokenType.text()),
      emptyList());
    CompositeLiteralTree tree = checkJsonSerializationDeserialization(compositeLiteralTree, "composite_literal.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:12 - MyType { }");
    assertThat(tree.type()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo(
      "MyType"));
    assertThat(tree.elements()).isEmpty();
  }

  @Test
  void key_value() throws IOException {
    var key = otherToken(1, 0, "Name");
    otherToken(1, 5, ":");
    var value = otherToken(1, 7, "Example");
    var keyValueTree = new KeyValueTreeImpl(metaData(key, value), TreeCreationUtils.identifier(metaData(key), key.text()),
      new LiteralTreeImpl(metaData(value), value.text()));

    var tree = checkJsonSerializationDeserialization(keyValueTree, "key_value.json");

    assertThat(tokens(tree)).isEqualTo("1:0:1:14 - Name : Example");
    assertThat(tree.key()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Name"));
    assertThat(tree.value()).isInstanceOfSatisfying(LiteralTree.class, literal -> assertThat(literal.value()).isEqualTo("Example"));
  }

  @Test
  void exception_handling() throws IOException {
    Token tokenTry = keywordToken(1, 0, "try");
    Token tokenX = otherToken(1, 10, "x");
    Tree identifierX = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());

    Token tokenCatch = keywordToken(1, 20, "catch");
    Token tokenY = otherToken(1, 30, "y");
    Tree identifierY = TreeCreationUtils.identifier(metaData(tokenY), tokenY.text());
    CatchTree catchTree = new CatchTreeImpl(metaData(tokenCatch, tokenY), null, identifierY, tokenCatch);

    keywordToken(1, 40, "finally");
    Token tokenZ = otherToken(1, 50, "z");
    Tree identifierZ = TreeCreationUtils.identifier(metaData(tokenZ), tokenZ.text());

    TreeMetaData metaData = metaData(tokenTry, identifierZ);
    ExceptionHandlingTree initialCatch = new ExceptionHandlingTreeImpl(metaData, identifierX, tokenTry,
      singletonList(catchTree), identifierZ);
    ExceptionHandlingTree tree = checkJsonSerializationDeserialization(initialCatch, "exception_handling.json");
    assertThat(tree.textRange()).isEqualTo(metaData.textRange());
    assertThat(tree.tryBlock().textRange()).isEqualTo(identifierX.textRange());
    assertThat(tree.tryKeyword().text()).isEqualTo("try");
    assertThat(tree.catchBlocks()).hasSize(1);
    assertThat(tree.catchBlocks().get(0)).isInstanceOf(CatchTree.class);
    assertThat(tree.catchBlocks().get(0).keyword().text()).isEqualTo("catch");
    assertThat(tree.finallyBlock()).isNotNull();
    assertThat(tree.finallyBlock().textRange()).isEqualTo(identifierZ.textRange());

    assertThat(methodNames(ExceptionHandlingTree.class))
      .containsExactlyInAnyOrder(TRY_BLOCK, TRY_KEYWORD, CATCH_BLOCKS, FINALLY_BLOCK);
  }

  @Test
  void exception_handling_without_catch() throws IOException {
    Token tokenTry = keywordToken(1, 0, "try");
    Token tokenX = otherToken(1, 10, "x");
    Tree identifierX = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    TreeMetaData metaData = metaData(tokenTry, tokenX);
    ExceptionHandlingTree initialCatch = new ExceptionHandlingTreeImpl(metaData, identifierX, tokenTry, emptyList(), null);
    ExceptionHandlingTree tree = checkJsonSerializationDeserialization(initialCatch, "exception_handling_without_catch.json");
    assertThat(tree.textRange()).isEqualTo(metaData.textRange());
    assertThat(tree.tryBlock().textRange()).isEqualTo(identifierX.textRange());
    assertThat(tree.tryKeyword().text()).isEqualTo("try");
    assertThat(tree.catchBlocks()).isEmpty();
    assertThat(tree.finallyBlock()).isNull();
  }

  @Test
  void function_declaration() throws IOException {
    // A function declaration with everything could look like:
    // func (r MyType[T]) foo[T any](param T) int { }
    // In our case, we simplified the example and content of the children to keep the test simple.
    Token tokenR = otherToken(1, 5, "r");
    NativeTree nativeReceiver = new NativeTreeImpl(metaData(tokenR), StringNativeKind.of("Field"), emptyList());
    Token tokenName = otherToken(1, 7, "foo");
    IdentifierTree name = TreeCreationUtils.identifier(metaData(tokenName), tokenName.text());
    Token tokenTypeParam = otherToken(1, 11, "T");
    NativeTree nativeTypeParameters = new NativeTreeImpl(metaData(tokenTypeParam), StringNativeKind.of("Field"), emptyList());
    Token tokenParam = otherToken(1, 15, "param");
    Tree param = TreeCreationUtils.identifier(metaData(tokenParam), tokenParam.text());
    List<Tree> parameters = singletonList(param);
    Token tokenInt = otherToken(1, 20, "int");
    Tree returnType = TreeCreationUtils.identifier(metaData(tokenInt), tokenInt.text());
    Token tokenOpen = otherToken(1, 23, "{");
    Token tokenClose = otherToken(1, 24, "}");
    BlockTree body = new BlockTreeImpl(metaData(tokenOpen, tokenClose), emptyList());
    TreeMetaData metaData = metaData(tokenR, tokenClose);
    FunctionDeclarationTree initialFunction = new FunctionDeclarationTreeImpl(metaData, returnType, nativeReceiver, name, parameters, nativeTypeParameters, body, null);
    FunctionDeclarationTree function = checkJsonSerializationDeserialization(initialFunction, "function_declaration.json");
    assertThat(function.textRange()).isEqualTo(metaData.textRange());
    assertThat(function.returnType().textRange()).isEqualTo(tokenInt.textRange());
    assertThat(function.name().textRange()).isEqualTo(tokenName.textRange());
    assertThat(function.formalParameters()).hasSize(1);
    assertThat(function.formalParameters().get(0).textRange()).isEqualTo(tokenParam.textRange());
    assertThat(function.body().textRange()).isEqualTo(body.textRange());

    assertThat(methodNames(FunctionDeclarationTree.class))
      .containsExactlyInAnyOrder(RETURN_TYPE, RECEIVER, NAME, FORMAL_PARAMETERS, TYPE_PARAMETERS, BODY,
        "cfg", "rangeToHighlight", "receiverName", "receiverType", "signature");
  }

  @Test
  void identifier() throws IOException {
    Token token = otherToken(1, 0, "foo");
    IdentifierTree initialIdentifier = TreeCreationUtils.identifier(metaData(token), token.text());
    IdentifierTree identifier = checkJsonSerializationDeserialization(initialIdentifier, "identifier.json");
    assertThat(identifier.name()).isEqualTo("foo");
    assertThat(identifier.type()).isEqualTo("UNKNOWN");
    assertThat(identifier.identifier()).isEqualTo("foo");
    assertThat(identifier.textRange()).isEqualTo(token.textRange());

    assertThat(methodNames(IdentifierTree.class))
      .containsExactlyInAnyOrder(NAME, "identifier", "type", "packageName", "id", "symbol", "setSymbol");
  }

  @Test
  void identifier_type_and_package() throws IOException {
    Token token = otherToken(1, 0, "foo");
    IdentifierTree initialIdentifier = TreeCreationUtils.identifier(metaData(token), token.text(), "bar", "baz", 1);
    IdentifierTree identifier = checkJsonSerializationDeserialization(initialIdentifier, "identifier_type_and_package.json");
    assertThat(identifier.name()).isEqualTo("foo");
    assertThat(identifier.type()).isEqualTo("bar");
    assertThat(identifier.packageName()).isEqualTo("baz");
    assertThat(identifier.id()).isEqualTo(1);
    assertThat(identifier.identifier()).isEqualTo("foo");
    assertThat(identifier.textRange()).isEqualTo(token.textRange());

    assertThat(methodNames(IdentifierTree.class))
      .containsExactlyInAnyOrder(NAME, "identifier", "type", "packageName", "id", "symbol", "setSymbol");
  }

  @Test
  void if_tree() throws IOException {
    Token tokenIf = keywordToken(1, 0, "if");
    Token tokenTrue = otherToken(1, 3, "true");
    Tree condition = new LiteralTreeImpl(metaData(tokenTrue), tokenTrue.text());

    Token tokenX = otherToken(1, 8, "x");
    Tree thenBranch = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());

    Token tokenElse = otherToken(1, 10, "else");
    Token tokenY = otherToken(1, 15, "y");
    Tree elseBranch = TreeCreationUtils.identifier(metaData(tokenY), tokenY.text());

    IfTree initialTree = new IfTreeImpl(metaData(tokenIf, tokenY), condition, thenBranch, elseBranch, tokenIf, tokenElse);
    IfTree tree = checkJsonSerializationDeserialization(initialTree, "if_tree.json");
    assertThat(tree.ifKeyword().text()).isEqualTo("if");
    assertThat(tree.thenBranch().textRange()).isEqualTo(thenBranch.textRange());
    assertThat(tree.elseKeyword().text()).isEqualTo("else");
    assertThat(tree.elseBranch().textRange()).isEqualTo(elseBranch.textRange());

    assertThat(methodNames(IfTree.class))
      .containsExactlyInAnyOrder(CONDITION, THEN_BRANCH, ELSE_BRANCH, IF_KEYWORD, ELSE_KEYWORD);
  }

  @Test
  void if_tree_without_else() throws IOException {
    Token tokenIf = keywordToken(1, 0, "if");
    Token tokenTrue = otherToken(1, 3, "true");
    Tree condition = new LiteralTreeImpl(metaData(tokenTrue), tokenTrue.text());

    Token tokenX = otherToken(1, 8, "x");
    Tree thenBranch = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());

    Token tokenElse = null;
    Tree elseBranch = null;
    IfTree initialTree = new IfTreeImpl(metaData(tokenIf, tokenX), condition, thenBranch, elseBranch, tokenIf, tokenElse);
    IfTree tree = checkJsonSerializationDeserialization(initialTree, "if_tree_without_else.json");
    assertThat(tree.ifKeyword().text()).isEqualTo("if");
    assertThat(tree.thenBranch().textRange()).isEqualTo(thenBranch.textRange());
    assertThat(tree.elseKeyword()).isNull();
    assertThat(tree.elseBranch()).isNull();
  }

  @Test
  void import_declaration() throws IOException {
    Token tokenImport = keywordToken(1, 0, "import");
    Token tokenLib = otherToken(1, 7, "lib");
    Tree lib = TreeCreationUtils.identifier(metaData(tokenLib), tokenLib.text());
    ImportDeclarationTree initialTree = new ImportDeclarationTreeImpl(metaData(tokenImport, tokenLib), singletonList(lib));
    ImportDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "import_declaration.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:10 - import lib");
    assertThat(tree.children()).hasSize(1);
    assertThat(tokens(tree.children().get(0))).isEqualTo("1:7:1:10 - lib");

    assertThat(methodNames(ImportDeclarationTree.class))
      .isEmpty();
  }

  @Test
  void import_specification() throws IOException {
    Token tokenName = otherToken(1, 7, "name");
    IdentifierTree name = TreeCreationUtils.identifier(metaData(tokenName), tokenName.text());
    Token tokenPath = otherToken(1, 13, "\"path\"");
    StringLiteralTree path = new StringLiteralTreeImpl(metaData(tokenPath), tokenPath.text());
    ImportSpecificationTree initialTree = new ImportSpecificationTreeImpl(metaData(tokenName, tokenPath), name, path);
    ImportSpecificationTree tree = checkJsonSerializationDeserialization(initialTree, "import_specification.json");
    assertThat(tokens(tree)).isEqualTo("1:7:1:19 - name \"path\"");
    assertThat(tree.children()).hasSize(2);
    assertThat(tokens(tree.name())).isNotNull().isEqualTo("1:7:1:11 - name");
    assertThat(tokens(tree.path())).isEqualTo("1:13:1:19 - \"path\"");

    assertThat(methodNames(ImportSpecificationTree.class))
      .containsExactlyInAnyOrder(NAME, PATH);
  }

  @Test
  void integer_literal() throws IOException {
    Token token = otherToken(1, 0, "0xFF");
    IntegerLiteralTree initialLiteral = new IntegerLiteralTreeImpl(metaData(token), token.text());
    IntegerLiteralTree literal = checkJsonSerializationDeserialization(initialLiteral, "integer_literal.json");
    assertThat(literal.value()).isEqualTo("0xFF");
    assertThat(literal.getBase()).isEqualTo(IntegerLiteralTree.Base.HEXADECIMAL);
    assertThat(literal.getNumericPart()).isEqualTo("FF");
    assertThat(literal.getIntegerValue()).isEqualTo(BigInteger.valueOf(255));

    assertThat(methodNames(IntegerLiteralTree.class))
      .containsExactlyInAnyOrder(VALUE, "getBase", "getIntegerValue", "getNumericPart");
  }

  @Test
  void testIndexExpression() throws IOException {
    var exprToken = otherToken(1, 0, "x");
    var expr = TreeCreationUtils.identifier(metaData(exprToken), exprToken.text());
    var tokenOpen = otherToken(1, 1, "[");

    var indexToken = otherToken(1, 2, "42");
    var index = new IntegerLiteralTreeImpl(metaData(indexToken), indexToken.text());
    var tokenClose = otherToken(1, 4, "]");

    var initialIndexExpressionTree = new IndexExpressionTreeImpl(metaData(tokenOpen, tokenClose), expr, index);
    var indexExpressionTree = checkJsonSerializationDeserialization(initialIndexExpressionTree, "index_expression.json");

    assertThat(indexExpressionTree.expression()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("x"));
    assertThat(indexExpressionTree.index()).isInstanceOfSatisfying(IntegerLiteralTree.class, integerLiteralTree -> assertThat(integerLiteralTree.value()).isEqualTo("42"));

    assertThat(methodNames(IndexExpressionTree.class))
      .containsExactlyInAnyOrder(EXPRESSION, INDEX);
  }

  @Test
  void testIndexListExpression() throws IOException {
    var exprToken = otherToken(1, 0, "x");
    var expr = TreeCreationUtils.identifier(metaData(exprToken), exprToken.text());
    var tokenOpen = otherToken(1, 1, "[");

    var indexToken1 = otherToken(1, 2, "1");
    var index1 = new IntegerLiteralTreeImpl(metaData(indexToken1), indexToken1.text());
    otherToken(1, 3, ",");
    var indexToken2 = otherToken(1, 4, "2");
    var index2 = new IntegerLiteralTreeImpl(metaData(indexToken2), indexToken2.text());
    var tokenClose = otherToken(1, 5, "]");

    var initialIndexExpressionTree = new IndexListExpressionTreeImpl(metaData(tokenOpen, tokenClose), expr, List.of(index1, index2));
    var indexExpressionTree = checkJsonSerializationDeserialization(initialIndexExpressionTree, "index_list_expression.json");

    assertThat(indexExpressionTree.expression()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("x"));
    assertThat(indexExpressionTree.indices()).hasSize(2);
    assertThat(indexExpressionTree.indices().get(0)).isInstanceOfSatisfying(IntegerLiteralTree.class, integerLiteralTree -> assertThat(integerLiteralTree.value()).isEqualTo("1"));
    assertThat(indexExpressionTree.indices().get(1)).isInstanceOfSatisfying(IntegerLiteralTree.class, integerLiteralTree -> assertThat(integerLiteralTree.value()).isEqualTo("2"));

    assertThat(methodNames(IndexListExpressionTree.class)).containsExactlyInAnyOrder(EXPRESSION, INDICES);
  }

  @Test
  void float_literal() throws IOException {
    Token token = otherToken(1, 0, "5.0");
    FloatLiteralTree initialLiteral = new FloatLiteralTreeImpl(metaData(token), token.text());
    FloatLiteralTree literal = checkJsonSerializationDeserialization(initialLiteral, "float_literal.json");
    assertThat(literal.value()).isEqualTo("5.0");
    assertThat(literal.getFloatValue()).isEqualTo(BigDecimal.valueOf(5.0));

    assertThat(methodNames(FloatLiteralTree.class))
      .containsExactlyInAnyOrder(VALUE, "getFloatValue");
  }

  @Test
  void imaginary_literal() throws IOException {
    Token token = otherToken(1, 0, "5i");
    ImaginaryLiteralTree initialLiteral = new ImaginaryLiteralTreeImpl(metaData(token), token.text());
    ImaginaryLiteralTree literal = checkJsonSerializationDeserialization(initialLiteral, "imaginary_literal.json");
    assertThat(literal.value()).isEqualTo("5i");

    assertThat(methodNames(ImaginaryLiteralTree.class))
      .containsExactlyInAnyOrder(VALUE);
  }

  @Test
  void jump() throws IOException {
    Token tokenKeyword = keywordToken(1, 0, "break");
    Token tokenLabel = otherToken(1, 6, "hard");
    IdentifierTree label = TreeCreationUtils.identifier(metaData(tokenLabel), tokenLabel.text());
    JumpTree.JumpKind kind = JumpTree.JumpKind.BREAK;
    JumpTree initialTree = new JumpTreeImpl(metaData(tokenKeyword, tokenLabel), tokenKeyword, kind, label);
    JumpTree tree = checkJsonSerializationDeserialization(initialTree, "jump.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:10 - break hard");
    assertThat(tokens(tree.label())).isEqualTo("1:6:1:10 - hard");
    assertThat(tree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);
    assertThat(token(tree.keyword())).isEqualTo("1:0:1:5 - break");

    assertThat(methodNames(JumpTree.class))
      .containsExactlyInAnyOrder(LABEL, KEYWORD, KIND);
  }

  @Test
  void literal() throws IOException {
    Token token = otherToken(1, 0, "true");
    LiteralTree initialLiteral = new LiteralTreeImpl(metaData(token), token.text());
    LiteralTree literal = checkJsonSerializationDeserialization(initialLiteral, "literal.json");
    assertThat(literal.value()).isEqualTo("true");
    TreeMetaData metaData = literal.metaData();
    assertThat(metaData.textRange()).isEqualTo(token.textRange());
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.commentsInside()).isEmpty();
    assertThat(metaData.tokens()).hasSize(1);
    Token metaDataToken = metaData.tokens().get(0);
    assertThat(metaDataToken.text()).isEqualTo("true");
    assertThat(metaDataToken.type()).isEqualTo(Token.Type.OTHER);
    assertThat(metaDataToken.textRange()).isEqualTo(token.textRange());

    assertThat(methodNames(LiteralTree.class))
      .containsExactlyInAnyOrder(VALUE);
  }

  @Test
  void loop() throws IOException {
    Token tokenSwitch = keywordToken(1, 0, "while");
    Token tokenTrue = otherToken(1, 6, "true");
    Token tokenX = keywordToken(1, 11, "x");
    Tree condition = new LiteralTreeImpl(metaData(tokenTrue), tokenTrue.text());
    Tree body = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    LoopTree initialTree = new LoopTreeImpl(metaData(tokenSwitch, tokenX), condition, body, LoopTree.LoopKind.WHILE, tokenSwitch);
    LoopTree tree = checkJsonSerializationDeserialization(initialTree, "loop.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:12 - while true x");
    assertThat(tokens(tree.condition())).isNotNull();
    assertThat(tokens(tree.condition())).isEqualTo("1:6:1:10 - true");
    assertThat(tokens(tree.body())).isEqualTo("1:11:1:12 - x");
    assertThat(tree.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(token(tree.keyword())).isEqualTo("1:0:1:5 - while");

    assertThat(methodNames(LoopTree.class))
      .containsExactlyInAnyOrder(CONDITION, BODY, KIND, KEYWORD);
  }

  @Test
  void match() throws IOException {
    Token tokenSwitch = keywordToken(1, 0, "switch");
    Token tokenX = otherToken(1, 7, "x");
    Token tokenDefault = keywordToken(1, 9, "default");
    Token tokenValue = otherToken(1, 17, "42");

    Tree expression = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    Tree value = new IntegerLiteralTreeImpl(metaData(tokenValue), tokenValue.text());
    MatchCaseTree matchCase = new MatchCaseTreeImpl(metaData(tokenDefault, tokenValue), null, value);
    MatchTree initialTree = new MatchTreeImpl(metaData(tokenSwitch, tokenValue), expression, singletonList(matchCase), tokenSwitch);
    MatchTree tree = checkJsonSerializationDeserialization(initialTree, "match.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:19 - switch x default 42");
    assertThat(tokens(tree.expression())).isEqualTo("1:7:1:8 - x");
    assertThat(tree.cases()).hasSize(1);
    assertThat(tokens(tree.cases().get(0))).isEqualTo("1:9:1:19 - default 42");
    assertThat(token(tree.keyword())).isEqualTo("1:0:1:6 - switch");

    assertThat(methodNames(MatchTree.class))
      .containsExactlyInAnyOrder(EXPRESSION, CASES, KEYWORD);
  }

  @Test
  void match_case() throws IOException {
    Token tokenCase = keywordToken(1, 0, "case");
    Token tokenSeven = otherToken(1, 5, "7");
    otherToken(1, 7, ":");
    Token tokenX = otherToken(1, 9, "x");
    Tree expression = new IntegerLiteralTreeImpl(metaData(tokenSeven), tokenSeven.text());
    Tree body = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    TreeMetaData metaData = metaData(tokenCase, tokenX);
    MatchCaseTree initialTree = new MatchCaseTreeImpl(metaData, expression, body);
    MatchCaseTree tree = checkJsonSerializationDeserialization(initialTree, "match_case.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:10 - case 7 : x");
    assertThat(tokens(tree.expression())).isEqualTo("1:5:1:6 - 7");
    assertThat(tokens(tree.body())).isEqualTo("1:9:1:10 - x");

    assertThat(methodNames(MatchCaseTree.class))
      .containsExactlyInAnyOrder(EXPRESSION, BODY, "rangeToHighlight");
  }

  @Test
  void modifier() throws IOException {
    Token tokenPublic = keywordToken(1, 0, "public");
    ModifierTree initialTree = new ModifierTreeImpl(metaData(tokenPublic), ModifierTree.Kind.PUBLIC);
    ModifierTree tree = checkJsonSerializationDeserialization(initialTree, "modifier.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:6 - public");
    assertThat(tree.kind()).isEqualTo(ModifierTree.Kind.PUBLIC);

    assertThat(methodNames(ModifierTree.class))
      .containsExactlyInAnyOrder(KIND);
  }

  @Test
  void native_tree() throws IOException {
    Token token10 = otherToken(10, 0, "token10");
    Token token21 = otherToken(21, 0, "token21");
    Token token22 = otherToken(22, 0, "token22");
    NativeTree initialTree = new NativeTreeImpl(metaData(token10, token22), StringNativeKind.of("PARENT"),
      Arrays.asList(
        new NativeTreeImpl(metaData(token21), StringNativeKind.of("CHILD"), emptyList()),
        new NativeTreeImpl(metaData(token22), StringNativeKind.of("CHILD"), emptyList())));
    NativeTree tree = checkJsonSerializationDeserialization(initialTree, "native_tree.json");
    assertThat(tree.nativeKind()).isInstanceOfSatisfying(StringNativeKind.class, stringNativeKind -> assertThat(stringNativeKind.kind()).isEqualTo("PARENT"));
    assertThat(tree.children()).hasSize(2);
    assertThat(((NativeTree) tree.children().get(0)).nativeKind()).isInstanceOfSatisfying(StringNativeKind.class,
      stringNativeKind -> assertThat(stringNativeKind.kind()).isEqualTo("CHILD"));
    assertThat(tokens(tree.children().get(0))).isEqualTo("21:0:21:7 - token21");
    assertThat(tokens(tree.children().get(1))).isEqualTo("22:0:22:7 - token22");

    assertThat(methodNames(NativeTree.class))
      .containsExactlyInAnyOrder(NATIVE_KIND);
  }

  @Test
  void package_declaration() throws IOException {
    Token tokenPackage = keywordToken(1, 0, "package");
    Token tokenName = otherToken(1, 8, "hello");
    IdentifierTree name = TreeCreationUtils.identifier(metaData(tokenName), tokenName.text());
    TreeMetaData metaData = metaData(tokenPackage, tokenName);
    PackageDeclarationTree initialTree = new PackageDeclarationTreeImpl(metaData, Collections.singletonList(name));
    PackageDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "package_declaration.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:13 - package hello");
    assertThat(tree.children()).hasSize(1);
    assertThat(tokens(tree.children().get(0))).isEqualTo("1:8:1:13 - hello");

    assertThat(methodNames(PackageDeclarationTree.class))
      .containsExactly("packageName");
  }

  @Test
  void parameter() throws IOException {
    Token tokenX = otherToken(2, 0, "x");
    Token tokenInt = otherToken(2, 2, "int");

    IdentifierTree identifier = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    Tree type = TreeCreationUtils.identifier(metaData(tokenInt), tokenInt.text());

    TreeMetaData metaData = metaData(tokenX, tokenInt);
    ParameterTree initialTree = new ParameterTreeImpl(metaData, identifier, type);
    ParameterTree tree = checkJsonSerializationDeserialization(initialTree, "parameter.json");
    assertThat(tokens(tree)).isEqualTo("2:0:2:5 - x int");
    assertThat(tokens(tree.identifier())).isEqualTo("2:0:2:1 - x");
    assertThat(tokens(tree.typeTree())).isEqualTo("2:2:2:5 - int");

    assertThat(methodNames(ParameterTree.class))
      .containsExactlyInAnyOrder(IDENTIFIER, TYPE, "typeTree");
  }

  @Test
  void parenthesized_expression() throws IOException {
    Token leftParenthesis = otherToken(1, 0, "(");
    Token tokenX = otherToken(1, 1, "x");
    Token rightParenthesis = otherToken(1, 2, ")");
    Tree expression = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    TreeMetaData metaData = metaData(leftParenthesis, rightParenthesis);
    ParenthesizedExpressionTree initialTree = new ParenthesizedExpressionTreeImpl(metaData, expression, leftParenthesis, rightParenthesis);
    ParenthesizedExpressionTree tree = checkJsonSerializationDeserialization(initialTree, "parenthesized_expression.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:3 - ( x )");
    assertThat(tokens(tree.expression())).isEqualTo("1:1:1:2 - x");
    assertThat(token(tree.leftParenthesis())).isEqualTo("1:0:1:1 - (");
    assertThat(token(tree.rightParenthesis())).isEqualTo("1:2:1:3 - )");

    assertThat(methodNames(ParenthesizedExpressionTree.class))
      .containsExactlyInAnyOrder(EXPRESSION, LEFT_PARENTHESIS, RIGHT_PARENTHESIS);
  }

  @Test
  void place_holder() throws IOException {
    Token token = keywordToken(1, 0, "_");
    PlaceHolderTree initialTree = new PlaceHolderTreeImpl(metaData(token), token);
    PlaceHolderTree tree = checkJsonSerializationDeserialization(initialTree, "place_holder.json");
    assertThat(tree.textRange()).isEqualTo(token.textRange());
    assertThat(tree.placeHolderToken().textRange()).isEqualTo(token.textRange());
    assertThat(tree.placeHolderToken().text()).isEqualTo("_");

    assertThat(methodNames(PlaceHolderTree.class))
      .containsExactlyInAnyOrder(PLACE_HOLDER_TOKEN, IDENTIFIER, NAME, "type", "packageName", "id", "symbol", "setSymbol");
  }

  @Test
  void return_true() throws IOException {
    Token returnToken = keywordToken(1, 0, "return");
    Token trueToken = otherToken(1, 7, "true");
    Token semicolonToken = otherToken(1, 11, ";");
    Tree trueLiteral = new LiteralTreeImpl(metaData(trueToken), trueToken.text());
    ReturnTree initialReturnTree = new ReturnTreeImpl(metaData(returnToken, semicolonToken), returnToken, List.of(trueLiteral));
    ReturnTree returnTree = checkJsonSerializationDeserialization(initialReturnTree, "return_true.json");
    assertThat(returnTree.keyword().text()).isEqualTo("return");
    assertThat(returnTree.expressions()).hasSize(1);
    assertThat(returnTree.expressions().get(0)).isInstanceOf(LiteralTree.class);
    TreeMetaData metaData = returnTree.metaData();
    assertThat(metaData.textRange()).isEqualTo(new TextRangeImpl(1, 0, 1, 12));
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.commentsInside()).isEmpty();
    assertThat(metaData.tokens()).hasSize(3);

    assertThat(methodNames(ReturnTree.class))
      .containsExactlyInAnyOrder(KEYWORD, EXPRESSIONS);
  }

  @Test
  void star_expression() throws IOException {
    Token tokenStar = otherToken(1, 0, "*");
    Token tokenString = otherToken(1, 1, "string");
    IdentifierTree identifierTree = TreeCreationUtils.identifier(metaData(tokenString), tokenString.text());
    StarExpressionTree initialStarExpression = new StarExpressionTreeImpl(metaData(tokenStar, tokenString), identifierTree);
    StarExpressionTree starExpressionTree = checkJsonSerializationDeserialization(initialStarExpression, "star_expression.json");
    assertThat(starExpressionTree.operand()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("string"));
  }

  @Test
  void string_literal() throws IOException {
    Token token = stringToken(1, 0, "\"a\"");
    StringLiteralTree initialLiteral = new StringLiteralTreeImpl(metaData(token), token.text());
    StringLiteralTree literal = checkJsonSerializationDeserialization(initialLiteral, "string_literal.json");
    assertThat(literal.value()).isEqualTo("\"a\"");
    assertThat(literal.content()).isEqualTo("a");
    TreeMetaData metaData = literal.metaData();
    assertThat(metaData.textRange()).isEqualTo(token.textRange());
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.commentsInside()).isEmpty();
    assertThat(metaData.tokens()).hasSize(1);
    Token metaDataToken = metaData.tokens().get(0);
    assertThat(metaDataToken.text()).isEqualTo("\"a\"");
    assertThat(metaDataToken.type()).isEqualTo(Token.Type.STRING_LITERAL);
    assertThat(metaDataToken.textRange()).isEqualTo(token.textRange());

    assertThat(methodNames(StringLiteralTree.class))
      .containsExactlyInAnyOrder(CONTENT, VALUE);
  }

  @Test
  void throw_tree() throws IOException {
    Token tokenThrow = keywordToken(1, 0, "throw");
    Token tokenEx = otherToken(1, 6, "ex");
    Tree body = TreeCreationUtils.identifier(metaData(tokenEx), tokenEx.text());
    ThrowTree initialTree = new ThrowTreeImpl(metaData(tokenThrow, tokenEx), tokenThrow, body);
    ThrowTree tree = checkJsonSerializationDeserialization(initialTree, "throw_tree.json");
    assertThat(tree.keyword().text()).isEqualTo("throw");
    assertThat(tree.body().textRange()).isEqualTo(tokenEx.textRange());

    assertThat(methodNames(ThrowTree.class))
      .containsExactlyInAnyOrder(KEYWORD, BODY);
  }

  @Test
  void throw_nothing() throws IOException {
    Token tokenThrow = keywordToken(1, 0, "throw");
    ThrowTree initialTree = new ThrowTreeImpl(metaData(tokenThrow), tokenThrow, null);
    ThrowTree tree = checkJsonSerializationDeserialization(initialTree, "throw_nothing.json");
    assertThat(tree.keyword().text()).isEqualTo("throw");
    assertThat(tree.body()).isNull();
  }

  @Test
  void top_level() throws IOException {
    Comment comment = comment(1, 0, "// hello", 2, 0);
    Token token = otherToken(2, 0, "true");
    Tree trueLiteral = new LiteralTreeImpl(metaData(token), token.text());
    TreeMetaData metaData = metaData(comment, token);
    TopLevelTree initialTree = new TopLevelTreeImpl(metaData, singletonList(trueLiteral), singletonList(comment), token);
    TopLevelTree tree = checkJsonSerializationDeserialization(initialTree, "top_level.json");
    assertThat(tree.allComments()).hasSize(1);
    assertThat(tree.allComments().get(0).text()).isEqualTo("// hello");
    assertThat(tree.declarations()).hasSize(1);
    assertThat(tree.declarations().get(0).metaData().textRange()).isEqualTo(token.textRange());
    assertThat(tree.firstCpdToken().text()).isEqualTo("true");

    assertThat(methodNames(TopLevelTree.class))
      .containsExactlyInAnyOrder(DECLARATIONS, FIRST_CPD_TOKEN, "allComments", "doesImportType", "packageName");
  }

  @Test
  void unary_expression() throws IOException {
    Token tokenMinus = otherToken(1, 0, "-");
    Token tokenX = otherToken(1, 1, "x");
    UnaryExpressionTree.Operator operator = UnaryExpressionTree.Operator.MINUS;
    Tree operand = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    UnaryExpressionTree initialTree = new UnaryExpressionTreeImpl(metaData(tokenMinus, tokenX), operator, operand);
    UnaryExpressionTree tree = checkJsonSerializationDeserialization(initialTree, "unary_expression.json");
    assertThat(tree.operator()).isEqualTo(UnaryExpressionTree.Operator.MINUS);
    assertThat(tree.operand().textRange()).isEqualTo(operand.textRange());

    assertThat(methodNames(UnaryExpressionTree.class))
      .containsExactlyInAnyOrder(OPERATOR, OPERAND);
  }

  @Test
  void unary_expression_address_of() throws IOException {
    Token tokenAddressOf = otherToken(1, 0, "&");
    Token tokenX = otherToken(1, 1, "x");
    UnaryExpressionTree.Operator operator = UnaryExpressionTree.Operator.ADDRESS_OF;
    Tree operand = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    UnaryExpressionTree initialTree = new UnaryExpressionTreeImpl(metaData(tokenAddressOf, tokenX), operator, operand);
    UnaryExpressionTree tree = checkJsonSerializationDeserialization(initialTree, "unary_expression_address_of.json");
    assertThat(tree.operator()).isEqualTo(UnaryExpressionTree.Operator.ADDRESS_OF);
    assertThat(tree.operand().textRange()).isEqualTo(operand.textRange());

    assertThat(methodNames(UnaryExpressionTree.class))
      .containsExactlyInAnyOrder(OPERATOR, OPERAND);
  }

  @Test
  void variable_declaration() throws IOException {
    Token tokenInt = otherToken(1, 0, "int");
    Token tokenX = otherToken(1, 4, "x");
    Token tokenValue = otherToken(1, 6, "42");
    IdentifierTree identifier = TreeCreationUtils.identifier(metaData(tokenX), tokenX.text());
    IdentifierTree type = TreeCreationUtils.identifier(metaData(tokenInt), tokenInt.text());
    Tree initializer = new IntegerLiteralTreeImpl(metaData(tokenValue), tokenValue.text());
    VariableDeclarationTree initialTree = new VariableDeclarationTreeImpl(
      metaData(tokenInt, tokenValue), List.of(identifier), type, List.of(initializer), true);
    VariableDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "variable_declaration.json");
    assertThat(tree.identifiers()).hasSize(1);
    assertThat(tree.identifiers().get(0).name()).isEqualTo("x");
    assertThat(((IdentifierTree) tree.type()).name()).isEqualTo("int");
    assertThat(tree.initializers()).hasSize(1);
    assertThat(tree.initializers().get(0)).isInstanceOfSatisfying(IntegerLiteralTree.class, val -> assertThat(val.getIntegerValue()).isEqualTo(BigInteger.valueOf(42)));
    assertThat(tree.isVal()).isTrue();

    assertThat(methodNames(VariableDeclarationTree.class))
      .containsExactlyInAnyOrder(IDENTIFIERS, TYPE, INITIALIZERS, IS_VAL);
  }

  @Test
  void member_select() throws IOException {
    // x.y.z
    Token tokenX = otherToken(1, 0, "x");
    Token tokenY = otherToken(1, 2, "y");
    Token tokenZ = otherToken(1, 4, "z");
    MemberSelectTree memberSelectTree = new MemberSelectTreeImpl(
      metaData(tokenX, tokenZ),
      new MemberSelectTreeImpl(
        metaData(tokenX, tokenY),
        TreeCreationUtils.identifier(metaData(tokenX), tokenX.text()),
        TreeCreationUtils.identifier(metaData(tokenY), tokenY.text())),
      TreeCreationUtils.identifier(metaData(tokenZ), tokenZ.text()));

    MemberSelectTree tree = checkJsonSerializationDeserialization(memberSelectTree, "member_select.json");
    assertThat(tree.identifier().name()).isEqualTo("z");
    assertThat(tree.expression()).isInstanceOf(MemberSelectTree.class);
    MemberSelectTree nestedExpression = (MemberSelectTree) tree.expression();
    assertThat(nestedExpression.identifier().name()).isEqualTo("y");
    assertThat(nestedExpression.expression()).isInstanceOf(IdentifierTree.class);
    assertThat(((IdentifierTree) nestedExpression.expression()).name()).isEqualTo("x");
  }

  @Test
  void function_invocation() throws IOException {
    Token tokenBar = otherToken(1, 0, "bar");
    Token tokenArg = stringToken(1, 4, "\"arg\"");
    Tree argument = new StringLiteralTreeImpl(metaData(tokenArg), tokenArg.text());
    TreeMetaData metaData = metaData(tokenBar, tokenArg);
    FunctionInvocationTree initialInvocation = new FunctionInvocationTreeImpl(metaData, TreeCreationUtils.identifier(metaData(tokenBar),
      tokenBar.text()), singletonList(argument), List.of(TypeImpl.createFromType("string")));
    FunctionInvocationTree invocation = checkJsonSerializationDeserialization(initialInvocation, "function_invocation.json");
    assertThat(invocation.memberSelect()).isInstanceOfSatisfying(IdentifierTree.class,
      identifier -> assertThat(identifier.name()).isEqualTo("bar"));
    assertThat(invocation.arguments()).hasSize(1);
    assertThat(((StringLiteralTree) invocation.arguments().get(0)).value()).isEqualTo("\"arg\"");
  }

  @Test
  void testEllipsis() throws IOException {
    Token ellipsis = otherToken(1, 0, "...");
    Token typeToken = otherToken(1, 4, "string");
    Tree element = TreeCreationUtils.identifier(metaData(typeToken), typeToken.text());
    TreeMetaData metaData = metaData(ellipsis, typeToken);

    EllipsisTreeImpl ellipsisTree = new EllipsisTreeImpl(metaData, ellipsis, element);

    var tree = checkJsonSerializationDeserialization(ellipsisTree, "ellipsis.json");

    assertThat(tokens(tree)).isEqualTo("1:0:1:10 - ... string");
    assertThat(tree.ellipsis().text()).isEqualTo("...");
    assertThat(tree.element()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("string"));

    assertThat(methodNames(EllipsisTree.class)).containsExactlyInAnyOrder(ELEMENT, ELLIPSIS);
  }

  @Test
  void testMapType() throws IOException {
    var tokenMap = otherToken(1, 0, "map");
    otherToken(1, 1, "[");
    var tokenKey = otherToken(1, 2, "string");
    otherToken(1, 8, "]");
    var tokenValue = otherToken(1, 9, "int");

    var integerLiteral = TreeCreationUtils.identifier(metaData(tokenKey), tokenKey.text());
    var identifier = TreeCreationUtils.identifier(metaData(tokenValue), tokenValue.text());

    var metaData = metaData(tokenMap, tokenValue);
    var mapTypeTree = new MapTypeTreeImpl(metaData, integerLiteral, identifier);

    var expression = checkJsonSerializationDeserialization(mapTypeTree, "map_type.json");
    assertThat(expression.key().textRange()).isEqualTo(tokenKey.textRange());
    assertThat(expression.value().textRange()).isEqualTo(tokenValue.textRange());

    assertThat(methodNames(MapTypeTree.class))
      .containsExactlyInAnyOrder(KEY, VALUE, TYPE);
  }

  @Test
  void testSlice() throws IOException {
    var tokenExpr = otherToken(1, 0, "arr");
    otherToken(1, 4, "[");
    var tokenLow = otherToken(1, 5, "1");
    otherToken(1, 6, ":");
    var tokenHigh = otherToken(1, 7, "4");
    otherToken(1, 8, ":");
    var tokenMax = otherToken(1, 9, "6");
    var lastToken = otherToken(1, 10, "]");

    var expr = TreeCreationUtils.identifier(metaData(tokenExpr), tokenExpr.text());
    var low = TreeCreationUtils.integerLiteral(metaData(tokenLow), tokenLow.text());
    var high = TreeCreationUtils.integerLiteral(metaData(tokenHigh), tokenHigh.text());
    var max = TreeCreationUtils.integerLiteral(metaData(tokenMax), tokenMax.text());

    var metaData = metaData(tokenExpr, lastToken);
    var sliceTree = new SliceTreeImpl(metaData, expr, low, high, max, true);

    var expression = checkJsonSerializationDeserialization(sliceTree, "slice.json");
    assertThat(expression.expression().textRange()).isEqualTo(tokenExpr.textRange());
    assertThat(expression.low().textRange()).isEqualTo(tokenLow.textRange());
    assertThat(expression.high().textRange()).isEqualTo(tokenHigh.textRange());
    assertThat(expression.max().textRange()).isEqualTo(tokenMax.textRange());
    assertThat(expression.slice3()).isTrue();

    assertThat(methodNames(SliceTree.class))
      .containsExactlyInAnyOrder(EXPRESSION, LOW, HIGH, MAX, SLICE_3);
  }

  @Test
  void testTypeAssertionExpression() throws IOException {
    var tokenExpr = otherToken(1, 0, "x");
    otherToken(1, 2, ".");
    otherToken(1, 3, "(");
    var tokenType = otherToken(1, 4, "type");
    Token closingParenthesis = otherToken(1, 8, ")");
    var expr = TreeCreationUtils.identifier(metaData(tokenExpr), tokenExpr.text());
    var type = TreeCreationUtils.identifier(metaData(tokenType), tokenType.text());

    var metaData = metaData(tokenExpr, closingParenthesis);
    var typeAssertionExpression = new TypeAssertionExpressionTreeImpl(metaData, expr, type);

    var expression = checkJsonSerializationDeserialization(typeAssertionExpression, "type_assertion_expression.json");
    assertThat(expression.textRange()).isEqualTo(metaData.textRange());
    assertThat(expression.expression().textRange()).isEqualTo(expr.textRange());
    assertThat(expression.type().textRange()).isEqualTo(tokenType.textRange());

    assertThat(methodNames(TypeAssertionExpressionTreeImpl.class))
      .containsExactlyInAnyOrder(EXPRESSION, TYPE);
  }

  @Test
  void shouldConvertParsingError() throws IOException {
    var content = indentedJsonFromFile("parsing_error.json");

    var filenameToTreeOrError = JsonTree.fromJson(content);
    assertThat(filenameToTreeOrError).containsOnly(Map.entry(
      "resources/invalid_file.go",
      new TreeOrError(null, "resources/invalid_file.go:1:1: expected 'package', found xpackage")));
  }

  @Test
  void shouldConvertErroneousContent() throws IOException {
    var content = indentedJsonFromFile("erroneous_content.json");
    var filenameToTreeOrError = JsonTree.fromJson(content);
    assertThat(filenameToTreeOrError).hasSize(1).containsOnlyKeys("resources/erroneous_content.go");
    TreeOrError treeOrError = filenameToTreeOrError.get("resources/erroneous_content.go");
    assertThat(treeOrError.isError()).isTrue();
    assertThat(treeOrError.error()).startsWith("Error converting json tree: Expect String instead of 'JsonLiteral' for field 'value' at 'tree/Literal'");
  }
}
