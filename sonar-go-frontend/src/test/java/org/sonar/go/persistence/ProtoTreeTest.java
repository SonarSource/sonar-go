/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.plugins.go.api.ArrayTypeTree;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.CompositeLiteralTree;
import org.sonar.plugins.go.api.EllipsisTree;
import org.sonar.plugins.go.api.ExceptionHandlingTree;
import org.sonar.plugins.go.api.ExpressionStatementTree;
import org.sonar.plugins.go.api.FieldListTree;
import org.sonar.plugins.go.api.FieldTree;
import org.sonar.plugins.go.api.FloatLiteralTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.GoStatementTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.ImaginaryLiteralTree;
import org.sonar.plugins.go.api.ImportDeclarationTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.IndexExpressionTree;
import org.sonar.plugins.go.api.IndexListExpressionTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.KeyValueTree;
import org.sonar.plugins.go.api.LeftRightHandSideTree;
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
import org.sonar.plugins.go.api.RangeClauseTree;
import org.sonar.plugins.go.api.ReturnTree;
import org.sonar.plugins.go.api.SliceTree;
import org.sonar.plugins.go.api.StarExpressionTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.ThrowTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeOrError;
import org.sonar.plugins.go.api.TypeAssertionExpressionTree;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonarsource.go.proto.SlangProto;
import org.sonarsource.go.proto.SlangProto.Node;
import org.sonarsource.go.proto.SlangProto.Node.KindCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The trees of the fixtures under sonar-go-to-slang/resources/ast are what the Go side really sends,
 * and GoConverterTest checks the round trip over them. This test builds the trees the schema allows
 * instead, so that every kind — including the ones Go has no counterpart for — is decoded here. That
 * every kind is decoded at all is a compile-time matter: the switch of ProtoTree has no default case.
 */
class ProtoTreeTest {

  private static final String RANGE = "1:0::3";
  private static final String SECOND_RANGE = "1:4::7";

  @Test
  void shouldDecodeAnIdentifier() {
    var tree = decode(identifierNode("foo"));

    assertThat(tree).isInstanceOfSatisfying(IdentifierTree.class, identifier -> {
      assertThat(identifier.name()).isEqualTo("foo");
      assertThat(identifier.type()).isEqualTo("int");
      assertThat(identifier.packageName()).isEqualTo("main");
      assertThat(identifier.id()).isEqualTo(42);
      assertThat(identifier.textRange()).isEqualTo(identifier.metaData().textRange());
    });
  }

  @Test
  void shouldDecodeANativeTree() {
    var tree = decode(node(KindCase.NATIVE, SlangProto.Native.newBuilder()
      .setNativeKind("Lparen")
      .addChildren(identifierNode("foo").setTextRange(SECOND_RANGE))));

    assertThat(tree).isInstanceOfSatisfying(NativeTree.class, nativeTree -> {
      assertThat(StringNativeKind.toString(nativeTree.nativeKind())).isEqualTo("Lparen");
      assertThat(nativeTree.children()).hasSize(1);
    });
  }

