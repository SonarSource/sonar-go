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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.CatchTree;
import org.sonar.go.api.ClassDeclarationTree;
import org.sonar.go.api.Comment;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.ExceptionHandlingTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IfTree;
import org.sonar.go.api.ImportDeclarationTree;
import org.sonar.go.api.ImportSpecificationTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.api.JumpTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchCaseTree;
import org.sonar.go.api.MatchTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.ModifierTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.PackageDeclarationTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.ParenthesizedExpressionTree;
import org.sonar.go.api.PlaceHolderTree;
import org.sonar.go.api.ReturnTree;
import org.sonar.go.api.StarExpressionTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.ThrowTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.UnaryExpressionTree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.CatchTreeImpl;
import org.sonar.go.impl.ClassDeclarationTreeImpl;
import org.sonar.go.impl.CompositeLiteralTreeImpl;
import org.sonar.go.impl.ExceptionHandlingTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.IfTreeImpl;
import org.sonar.go.impl.ImportDeclarationTreeImpl;
import org.sonar.go.impl.ImportSpecificationTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.JumpTreeImpl;
import org.sonar.go.impl.KeyValueTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
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
import org.sonar.go.impl.StarExpressionTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.ThrowTreeImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.UnaryExpressionTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;

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
import static org.sonar.go.persistence.conversion.JsonTreeConverter.DEFAULT_VALUE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELSE_BRANCH;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.ELSE_KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.EXPRESSION;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FINALLY_BLOCK;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FIRST_CPD_TOKEN;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FORMAL_PARAMETERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IDENTIFIER;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IDENTIFIERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IF_KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.INITIALIZERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.IS_VAL;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.KEYWORD;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.KIND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LABEL;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_HAND_SIDE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_OPERAND;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.LEFT_PARENTHESIS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.MODIFIERS;
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
  void assignment_expression() throws IOException {
    Token tokenIdentifier = otherToken(1, 0, "x");
    otherToken(1, 2, "=");
    Token tokenLiteral = otherToken(1, 4, "2");

    Tree identifier = new IdentifierTreeImpl(metaData(tokenIdentifier), tokenIdentifier.text());
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

    Tree identifierX = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
    Tree identifierY = new IdentifierTreeImpl(metaData(tokenY), tokenY.text());
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
    Tree identifierX = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    Tree identifierX = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
    Tree identifierY = new IdentifierTreeImpl(metaData(tokenY), tokenY.text());
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
    Tree identifierY = new IdentifierTreeImpl(metaData(tokenY), tokenY.text());
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
    IdentifierTree identifierA = new IdentifierTreeImpl(metaData(tokenA), tokenA.text());
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
      new IdentifierTreeImpl(metaData(tokenType), tokenType.text()),
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
    var keyValueTree = new KeyValueTreeImpl(metaData(key, value), new IdentifierTreeImpl(metaData(key), key.text()),
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
    Tree identifierX = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());

    Token tokenCatch = keywordToken(1, 20, "catch");
    Token tokenY = otherToken(1, 30, "y");
    Tree identifierY = new IdentifierTreeImpl(metaData(tokenY), tokenY.text());
    CatchTree catchTree = new CatchTreeImpl(metaData(tokenCatch, tokenY), null, identifierY, tokenCatch);

    keywordToken(1, 40, "finally");
    Token tokenZ = otherToken(1, 50, "z");
    Tree identifierZ = new IdentifierTreeImpl(metaData(tokenZ), tokenZ.text());

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
    Tree identifierX = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    IdentifierTree name = new IdentifierTreeImpl(metaData(tokenName), tokenName.text());
    Token tokenTypeParam = otherToken(1, 11, "T");
    NativeTree nativeTypeParameters = new NativeTreeImpl(metaData(tokenTypeParam), StringNativeKind.of("Field"), emptyList());
    Token tokenParam = otherToken(1, 15, "param");
    Tree param = new IdentifierTreeImpl(metaData(tokenParam), tokenParam.text());
    List<Tree> parameters = singletonList(param);
    Token tokenInt = otherToken(1, 20, "int");
    Tree returnType = new IdentifierTreeImpl(metaData(tokenInt), tokenInt.text());
    Token tokenOpen = otherToken(1, 23, "{");
    Token tokenClose = otherToken(1, 24, "}");
    BlockTree body = new BlockTreeImpl(metaData(tokenOpen, tokenClose), emptyList());
    TreeMetaData metaData = metaData(tokenR, tokenClose);
    FunctionDeclarationTree initialFunction = new FunctionDeclarationTreeImpl(metaData, returnType, nativeReceiver, name, parameters, nativeTypeParameters, body);
    FunctionDeclarationTree function = checkJsonSerializationDeserialization(initialFunction, "function_declaration.json");
    assertThat(function.textRange()).isEqualTo(metaData.textRange());
    assertThat(function.returnType().textRange()).isEqualTo(tokenInt.textRange());
    assertThat(function.name().textRange()).isEqualTo(tokenName.textRange());
    assertThat(function.formalParameters()).hasSize(1);
    assertThat(function.formalParameters().get(0).textRange()).isEqualTo(tokenParam.textRange());
    assertThat(function.body().textRange()).isEqualTo(body.textRange());

    assertThat(methodNames(FunctionDeclarationTree.class))
      .containsExactlyInAnyOrder(RETURN_TYPE, RECEIVER, NAME, FORMAL_PARAMETERS, TYPE_PARAMETERS, BODY, "rangeToHighlight", "receiverName");
  }

  @Test
  void identifier() throws IOException {
    Token token = otherToken(1, 0, "foo");
    IdentifierTree initialIdentifier = new IdentifierTreeImpl(metaData(token), token.text());
    IdentifierTree identifier = checkJsonSerializationDeserialization(initialIdentifier, "identifier.json");
    assertThat(identifier.name()).isEqualTo("foo");
    assertThat(identifier.identifier()).isEqualTo("foo");
    assertThat(identifier.textRange()).isEqualTo(token.textRange());

    assertThat(methodNames(IdentifierTree.class))
      .containsExactlyInAnyOrder(NAME, "identifier");
  }

  @Test
  void if_tree() throws IOException {
    Token tokenIf = keywordToken(1, 0, "if");
    Token tokenTrue = otherToken(1, 3, "true");
    Tree condition = new LiteralTreeImpl(metaData(tokenTrue), tokenTrue.text());

    Token tokenX = otherToken(1, 8, "x");
    Tree thenBranch = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());

    Token tokenElse = otherToken(1, 10, "else");
    Token tokenY = otherToken(1, 15, "y");
    Tree elseBranch = new IdentifierTreeImpl(metaData(tokenY), tokenY.text());

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
    Tree thenBranch = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());

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
    Tree lib = new IdentifierTreeImpl(metaData(tokenLib), tokenLib.text());
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
    IdentifierTree name = new IdentifierTreeImpl(metaData(tokenName), tokenName.text());
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
  void jump() throws IOException {
    Token tokenKeyword = keywordToken(1, 0, "break");
    Token tokenLabel = otherToken(1, 6, "hard");
    IdentifierTree label = new IdentifierTreeImpl(metaData(tokenLabel), tokenLabel.text());
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
    Tree body = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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

    Tree expression = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    Tree body = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    assertThat(tree.nativeKind()).hasToString("PARENT");
    assertThat(tree.children()).hasSize(2);
    assertThat(((NativeTree) tree.children().get(0)).nativeKind()).hasToString("CHILD");
    assertThat(tokens(tree.children().get(0))).isEqualTo("21:0:21:7 - token21");
    assertThat(tokens(tree.children().get(1))).isEqualTo("22:0:22:7 - token22");

    assertThat(methodNames(NativeTree.class))
      .containsExactlyInAnyOrder(NATIVE_KIND);
  }

  @Test
  void package_declaration() throws IOException {
    Token tokenPackage = keywordToken(1, 0, "package");
    Token tokenName = otherToken(1, 8, "hello");
    IdentifierTree name = new IdentifierTreeImpl(metaData(tokenName), tokenName.text());
    TreeMetaData metaData = metaData(tokenPackage, tokenName);
    PackageDeclarationTree initialTree = new PackageDeclarationTreeImpl(metaData, Collections.singletonList(name));
    PackageDeclarationTree tree = checkJsonSerializationDeserialization(initialTree, "package_declaration.json");
    assertThat(tokens(tree)).isEqualTo("1:0:1:13 - package hello");
    assertThat(tree.children()).hasSize(1);
    assertThat(tokens(tree.children().get(0))).isEqualTo("1:8:1:13 - hello");

    assertThat(methodNames(PackageDeclarationTree.class))
      .isEmpty();
  }

  @Test
  void parameter() throws IOException {
    Token tokenMod = otherToken(1, 0, "@Nullable");
    Token tokenX = otherToken(2, 0, "x");
    Token tokenInt = otherToken(2, 2, "int");
    Token tokenValue = otherToken(2, 6, "42");

    IdentifierTree modifier = new IdentifierTreeImpl(metaData(tokenMod), tokenMod.text());
    IdentifierTree identifier = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
    Tree type = new IdentifierTreeImpl(metaData(tokenInt), tokenInt.text());
    Tree defaultValue = new IntegerLiteralTreeImpl(metaData(tokenValue), tokenValue.text());

    TreeMetaData metaData = metaData(tokenMod, tokenValue);
    ParameterTree initialTree = new ParameterTreeImpl(metaData, identifier, type, defaultValue, Collections.singletonList(modifier));
    ParameterTree tree = checkJsonSerializationDeserialization(initialTree, "parameter.json");
    assertThat(tokens(tree)).isEqualTo("1:0:2:8 - @Nullable x int 42");
    assertThat(tokens(tree.identifier())).isEqualTo("2:0:2:1 - x");
    assertThat(tokens(tree.type())).isEqualTo("2:2:2:5 - int");
    assertThat(tokens(tree.defaultValue())).isEqualTo("2:6:2:8 - 42");
    assertThat(tree.modifiers()).hasSize(1);
    assertThat(tokens(tree.modifiers().get(0))).isEqualTo("1:0:1:9 - @Nullable");

    assertThat(methodNames(ParameterTree.class))
      .containsExactlyInAnyOrder(IDENTIFIER, TYPE, DEFAULT_VALUE, MODIFIERS);
  }

  @Test
  void parenthesized_expression() throws IOException {
    Token leftParenthesis = otherToken(1, 0, "(");
    Token tokenX = otherToken(1, 1, "x");
    Token rightParenthesis = otherToken(1, 2, ")");
    Tree expression = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
      .containsExactlyInAnyOrder(PLACE_HOLDER_TOKEN, IDENTIFIER, NAME);
  }

  @Test
  void return_true() throws IOException {
    Token returnToken = keywordToken(1, 0, "return");
    Token trueToken = otherToken(1, 7, "true");
    Token semicolonToken = otherToken(1, 11, ";");
    Tree trueLiteral = new LiteralTreeImpl(metaData(trueToken), trueToken.text());
    ReturnTree initialReturnTree = new ReturnTreeImpl(metaData(returnToken, semicolonToken), returnToken, trueLiteral);
    ReturnTree returnTree = checkJsonSerializationDeserialization(initialReturnTree, "return_true.json");
    assertThat(returnTree.keyword().text()).isEqualTo("return");
    assertThat(returnTree.body()).isInstanceOf(LiteralTree.class);
    TreeMetaData metaData = returnTree.metaData();
    assertThat(metaData.textRange()).isEqualTo(new TextRangeImpl(1, 0, 1, 12));
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.commentsInside()).isEmpty();
    assertThat(metaData.tokens()).hasSize(3);

    assertThat(methodNames(ReturnTree.class))
      .containsExactlyInAnyOrder(BODY, KEYWORD);
  }

  @Test
  void star_expression() throws IOException {
    Token tokenStar = otherToken(1, 0, "*");
    Token tokenString = otherToken(1, 1, "string");
    IdentifierTree identifierTree = new IdentifierTreeImpl(metaData(tokenString), tokenString.text());
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
    Tree body = new IdentifierTreeImpl(metaData(tokenEx), tokenEx.text());
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
      .containsExactlyInAnyOrder(DECLARATIONS, FIRST_CPD_TOKEN, "allComments", "doesImportType");
  }

  @Test
  void unary_expression() throws IOException {
    Token tokenMinus = otherToken(1, 0, "-");
    Token tokenX = otherToken(1, 1, "x");
    UnaryExpressionTree.Operator operator = UnaryExpressionTree.Operator.MINUS;
    Tree operand = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    Tree operand = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
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
    IdentifierTree identifier = new IdentifierTreeImpl(metaData(tokenX), tokenX.text());
    IdentifierTree type = new IdentifierTreeImpl(metaData(tokenInt), tokenInt.text());
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
        new IdentifierTreeImpl(metaData(tokenX), tokenX.text()),
        new IdentifierTreeImpl(metaData(tokenY), tokenY.text())),
      new IdentifierTreeImpl(metaData(tokenZ), tokenZ.text()));

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
    FunctionInvocationTree initialInvocation = new FunctionInvocationTreeImpl(metaData, new IdentifierTreeImpl(metaData(tokenBar),
      tokenBar.text()), singletonList(argument));
    FunctionInvocationTree invocation = checkJsonSerializationDeserialization(initialInvocation, "function_invocation.json");
    assertThat(invocation.memberSelect()).isInstanceOfSatisfying(IdentifierTree.class,
      identifier -> assertThat(identifier.name()).isEqualTo("bar"));
    assertThat(invocation.arguments()).hasSize(1);
    assertThat(((StringLiteralTree) invocation.arguments().get(0)).value()).isEqualTo("\"arg\"");
  }
}
