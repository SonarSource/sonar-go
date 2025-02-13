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
package org.sonar.go.converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.ClassDeclarationTree;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ImportDeclarationTree;
import org.sonar.go.api.ImportSpecificationTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.ParseException;
import org.sonar.go.api.ReturnTree;
import org.sonar.go.api.StarExpressionTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.go.converter.GoConverter.DefaultCommand.getExecutableForCurrentOS;

class GoConverterTest {

  @Test
  void test_parse_return() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 42}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "42");
  }

  @Test
  void test_parse_binary_notation() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 0b_0010_1010}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "_0010_1010");
  }

  @Test
  void test_parse_imaginary_literals() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 6.67428e-11i}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);
  }

  @Test
  void test_parse_embed_overlapping_interfaces() {
    Tree tree = TestGoConverter.parse("package main\ntype A interface{\n     DoX() string\n}\ntype B interface{\n     DoX() \n}\ntype AB interface{\n    A\n    B\n}");
    List<Tree> classList = tree.descendants()
      .filter(t -> t instanceof ClassDeclarationTree)
      .collect(Collectors.toList());
    assertThat(classList).hasSize(3);
  }

  @Test
  void test_parse_infinite_for() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {for {}}");
    List<Tree> returnList = tree.descendants().filter(t -> t instanceof LoopTree).collect(Collectors.toList());
    assertThat(returnList).hasSize(1);
  }

  @Test
  void test_parse_generics() {
    Tree tree = TestGoConverter.parse("package main\nfunc f1[T any]() {}\nfunc f2() {\nf:=f1[string]}");
    List<FunctionDeclarationTree> functions = tree.descendants()
      .filter(FunctionDeclarationTree.class::isInstance)
      .map(FunctionDeclarationTree.class::cast)
      .collect(Collectors.toList());
    assertThat(functions).hasSize(2);

    FunctionDeclarationTree functionDeclarationTree = functions.get(0);
    List<Tree> f1Children = functionDeclarationTree.children();
    assertThat(f1Children).hasSize(3);
    Tree typeParams = f1Children.get(1);
    assertThat(typeParams).isEqualTo(functionDeclarationTree.typeParameters()).isInstanceOf(NativeTree.class);
    assertThat(((NativeTree) typeParams).nativeKind()).hasToString("TypeParams(FieldList)");

    List<Tree> f2Children = functions.get(1).children();
    Tree f = ((BlockTree) f2Children.get(1)).statementOrExpressions().get(0);
    assertThat(f).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void test_parse_member_select() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {fmt.Println()}");
    List<MemberSelectTree> returnList = tree.descendants().filter(MemberSelectTree.class::isInstance).map(MemberSelectTree.class::cast).toList();
    assertThat(returnList).hasSize(1);
    MemberSelectTree memberSelectTree = returnList.get(0);
    assertThat(memberSelectTree.identifier().name()).isEqualTo("Println");
    Tree expression = memberSelectTree.expression();
    assertThat(expression).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("fmt"));
  }

  @Test
  void test_parse_function_invocation() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {bar(\"arg\", 42)}");
    List<Tree> functionInvocations = tree.descendants().filter(FunctionInvocationTree.class::isInstance).toList();
    assertThat(functionInvocations).hasSize(1);
    FunctionInvocationTree functionInvocation = (FunctionInvocationTree) functionInvocations.get(0);
    assertThat(functionInvocation.memberSelect()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("bar"));
    assertThat(functionInvocation.arguments()).hasSize(2);
    assertThat(functionInvocation.arguments().get(0)).isInstanceOfSatisfying(StringLiteralTree.class,
      stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("arg"));
    assertThat(functionInvocation.arguments().get(1)).isInstanceOfSatisfying(IntegerLiteralTree.class,
      integerLiteralTree -> assertThat(integerLiteralTree.getIntegerValue()).isEqualTo(42));
  }

  @Test
  void test_parse_composite_literal() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {thing := Thing{\"value\"}}");
    List<CompositeLiteralTree> returnList = tree.descendants().filter(CompositeLiteralTree.class::isInstance).map(CompositeLiteralTree.class::cast).toList();
    assertThat(returnList).hasSize(1);
    CompositeLiteralTree compositeLiteralTree = returnList.get(0);
    assertThat(compositeLiteralTree.type()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("Thing"));
    List<Tree> elements = compositeLiteralTree.elements();
    assertThat(elements).hasSize(1);
    assertThat(elements.get(0)).isInstanceOfSatisfying(StringLiteralTree.class, stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("value"));
  }

  static Stream<Arguments> test_parse_binary_expression() {
    return Stream.of(
      arguments("a + b", BinaryExpressionTree.Operator.PLUS),
      arguments("a - b", BinaryExpressionTree.Operator.MINUS),
      arguments("a * b", BinaryExpressionTree.Operator.TIMES),
      arguments("a / b", BinaryExpressionTree.Operator.DIVIDED_BY),
      arguments("a == b", BinaryExpressionTree.Operator.EQUAL_TO),
      arguments("a != b", BinaryExpressionTree.Operator.NOT_EQUAL_TO),
      arguments("a > b", BinaryExpressionTree.Operator.GREATER_THAN),
      arguments("a >= b", BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO),
      arguments("a < b", BinaryExpressionTree.Operator.LESS_THAN),
      arguments("a <= b", BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO),
      arguments("a && b", BinaryExpressionTree.Operator.CONDITIONAL_AND),
      arguments("a || b", BinaryExpressionTree.Operator.CONDITIONAL_OR),
      arguments("a & b", BinaryExpressionTree.Operator.BITWISE_AND),
      arguments("a | b", BinaryExpressionTree.Operator.BITWISE_OR),
      arguments("a ^ b", BinaryExpressionTree.Operator.BITWISE_XOR),
      arguments("a << b", BinaryExpressionTree.Operator.BITWISE_SHL),
      arguments("a >> b", BinaryExpressionTree.Operator.BITWISE_SHR),
      arguments("a &^ b", BinaryExpressionTree.Operator.BITWISE_AND_NOT));
  }

  @ParameterizedTest
  @MethodSource
  void test_parse_binary_expression(String code, BinaryExpressionTree.Operator operator) {
    var binaryExpression = (BinaryExpressionTree) TestGoConverter.parseStatement(code);
    assertThat(binaryExpression.leftOperand()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("a"));
    assertThat(binaryExpression.operator()).isSameAs(operator);
    assertThat(binaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("b"));
  }

  @Test
  void shouldParseComplexBinaryExpression() {
    var binaryExpression = (BinaryExpressionTree) TestGoConverter.parseStatement("a + b + c");
    assertThat(binaryExpression.leftOperand()).isInstanceOfSatisfying(BinaryExpressionTree.class, subBinaryExpression -> {
      assertThat(subBinaryExpression.leftOperand()).isInstanceOfSatisfying(IdentifierTree.class, identifierA -> assertThat(identifierA.name()).isEqualTo("a"));
      assertThat(subBinaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class, identifierB -> assertThat(identifierB.name()).isEqualTo("b"));
    });
    assertThat(binaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class, identifierC -> assertThat(identifierC.name()).isEqualTo("c"));
  }

  @Test
  void test_parse_star_expression() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {*string}");
    List<StarExpressionTree> starExpressionsList = tree.descendants().filter(StarExpressionTree.class::isInstance).map(StarExpressionTree.class::cast).toList();
    assertThat(starExpressionsList).hasSize(1);
    StarExpressionTree starExpressionTree = starExpressionsList.get(0);
    assertThat(starExpressionTree.operand()).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("string"));
  }

  @Test
  void test_parse_function_declaration_with_receiver() {
    Tree tree = TestGoConverter.parse("package main\nfunc (m MyType) foo(i int) {}");
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("foo");
    assertThat(functionDeclaration.typeParameters()).isNull();
    assertThat(functionDeclaration.receiver()).isInstanceOfSatisfying(NativeTree.class, nativeTree -> assertThat(nativeTree.nativeKind()).hasToString("Recv(FieldList)"));
    assertThat(functionDeclaration.formalParameters()).hasSize(1);
    assertThat(functionDeclaration.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
  }

  @Test
  void test_parse_external_function_declaration() {
    Tree tree = TestGoConverter.parse("""
      package main
      //go:noescape
      func externalFunction()
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("externalFunction");
    assertThat(functionDeclaration.formalParameters()).isEmpty();
    assertThat(functionDeclaration.body()).isNull();
  }

  @Test
  void test_parse_function_declaration_with_type_parameter() {
    Tree tree = TestGoConverter.parse("""
      package main
      func funWithTypeParameter[T any]() {}
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("funWithTypeParameter");
    assertThat(functionDeclaration.formalParameters()).isEmpty();
    assertThat(functionDeclaration.typeParameters()).isInstanceOfSatisfying(NativeTree.class,
      nativeTree -> assertThat(nativeTree.nativeKind()).hasToString("TypeParams(FieldList)"));
  }

  @Test
  void test_parse_imports() {
    Tree tree = TestGoConverter.parse("""
      package main
      import (
      	"io"
      	name "log"
      )
      """);
    List<Tree> importDeclarations = tree.descendants().filter(ImportDeclarationTree.class::isInstance).toList();
    assertThat(importDeclarations).hasSize(1);
    ImportDeclarationTree importDeclaration = (ImportDeclarationTree) importDeclarations.get(0);

    // Token "import", "(", ImportSpec, ImportSpec, ")"
    assertThat(importDeclaration.children()).hasSize(5);

    Tree firstImport = importDeclaration.children().get(2);
    assertThat(firstImport).isInstanceOf(ImportSpecificationTree.class);
    ImportSpecificationTree firstImportSpecification = (ImportSpecificationTree) firstImport;
    assertThat(firstImportSpecification.name()).isNull();
    assertThat(firstImportSpecification.path()).isInstanceOfSatisfying(StringLiteralTree.class,
      stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("io"));

    Tree secondImport = importDeclaration.children().get(3);
    assertThat(secondImport).isInstanceOf(ImportSpecificationTree.class);
    ImportSpecificationTree secondImportSpecification = (ImportSpecificationTree) secondImport;
    assertThat(secondImportSpecification.name()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierTree -> assertThat(identifierTree.name()).isEqualTo("name"));
    assertThat(secondImportSpecification.path()).isInstanceOfSatisfying(StringLiteralTree.class,
      stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("log"));
  }

  @Test
  void parse_error() {
    ParseException e = assertThrows(ParseException.class,
      () -> TestGoConverter.parse("$!#@"));
    assertThat(e).hasMessage("Go parser external process returned non-zero exit value: 2");
  }

  @Test
  void invalid_command() {
    GoConverter.Command command = mock(GoConverter.Command.class);
    when(command.getCommand()).thenReturn(Collections.singletonList("invalid-command"));
    GoConverter converter = new GoConverter(command);
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("package main\nfunc foo() {}"));
    assertThat(e).hasMessageContaining("Cannot run program \"invalid-command\"");
  }

  @Test
  void parse_accepted_big_file() {
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[700_000 - code.length()]).replace("\0", "\n");
    Tree tree = TestGoConverter.parse(bigCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  void parse_rejected_big_file() {
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[1_500_000]).replace("\0", "\n");
    ParseException e = assertThrows(ParseException.class,
      () -> TestGoConverter.parse(bigCode));
    assertThat(e).hasMessage("The file size is too big and should be excluded, its size is 1500028 (maximum allowed is 1500000 bytes)");
  }

  @Test
  void load_invalid_executable_path() throws IOException {
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> GoConverter.DefaultCommand.getBytesFromResource("invalid-exe-path"));
    assertThat(e).hasMessage("invalid-exe-path binary not found on class path");
  }

  @Test
  void executable_for_current_os() {
    assertThat(getExecutableForCurrentOS("Linux", "x86_64")).isEqualTo("sonar-go-to-slang-linux-amd64");
    assertThat(getExecutableForCurrentOS("Windows 10", "x86_64")).isEqualTo("sonar-go-to-slang-windows-amd64.exe");
    assertThat(getExecutableForCurrentOS("Mac OS X", "x86_64")).isEqualTo("sonar-go-to-slang-darwin-amd64");
    assertThat(getExecutableForCurrentOS("Mac OS X", "aarch64")).isEqualTo("sonar-go-to-slang-darwin-arm64");
  }

  private List<ReturnTree> getReturnsList(Tree tree) {
    return tree.descendants()
      .filter(t -> t instanceof ReturnTree)
      .map(ReturnTree.class::cast)
      .collect(Collectors.toList());
  }

  private void checkIntegerValue(ReturnTree returnTree, String s) {
    IntegerLiteralTree integerLiteralTree = (IntegerLiteralTree) returnTree.body();
    assertThat(integerLiteralTree.getNumericPart()).isEqualTo(s);
  }
}