  @Test
  void shouldDecodeTheLiterals() {
    assertThat(decode(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("1"))))
      .isInstanceOfSatisfying(IntegerLiteralTree.class, literal -> assertThat(literal.value()).isEqualTo("1"));
    assertThat(decode(node(KindCase.FLOAT_LITERAL, SlangProto.FloatLiteral.newBuilder().setValue("1.5"))))
      .isInstanceOfSatisfying(FloatLiteralTree.class, literal -> assertThat(literal.value()).isEqualTo("1.5"));
    assertThat(decode(node(KindCase.IMAGINARY_LITERAL, SlangProto.ImaginaryLiteral.newBuilder().setValue("1i"))))
      .isInstanceOfSatisfying(ImaginaryLiteralTree.class, literal -> assertThat(literal.value()).isEqualTo("1i"));
    assertThat(decode(node(KindCase.LITERAL, SlangProto.Literal.newBuilder().setValue("nil"))))
      .isInstanceOfSatisfying(LiteralTree.class, literal -> assertThat(literal.value()).isEqualTo("nil"));
    assertThat(decode(node(KindCase.STRING_LITERAL, SlangProto.StringLiteral.newBuilder().setValue("\"a\"").setContent("a"))))
      .isInstanceOfSatisfying(StringLiteralTree.class, literal -> {
        assertThat(literal.value()).isEqualTo("\"a\"");
        assertThat(literal.content()).isEqualTo("a");
      });
  }

  @Test
  void shouldDecodeAVariableDeclaration() {
    var tree = decode(node(KindCase.VARIABLE_DECLARATION, SlangProto.VariableDeclaration.newBuilder()
      .addIdentifiers(identifierNode("foo"))
      .setType(identifierNode("int").setTextRange(SECOND_RANGE))
      .addInitializers(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("1")))
      .setIsVal(true)));

    assertThat(tree).isInstanceOfSatisfying(VariableDeclarationTree.class, declaration -> {
      assertThat(declaration.identifiers()).hasSize(1);
      assertThat(declaration.type()).isInstanceOf(IdentifierTree.class);
      assertThat(declaration.initializers()).hasSize(1);
      assertThat(declaration.isVal()).isTrue();
    });
  }

  @Test
  void shouldDecodeAVariableDeclarationWithoutAType() {
    var tree = decode(node(KindCase.VARIABLE_DECLARATION, SlangProto.VariableDeclaration.newBuilder()
      .addIdentifiers(identifierNode("foo"))));

    assertThat(tree).isInstanceOfSatisfying(VariableDeclarationTree.class, declaration -> {
      assertThat(declaration.type()).isNull();
      assertThat(declaration.initializers()).isEmpty();
      assertThat(declaration.isVal()).isFalse();
    });
  }

  @Test
  void shouldDecodeTheStatementContainers() {
    assertThat(decode(node(KindCase.BLOCK, SlangProto.Block.newBuilder().addStatementOrExpressions(identifierNode("foo")))))
      .isInstanceOfSatisfying(BlockTree.class, block -> assertThat(block.statementOrExpressions()).hasSize(1));
    assertThat(decode(node(KindCase.EXPRESSION_STATEMENT, SlangProto.ExpressionStatement.newBuilder().setExpression(identifierNode("foo")))))
      .isInstanceOfSatisfying(ExpressionStatementTree.class, statement -> assertThat(statement.expression()).isInstanceOf(IdentifierTree.class));
    assertThat(decode(node(KindCase.PACKAGE_DECLARATION, SlangProto.PackageDeclaration.newBuilder().addChildren(identifierNode("foo")))))
      .isInstanceOfSatisfying(PackageDeclarationTree.class, declaration -> assertThat(declaration.children()).hasSize(1));
    assertThat(decode(node(KindCase.IMPORT_DECLARATION, SlangProto.ImportDeclaration.newBuilder().addChildren(identifierNode("foo")))))
      .isInstanceOfSatisfying(ImportDeclarationTree.class, declaration -> assertThat(declaration.children()).hasSize(1));
    assertThat(decode(node(KindCase.LEFT_RIGHT_HAND_SIDE, SlangProto.LeftRightHandSide.newBuilder().addChildren(identifierNode("foo")))))
      .isInstanceOfSatisfying(LeftRightHandSideTree.class, handSide -> assertThat(handSide.children()).hasSize(1));
  }

  @Test
  void shouldDecodeATopLevelWithItsComments() {
    var comment = SlangProto.Comment.newBuilder().setText("// a").setContentText(" a").setRange(SECOND_RANGE).setContentRange(SECOND_RANGE);
    var protoTree = SlangProto.Tree.newBuilder()
      .addComments(comment)
      .addTokens(otherToken("foo", RANGE))
      .setRoot(node(KindCase.TOP_LEVEL, SlangProto.TopLevel.newBuilder()
        .addDeclarations(identifierNode("foo"))
        .setFirstCpdToken(RANGE))
          .setTextRange("1:0:1:7"));

    assertThat(decode(protoTree)).isInstanceOfSatisfying(TopLevelTree.class, topLevel -> {
      assertThat(topLevel.declarations()).hasSize(1);
      assertThat(topLevel.firstCpdToken().text()).isEqualTo("foo");
      assertThat(topLevel.allComments()).hasSize(1);
    });
  }

  @Test
  void shouldDecodeATopLevelWithoutAFirstCpdToken() {
    var tree = decode(node(KindCase.TOP_LEVEL, SlangProto.TopLevel.newBuilder().addDeclarations(identifierNode("foo"))));

    assertThat(tree).isInstanceOfSatisfying(TopLevelTree.class, topLevel -> assertThat(topLevel.firstCpdToken()).isNull());
  }

  @Test
  void shouldDecodeTheExpressions() {
    assertThat(decode(node(KindCase.BINARY_EXPRESSION, SlangProto.BinaryExpression.newBuilder()
      .setOperator("PLUS")
      .setOperatorToken(RANGE)
      .setLeftOperand(identifierNode("foo"))
      .setRightOperand(identifierNode("bar").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(BinaryExpressionTree.class, expression -> {
          assertThat(expression.operator()).isEqualTo(BinaryExpressionTree.Operator.PLUS);
          assertThat(expression.operatorToken().text()).isEqualTo("foo");
        });

    assertThat(decode(node(KindCase.UNARY_EXPRESSION, SlangProto.UnaryExpression.newBuilder()
      .setOperator("NEGATE")
      .setOperand(identifierNode("foo")))))
        .isInstanceOfSatisfying(UnaryExpressionTree.class, expression -> assertThat(expression.operator())
          .isEqualTo(UnaryExpressionTree.Operator.NEGATE));

    assertThat(decode(node(KindCase.ASSIGNMENT_EXPRESSION, SlangProto.AssignmentExpression.newBuilder()
      .setOperator("EQUAL")
      .setLeftHandSide(identifierNode("foo"))
      .setStatementOrExpression(identifierNode("bar").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(AssignmentExpressionTree.class, expression -> assertThat(expression.operator())
          .isEqualTo(AssignmentExpressionTree.Operator.EQUAL));

    assertThat(decode(node(KindCase.STAR_EXPRESSION, SlangProto.StarExpression.newBuilder().setExpression(identifierNode("foo")))))
      .isInstanceOfSatisfying(StarExpressionTree.class, expression -> assertThat(expression.operand()).isInstanceOf(IdentifierTree.class));

    assertThat(decode(node(KindCase.PARENTHESIZED_EXPRESSION, SlangProto.ParenthesizedExpression.newBuilder()
      .setExpression(identifierNode("foo"))
      .setLeftParenthesis(RANGE)
      .setRightParenthesis(RANGE))))
        .isInstanceOfSatisfying(ParenthesizedExpressionTree.class, expression -> assertThat(expression.leftParenthesis().text()).isEqualTo("foo"));
  }

  @Test
  void shouldDecodeTheTypeExpressions() {
    assertThat(decode(node(KindCase.ARRAY_TYPE, SlangProto.ArrayType.newBuilder()
      .setElement(identifierNode("int"))
      .setLength(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("2"))))))
        .isInstanceOfSatisfying(ArrayTypeTree.class, arrayType -> {
          assertThat(arrayType.element()).isInstanceOf(IdentifierTree.class);
          assertThat(arrayType.length()).isInstanceOf(IntegerLiteralTree.class);
        });

    assertThat(decode(node(KindCase.ARRAY_TYPE, SlangProto.ArrayType.newBuilder().setElement(identifierNode("int")))))
      .isInstanceOfSatisfying(ArrayTypeTree.class, arrayType -> assertThat(arrayType.length()).isNull());

    assertThat(decode(node(KindCase.MAP_TYPE, SlangProto.MapType.newBuilder()
      .setKey(identifierNode("string"))
      .setValue(identifierNode("int").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(MapTypeTree.class, mapType -> assertThat(mapType.key()).isInstanceOf(IdentifierTree.class));

    assertThat(decode(node(KindCase.ELLIPSIS, SlangProto.Ellipsis.newBuilder()
      .setEllipsis(RANGE)
      .setElement(identifierNode("int").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(EllipsisTree.class, ellipsis -> {
          assertThat(ellipsis.ellipsis().text()).isEqualTo("foo");
          assertThat(ellipsis.element()).isInstanceOf(IdentifierTree.class);
        });

    assertThat(decode(node(KindCase.ELLIPSIS, SlangProto.Ellipsis.newBuilder().setEllipsis(RANGE))))
      .isInstanceOfSatisfying(EllipsisTree.class, ellipsis -> assertThat(ellipsis.element()).isNull());

    assertThat(decode(node(KindCase.TYPE_ASSERTION_EXPRESSION, SlangProto.TypeAssertionExpression.newBuilder()
      .setExpression(identifierNode("foo"))
      .setType(identifierNode("int").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(TypeAssertionExpressionTree.class, assertion -> assertThat(assertion.type()).isInstanceOf(IdentifierTree.class));

    assertThat(decode(node(KindCase.TYPE_ASSERTION_EXPRESSION, SlangProto.TypeAssertionExpression.newBuilder()
      .setExpression(identifierNode("foo")))))
        .isInstanceOfSatisfying(TypeAssertionExpressionTree.class, assertion -> assertThat(assertion.type()).isNull());
  }

  @Test
  void shouldDecodeTheIndexingExpressions() {
    assertThat(decode(node(KindCase.INDEX_EXPRESSION, SlangProto.IndexExpression.newBuilder()
      .setExpression(identifierNode("foo"))
      .setIndex(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("0"))))))
        .isInstanceOfSatisfying(IndexExpressionTree.class, expression -> assertThat(expression.index()).isInstanceOf(IntegerLiteralTree.class));

    assertThat(decode(node(KindCase.INDEX_LIST_EXPRESSION, SlangProto.IndexListExpression.newBuilder()
      .setExpression(identifierNode("foo"))
      .addIndices(identifierNode("int").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(IndexListExpressionTree.class, expression -> assertThat(expression.indices()).hasSize(1));

    assertThat(decode(node(KindCase.SLICE, SlangProto.Slice.newBuilder()
      .setExpression(identifierNode("foo"))
      .setLow(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("0")))
      .setHigh(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("1")))
      .setMax(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("2")))
      .setSlice3(true))))
        .isInstanceOfSatisfying(SliceTree.class, slice -> {
          assertThat(slice.low()).isInstanceOf(IntegerLiteralTree.class);
          assertThat(slice.high()).isInstanceOf(IntegerLiteralTree.class);
          assertThat(slice.max()).isInstanceOf(IntegerLiteralTree.class);
          assertThat(slice.slice3()).isTrue();
        });

    assertThat(decode(node(KindCase.SLICE, SlangProto.Slice.newBuilder().setExpression(identifierNode("foo")))))
      .isInstanceOfSatisfying(SliceTree.class, slice -> {
        assertThat(slice.low()).isNull();
        assertThat(slice.high()).isNull();
        assertThat(slice.max()).isNull();
        assertThat(slice.slice3()).isFalse();
      });
  }

  @Test
  void shouldDecodeAFunctionInvocation() {
    var tree = decode(node(KindCase.FUNCTION_INVOCATION, SlangProto.FunctionInvocation.newBuilder()
      .setMemberSelect(node(KindCase.MEMBER_SELECT, SlangProto.MemberSelect.newBuilder()
        .setExpression(identifierNode("fmt"))
        .setIdentifier(identifierNode("Println").setTextRange(SECOND_RANGE))))
      .addArguments(identifierNode("foo"))
      .addReturnType("int")));

    assertThat(tree).isInstanceOfSatisfying(FunctionInvocationTree.class, invocation -> {
      assertThat(invocation.memberSelect()).isInstanceOf(MemberSelectTree.class);
      assertThat(invocation.arguments()).hasSize(1);
      assertThat(invocation.returnTypes()).hasSize(1);
      assertThat(invocation.returnTypes().get(0).type()).isEqualTo("int");
    });
  }

  @Test
  void shouldDecodeAGoStatement() {
    var invocation = node(KindCase.FUNCTION_INVOCATION, SlangProto.FunctionInvocation.newBuilder()
      .setMemberSelect(identifierNode("run")));
    var tree = decode(node(KindCase.GO_STATEMENT, SlangProto.GoStatement.newBuilder()
      .setGoToken(RANGE)
      .setFunctionInvocation(invocation)));

    assertThat(tree).isInstanceOfSatisfying(GoStatementTree.class, goStatement -> {
      assertThat(goStatement.goToken().text()).isEqualTo("foo");
      assertThat(goStatement.functionInvocation()).isInstanceOf(FunctionInvocationTree.class);
    });
  }

  @Test
  void shouldDecodeAFunctionDeclarationWithAllItsParts() {
    var tree = decode(node(KindCase.FUNCTION_DECLARATION, SlangProto.FunctionDeclaration.newBuilder()
      .setName(identifierNode("main"))
      .setReceiver(fieldListNode("receiver"))
      .setReturnType(fieldListNode("int"))
      .setTypeParameters(fieldListNode("T"))
      .addFormalParameters(node(KindCase.PARAMETER, SlangProto.Parameter.newBuilder()
        .setIdentifier(identifierNode("arg").setTextRange(SECOND_RANGE))
        .setType(identifierNode("int").setTextRange(SECOND_RANGE))))
      .setBody(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))));

    assertThat(tree).isInstanceOfSatisfying(FunctionDeclarationTree.class, function -> {
      assertThat(function.name().name()).isEqualTo("main");
      assertThat(function.receiver()).isInstanceOf(FieldListTree.class);
      assertThat(function.returnType()).isInstanceOf(FieldListTree.class);
      assertThat(function.typeParameters()).isInstanceOf(FieldListTree.class);
      assertThat(function.formalParameters()).hasSize(1);
      assertThat(function.formalParameters().get(0)).isInstanceOfSatisfying(ParameterTree.class,
        parameter -> assertThat(parameter.typeTree()).isInstanceOf(IdentifierTree.class));
      assertThat(function.body()).isInstanceOf(BlockTree.class);
      assertThat(function.cfg()).isNull();
    });
  }

  @Test
  void shouldDecodeAFunctionDeclarationWithoutAnyOptionalPart() {
    var tree = decode(node(KindCase.FUNCTION_DECLARATION, SlangProto.FunctionDeclaration.newBuilder()));

    assertThat(tree).isInstanceOfSatisfying(FunctionDeclarationTree.class, function -> {
      assertThat(function.name()).isNull();
      assertThat(function.receiver()).isNull();
      assertThat(function.returnType()).isNull();
      assertThat(function.typeParameters()).isNull();
      assertThat(function.body()).isNull();
      assertThat(function.cfg()).isNull();
    });
  }

  @Test
  void shouldRebuildTheControlFlowGraphOfAFunctionFromTheIdsOfItsOwnNodes() {
    var first = identifierNode("foo").setCfgId(1);
    var second = identifierNode("bar").setTextRange(SECOND_RANGE).setCfgId(2);
    var tree = decode(node(KindCase.FUNCTION_DECLARATION, SlangProto.FunctionDeclaration.newBuilder()
      .setBody(node(KindCase.BLOCK, SlangProto.Block.newBuilder().addStatementOrExpressions(first).addStatementOrExpressions(second)))
      .setCfg(SlangProto.Cfg.newBuilder()
        // The third id belongs to no node of the function, as the ids of a graph built by Go may.
        .addBlocks(SlangProto.CfgBlock.newBuilder().addNodeIds(1).addNodeIds(3).addSuccessors(1))
        .addBlocks(SlangProto.CfgBlock.newBuilder().addNodeIds(2)))));

    var cfg = ((FunctionDeclarationTree) tree).cfg();
    assertThat(cfg).isNotNull();
    assertThat(cfg.blocks()).hasSize(2);
    assertThat(cfg.entryBlock()).isSameAs(cfg.blocks().get(0));
    assertThat(cfg.entryBlock().nodes()).hasSize(1);
    assertThat(cfg.entryBlock().nodes().get(0)).isInstanceOfSatisfying(IdentifierTree.class,
      identifier -> assertThat(identifier.name()).isEqualTo("foo"));
    assertThat(cfg.entryBlock().successors()).containsExactly(cfg.blocks().get(1));
    assertThat(cfg.blocks().get(1).successors()).isEmpty();
  }

  @Test
  void shouldIgnoreAnEmptyControlFlowGraph() {
    var tree = decode(node(KindCase.FUNCTION_DECLARATION, SlangProto.FunctionDeclaration.newBuilder()
      .setCfg(SlangProto.Cfg.newBuilder())));

    assertThat(((FunctionDeclarationTree) tree).cfg()).isNull();
  }

  @Test
  void shouldDecodeTheIfStatements() {
    assertThat(decode(node(KindCase.IF_STATEMENT, SlangProto.If.newBuilder()
      .setCondition(identifierNode("foo"))
      .setThenBranch(identifierNode("bar").setTextRange(SECOND_RANGE))
      .setElseBranch(identifierNode("baz").setTextRange(SECOND_RANGE))
      .setIfKeyword(RANGE)
      .setElseKeyword(SECOND_RANGE))))
        .isInstanceOfSatisfying(IfTree.class, ifTree -> {
          assertThat(ifTree.ifKeyword().text()).isEqualTo("foo");
          assertThat(ifTree.elseKeyword().text()).isEqualTo("bar");
          assertThat(ifTree.elseBranch()).isInstanceOf(IdentifierTree.class);
        });

    assertThat(decode(node(KindCase.IF_STATEMENT, SlangProto.If.newBuilder()
      .setCondition(identifierNode("foo"))
      .setThenBranch(identifierNode("bar").setTextRange(SECOND_RANGE))
      .setIfKeyword(RANGE))))
        .isInstanceOfSatisfying(IfTree.class, ifTree -> {
          assertThat(ifTree.elseKeyword()).isNull();
          assertThat(ifTree.elseBranch()).isNull();
        });
  }

  @Test
  void shouldDecodeTheLoops() {
    assertThat(decode(node(KindCase.LOOP, SlangProto.Loop.newBuilder()
      .setCondition(identifierNode("foo"))
      .setBody(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setKind("FOR")
      .setKeyword(RANGE))))
        .isInstanceOfSatisfying(LoopTree.class, loop -> {
          assertThat(loop.kind()).isEqualTo(LoopTree.LoopKind.FOR);
          assertThat(loop.condition()).isInstanceOf(IdentifierTree.class);
        });

    assertThat(decode(node(KindCase.LOOP, SlangProto.Loop.newBuilder()
      .setBody(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setKind("FOR")
      .setKeyword(RANGE))))
        .isInstanceOfSatisfying(LoopTree.class, loop -> assertThat(loop.condition()).isNull());
  }

  @Test
  void shouldDecodeTheJumps() {
    assertThat(decode(node(KindCase.JUMP, SlangProto.Jump.newBuilder()
      .setKeyword(RANGE)
      .setKind("BREAK")
      .setLabel(identifierNode("label").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(JumpTree.class, jump -> {
          assertThat(jump.kind()).isEqualTo(JumpTree.JumpKind.BREAK);
          assertThat(jump.label().name()).isEqualTo("label");
        });

    assertThat(decode(node(KindCase.JUMP, SlangProto.Jump.newBuilder().setKeyword(RANGE).setKind("CONTINUE"))))
      .isInstanceOfSatisfying(JumpTree.class, jump -> assertThat(jump.label()).isNull());
  }

  @Test
  void shouldDecodeAReturnStatement() {
    assertThat(decode(node(KindCase.RETURN_STATEMENT, SlangProto.Return.newBuilder()
      .setKeyword(RANGE)
      .addExpressions(identifierNode("foo")))))
        .isInstanceOfSatisfying(ReturnTree.class, returnTree -> {
          assertThat(returnTree.keyword().text()).isEqualTo("foo");
          assertThat(returnTree.expressions()).hasSize(1);
        });
  }

  @Test
  void shouldDecodeAMatch() {
    var matchCase = node(KindCase.MATCH_CASE, SlangProto.MatchCase.newBuilder()
      .setExpression(identifierNode("foo"))
      .setBody(node(KindCase.BLOCK, SlangProto.Block.newBuilder())));
    var tree = decode(node(KindCase.MATCH, SlangProto.Match.newBuilder()
      .setExpression(identifierNode("bar"))
      .addCases(matchCase)
      .setKeyword(RANGE)));

    assertThat(tree).isInstanceOfSatisfying(MatchTree.class, match -> {
      assertThat(match.keyword().text()).isEqualTo("foo");
      assertThat(match.cases()).hasSize(1);
      assertThat(match.cases().get(0)).isInstanceOfSatisfying(MatchCaseTree.class,
        aCase -> assertThat(aCase.body()).isInstanceOf(BlockTree.class));
    });

    assertThat(decode(node(KindCase.MATCH, SlangProto.Match.newBuilder().setKeyword(RANGE))))
      .isInstanceOfSatisfying(MatchTree.class, match -> assertThat(match.expression()).isNull());
    assertThat(decode(node(KindCase.MATCH_CASE, SlangProto.MatchCase.newBuilder())))
      .isInstanceOfSatisfying(MatchCaseTree.class, aCase -> {
        assertThat(aCase.expression()).isNull();
        assertThat(aCase.body()).isNull();
      });
  }

  @Test
  void shouldDecodeTheCompositeLiterals() {
    assertThat(decode(node(KindCase.COMPOSITE_LITERAL, SlangProto.CompositeLiteral.newBuilder()
      .setType(identifierNode("Point"))
      .addElements(node(KindCase.KEY_VALUE, SlangProto.KeyValue.newBuilder()
        .setKey(identifierNode("x").setTextRange(SECOND_RANGE))
        .setValue(node(KindCase.INTEGER_LITERAL, SlangProto.IntegerLiteral.newBuilder().setValue("1"))))))))
          .isInstanceOfSatisfying(CompositeLiteralTree.class, literal -> {
            assertThat(literal.type()).isInstanceOf(IdentifierTree.class);
            assertThat(literal.elements()).hasSize(1);
            assertThat(literal.elements().get(0)).isInstanceOfSatisfying(KeyValueTree.class,
              keyValue -> assertThat(keyValue.key()).isInstanceOf(IdentifierTree.class));
          });

    assertThat(decode(node(KindCase.COMPOSITE_LITERAL, SlangProto.CompositeLiteral.newBuilder())))
      .isInstanceOfSatisfying(CompositeLiteralTree.class, literal -> assertThat(literal.type()).isNull());
  }

  @Test
  void shouldDecodeAnImportSpecification() {
    assertThat(decode(node(KindCase.IMPORT_SPECIFICATION, SlangProto.ImportSpecification.newBuilder()
      .setName(identifierNode("alias"))
      .setPath(node(KindCase.STRING_LITERAL, SlangProto.StringLiteral.newBuilder().setValue("\"fmt\"").setContent("fmt"))))))
        .isInstanceOfSatisfying(ImportSpecificationTree.class, specification -> {
          assertThat(specification.name().name()).isEqualTo("alias");
          assertThat(specification.path().content()).isEqualTo("fmt");
        });

    assertThat(decode(node(KindCase.IMPORT_SPECIFICATION, SlangProto.ImportSpecification.newBuilder()
      .setPath(node(KindCase.STRING_LITERAL, SlangProto.StringLiteral.newBuilder().setValue("\"fmt\"").setContent("fmt"))))))
        .isInstanceOfSatisfying(ImportSpecificationTree.class, specification -> assertThat(specification.name()).isNull());
  }

  @Test
  void shouldResolveTheIdentifierOfAClassDeclarationAmongItsOwnDescendants() {
    var classTree = node(KindCase.NATIVE, SlangProto.Native.newBuilder()
      .addChildren(identifierNode("Point").setTextRange(SECOND_RANGE)));
    var tree = decode(node(KindCase.CLASS_DECLARATION, SlangProto.ClassDeclaration.newBuilder()
      .setIdentifier(SECOND_RANGE)
      .setClassTree(classTree)));

    assertThat(tree).isInstanceOfSatisfying(ClassDeclarationTree.class, declaration -> {
      assertThat(declaration.identifier().name()).isEqualTo("Point");
      assertThat(declaration.classTree()).isInstanceOf(NativeTree.class);
    });
  }

  @Test
  void shouldDecodeAnAnonymousClassDeclaration() {
    var tree = decode(node(KindCase.CLASS_DECLARATION, SlangProto.ClassDeclaration.newBuilder()
      .setClassTree(node(KindCase.NATIVE, SlangProto.Native.newBuilder()))));

    assertThat(tree).isInstanceOfSatisfying(ClassDeclarationTree.class, declaration -> assertThat(declaration.identifier()).isNull());
  }

  @Test
  void shouldDecodeRangeClauseWithAndWithoutNames() {
    var clause = node(KindCase.RANGE_CLAUSE, SlangProto.RangeClause.newBuilder()
      .setKey(identifierNode("key"))
      .setValue(identifierNode("value"))
      .setRangedExpression(identifierNode("items"))
      .setIsDeclaration(true));
    assertThat(decode(clause)).isInstanceOfSatisfying(RangeClauseTree.class, range -> {
      assertThat(range.key()).isInstanceOf(IdentifierTree.class);
      assertThat(range.value()).isInstanceOf(IdentifierTree.class);
      assertThat(range.isDeclaration()).isTrue();
    });
    assertThat(decode(node(KindCase.RANGE_CLAUSE, SlangProto.RangeClause.newBuilder()
      .setRangedExpression(identifierNode("items")))))
        .isInstanceOfSatisfying(RangeClauseTree.class, range -> {
          assertThat(range.key()).isNull();
          assertThat(range.value()).isNull();
        });
  }

  @Test
  void shouldDecodeFieldListAndFields() {
    assertThat(decode(fieldListNode("T"))).isInstanceOfSatisfying(FieldListTree.class, fields -> {
      assertThat(fields.fields()).hasSize(1);
      assertThat(fields.fields().get(0)).isInstanceOf(FieldTree.class);
    });
  }

  @Test
  void shouldDecodeAPlaceHolder() {
    var tree = decode(node(KindCase.PLACE_HOLDER, SlangProto.PlaceHolder.newBuilder().setPlaceHolderToken(RANGE)));

    assertThat(tree).isInstanceOfSatisfying(PlaceHolderTree.class, placeHolder -> assertThat(placeHolder.placeHolderToken().text()).isEqualTo("foo"));
  }

  /**
   * Go has no counterpart for these, so only this test covers them; they are part of the schema
   * because the Tree interfaces the analyzer shares with the other analyzers have them.
   */
  @Test
  void shouldDecodeTheKindsWithoutAGoCounterpart() {
    assertThat(decode(node(KindCase.MODIFIER, SlangProto.Modifier.newBuilder().setKind("PUBLIC"))))
      .isInstanceOfSatisfying(ModifierTree.class, modifier -> assertThat(modifier.kind()).isEqualTo(ModifierTree.Kind.PUBLIC));

    assertThat(decode(node(KindCase.THROW_STATEMENT, SlangProto.Throw.newBuilder()
      .setKeyword(RANGE)
      .setBody(identifierNode("err").setTextRange(SECOND_RANGE)))))
        .isInstanceOfSatisfying(ThrowTree.class, throwTree -> {
          assertThat(throwTree.keyword().text()).isEqualTo("foo");
          assertThat(throwTree.body()).isInstanceOf(IdentifierTree.class);
        });
    assertThat(decode(node(KindCase.THROW_STATEMENT, SlangProto.Throw.newBuilder().setKeyword(RANGE))))
      .isInstanceOfSatisfying(ThrowTree.class, throwTree -> assertThat(throwTree.body()).isNull());

    var catchClause = node(KindCase.CATCH_CLAUSE, SlangProto.Catch.newBuilder()
      .setCatchParameter(identifierNode("err").setTextRange(SECOND_RANGE))
      .setCatchBlock(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setKeyword(RANGE));
    assertThat(decode(catchClause)).isInstanceOfSatisfying(CatchTree.class, aCatch -> {
      assertThat(aCatch.catchParameter()).isInstanceOf(IdentifierTree.class);
      assertThat(aCatch.catchBlock()).isInstanceOf(BlockTree.class);
    });
    assertThat(decode(node(KindCase.CATCH_CLAUSE, SlangProto.Catch.newBuilder()
      .setCatchBlock(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setKeyword(RANGE))))
        .isInstanceOfSatisfying(CatchTree.class, aCatch -> assertThat(aCatch.catchParameter()).isNull());

    assertThat(decode(node(KindCase.EXCEPTION_HANDLING, SlangProto.ExceptionHandling.newBuilder()
      .setTryBlock(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setTryKeyword(RANGE)
      .addCatchBlocks(catchClause)
      .setFinallyBlock(node(KindCase.BLOCK, SlangProto.Block.newBuilder())))))
        .isInstanceOfSatisfying(ExceptionHandlingTree.class, handling -> {
          assertThat(handling.catchBlocks()).hasSize(1);
          assertThat(handling.finallyBlock()).isInstanceOf(BlockTree.class);
        });
    assertThat(decode(node(KindCase.EXCEPTION_HANDLING, SlangProto.ExceptionHandling.newBuilder()
      .setTryBlock(node(KindCase.BLOCK, SlangProto.Block.newBuilder()))
      .setTryKeyword(RANGE))))
        .isInstanceOfSatisfying(ExceptionHandlingTree.class, handling -> assertThat(handling.finallyBlock()).isNull());
  }

  @Test
  void shouldReportTheParseErrorOfAFileInsteadOfItsTree() {
    var protoTree = SlangProto.Tree.newBuilder().setError("main.go:1:1: expected 'package', found xpackage");

    var result = ProtoTree.fromProto(frame("main.go", protoTree.build())).get("main.go");

    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isEqualTo("main.go:1:1: expected 'package', found xpackage");
  }

  @Test
  void shouldFailOnlyTheFileWhoseTreeCannotBeBuilt() {
    var unknownOperator = SlangProto.Tree.newBuilder()
      .addTokens(otherToken("foo", RANGE))
      .setRoot(node(KindCase.UNARY_EXPRESSION, SlangProto.UnaryExpression.newBuilder()
        .setOperator("NOT_AN_OPERATOR")
        .setOperand(identifierNode("foo"))));
    var response = new ByteArrayOutputStream();
    response.writeBytes(frame("broken.go", unknownOperator.build()));
    response.writeBytes(frame("main.go", treeOf(identifierNode("foo"))));

    Map<String, TreeOrError> trees = ProtoTree.fromProto(response.toByteArray());

    assertThat(trees).hasSize(2);
    assertThat(trees.get("broken.go").error()).startsWith("Error converting proto tree: No enum constant");
    assertThat(trees.get("main.go").isError()).isFalse();
  }

  @Test
  void shouldFailTheFileWhoseNodeHasNoKind() {
    var noKind = treeOf(Node.newBuilder().setTextRange(RANGE));

    var result = ProtoTree.fromProto(frame("main.go", noKind)).get("main.go");

    assertThat(result.error()).isEqualTo("Error converting proto tree: Node of no kind at " + RANGE);
  }

  @Test
  void shouldFailTheFileWhoseTreeIsNotReadable() throws IOException {
    var response = new ByteArrayOutputStream();
    response.writeBytes(frame(fileWithAnUnreadableTree("broken.go")));
    response.writeBytes(frame("main.go", treeOf(identifierNode("foo"))));

    Map<String, TreeOrError> trees = ProtoTree.fromProto(response.toByteArray());

    assertThat(trees).hasSize(2);
    assertThat(trees.get("broken.go").error()).startsWith("Error reading proto tree: ");
    assertThat(trees.get("main.go").isError()).isFalse();
  }

  @Test
  void shouldRejectAResponseWhoseFramingIsBroken() {
    assertThatThrownBy(() -> ProtoTree.fromProto(new byte[] {1, 0}))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Truncated response from the Go executable: no frame length at offset 0");

    var oneFrameAndAStrayByte = new ByteArrayOutputStream();
    oneFrameAndAStrayByte.writeBytes(frame("main.go", treeOf(identifierNode("foo"))));
    oneFrameAndAStrayByte.write(0);
    var oneFrameAndAStrayByteBytes = oneFrameAndAStrayByte.toByteArray();
    assertThatThrownBy(() -> ProtoTree.fromProto(oneFrameAndAStrayByteBytes))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Truncated response from the Go executable: no frame length at offset " + (oneFrameAndAStrayByte.size() - 1));

    var negativeLength = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(-1).putInt(0).array();
    assertThatThrownBy(() -> ProtoTree.fromProto(negativeLength))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid frame length -1 at offset 0 of the response from the Go executable");

    var lengthThatWouldOverflow = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(Integer.MAX_VALUE).putInt(0).array();
    assertThatThrownBy(() -> ProtoTree.fromProto(lengthThatWouldOverflow))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid frame length " + Integer.MAX_VALUE + " at offset 0 of the response from the Go executable");
  }

  @Test
  void shouldDecodeATreeNestedDeeperThanProtobufAllowsByDefault() {
    var depth = 400;
    var nested = identifierNode("foo");
    for (var i = 0; i < depth; i++) {
      nested = node(KindCase.PARENTHESIZED_EXPRESSION, SlangProto.ParenthesizedExpression.newBuilder()
        .setExpression(nested)
        .setLeftParenthesis(RANGE)
        .setRightParenthesis(RANGE));
    }

    Tree tree = decode(nested);

    for (var i = 0; i < depth; i++) {
      assertThat(tree).isInstanceOf(ParenthesizedExpressionTree.class);
      tree = ((ParenthesizedExpressionTree) tree).expression();
    }
    assertThat(tree).isInstanceOf(IdentifierTree.class);
  }

  // ─── Building the trees a response is made of ────────────────────────────

  /** All the nodes of these trees are at one of the two ranges the single token of the file covers. */
  private static Node.Builder identifierNode(String name) {
    return node(KindCase.IDENTIFIER, SlangProto.Identifier.newBuilder().setName(name).setType("int").setPackage("main").setId(42));
  }

  /**
   * Builds the node of a kind, with the message of that kind as its payload. The kind and the message
   * are set through the descriptor of the field the oneof case names, so that a mismatch between them
   * fails here rather than producing a node of another kind.
   */
  private static Node.Builder node(KindCase kind, Message.Builder payload) {
    var field = Node.getDescriptor().findFieldByNumber(kind.getNumber());
    return Node.newBuilder().setTextRange(RANGE).setField(field, payload.build());
  }

  private static Node.Builder fieldListNode(String name) {
    return node(KindCase.FIELD_LIST, SlangProto.FieldList.newBuilder()
      .addFields(node(KindCase.FIELD, SlangProto.Field.newBuilder()
        .addNames(identifierNode(name).setTextRange(SECOND_RANGE)))));
  }

  private static SlangProto.Token.Builder otherToken(String text, String textRange) {
    return SlangProto.Token.newBuilder().setText(text).setTextRange(textRange);
  }

  /** The two tokens every node of these trees resolves its positions against. */
  private static SlangProto.Tree treeOf(Node.Builder root) {
    return SlangProto.Tree.newBuilder()
      .addTokens(otherToken("foo", RANGE))
      .addTokens(otherToken("bar", SECOND_RANGE))
      .setRoot(root)
      .build();
  }

  private static Tree decode(Node.Builder root) {
    return decode(SlangProto.Tree.newBuilder(treeOf(root)));
  }

  private static Tree decode(SlangProto.Tree.Builder protoTree) {
    var result = ProtoTree.fromProto(frame("main.go", protoTree.build())).get("main.go");
    assertThat(result.isError()).as("decoding failed: %s", result.isError() ? result.error() : "").isFalse();
    return result.tree();
  }

  /** Frames one file the way toSlangProto does on the Go side: the length of the message, then the message. */
  private static byte[] frame(String fileName, SlangProto.Tree tree) {
    return frame(SlangProto.File.newBuilder().setName(fileName).setTree(tree).build().toByteArray());
  }

  private static byte[] frame(byte[] file) {
    return ByteBuffer.allocate(4 + file.length).order(ByteOrder.LITTLE_ENDIAN).putInt(file.length).put(file).array();
  }

  /**
   * A file whose name can be read but whose tree cannot: the tree is written as a length-delimited
   * field of bytes that are not a message, which is what a truncated or corrupted response looks like.
   */
  private static byte[] fileWithAnUnreadableTree(String fileName) throws IOException {
    var message = new ByteArrayOutputStream();
    var out = CodedOutputStream.newInstance(message);
    out.writeString(SlangProto.File.NAME_FIELD_NUMBER, fileName);
    out.writeByteArray(SlangProto.File.TREE_FIELD_NUMBER, new byte[] {(byte) 0xFF, (byte) 0xFF});
    out.flush();
    return message.toByteArray();
  }

}
