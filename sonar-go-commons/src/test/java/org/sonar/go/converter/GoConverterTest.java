/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.CompositeLiteralTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.ImportDeclarationTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.ReturnTree;
import org.sonar.plugins.go.api.StarExpressionTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeOrError;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.cfg.Block;
import org.sonar.plugins.go.api.cfg.ControlFlowGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.from;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GoConverterTest {
  @TempDir
  File tempDir;

  @Test
  void testParseReturn() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {return 42}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "42");
  }

  @Test
  void testParseBinaryNotation() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {return 0b_0010_1010}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "00101010");
  }

  @Test
  void testParseImaginaryLiterals() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {return 6.67428e-11i}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);
  }

  @Test
  void testParseEmbedOverlappingInterfaces() {
    var tree = TestGoConverterSingleFile.parse("""
      package main
      type A interface{
           DoX() string
      }
      type B interface{
           DoX()\s
      }
      type AB \
      interface{
          A
          B
      }""");
    List<Tree> classList = tree.descendants()
      .filter(ClassDeclarationTree.class::isInstance)
      .toList();
    assertThat(classList).hasSize(3);
  }

  @Test
  void testParseInfiniteFor() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {for {}}");
    List<Tree> returnList = tree.descendants().filter(LoopTree.class::isInstance).toList();
    assertThat(returnList).hasSize(1);
  }

  @Test
  void testParseGenerics() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc f1[T any]() {}\nfunc f2() {\nf:=f1[string]}");
    List<FunctionDeclarationTree> functions = tree.descendants()
      .filter(FunctionDeclarationTree.class::isInstance)
      .map(FunctionDeclarationTree.class::cast)
      .toList();
    assertThat(functions).hasSize(2);

    FunctionDeclarationTree functionDeclarationTree = functions.get(0);
    List<Tree> f1Children = functionDeclarationTree.children();
    assertThat(f1Children).hasSize(3);
    Tree typeParams = f1Children.get(1);
    assertThat(typeParams).isEqualTo(functionDeclarationTree.typeParameters()).isInstanceOf(NativeTree.class);
    assertThat(((NativeTree) typeParams).nativeKind()).isInstanceOfSatisfying(StringNativeKind.class,
      stringNativeKind -> assertThat(stringNativeKind.kind()).isEqualTo("TypeParams(FieldList)"));

    List<Tree> f2Children = functions.get(1).children();
    Tree f = ((BlockTree) f2Children.get(1)).statementOrExpressions().get(0);
    assertThat(f).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void testParseMemberSelect() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {fmt.Println()}");
    List<MemberSelectTree> returnList = tree.descendants().filter(MemberSelectTree.class::isInstance).map(MemberSelectTree.class::cast).toList();
    assertThat(returnList).hasSize(1);
    MemberSelectTree memberSelectTree = returnList.get(0);
    assertThat(memberSelectTree.identifier().name()).isEqualTo("Println");
    Tree expression = memberSelectTree.expression();
    assertThat(expression).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo(
      "fmt"));
  }

  @Test
  void testParseFunctionInvocation() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {bar(\"arg\", 42)}");
    List<Tree> functionInvocations = tree.descendants().filter(FunctionInvocationTree.class::isInstance).toList();
    assertThat(functionInvocations).hasSize(1);
    FunctionInvocationTree functionInvocation = (FunctionInvocationTree) functionInvocations.get(0);
    assertThat(functionInvocation.memberSelect()).isInstanceOfSatisfying(IdentifierTree.class,
      identifier -> assertThat(identifier.name()).isEqualTo("bar"));
    assertThat(functionInvocation.arguments()).hasSize(2);
    assertThat(functionInvocation.arguments().get(0)).isInstanceOfSatisfying(StringLiteralTree.class,
      stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("arg"));
    assertThat(functionInvocation.arguments().get(1)).isInstanceOfSatisfying(IntegerLiteralTree.class,
      integerLiteralTree -> assertThat(integerLiteralTree.getIntegerValue()).isEqualTo(42));
  }

  @Test
  void testParseCompositeLiteral() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {thing := Thing{\"value\"}}");
    List<CompositeLiteralTree> returnList = tree.descendants().filter(CompositeLiteralTree.class::isInstance).map(CompositeLiteralTree.class::cast).toList();
    assertThat(returnList).hasSize(1);
    CompositeLiteralTree compositeLiteralTree = returnList.get(0);
    assertThat(compositeLiteralTree.type()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierTree -> assertThat(identifierTree.name()).isEqualTo("Thing"));
    List<Tree> elements = compositeLiteralTree.elements();
    assertThat(elements).hasSize(1);
    assertThat(elements.get(0)).isInstanceOfSatisfying(StringLiteralTree.class,
      stringLiteralTree -> assertThat(stringLiteralTree.content()).isEqualTo("value"));
  }

  static Stream<Arguments> testParseBinaryExpression() {
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
  void testParseBinaryExpression(String code, BinaryExpressionTree.Operator operator) {
    var binaryExpression = (BinaryExpressionTree) TestGoConverterSingleFile.parseStatement(code);
    assertThat(binaryExpression.leftOperand()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierTree -> assertThat(identifierTree.name()).isEqualTo("a"));
    assertThat(binaryExpression.operator()).isSameAs(operator);
    assertThat(binaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierTree -> assertThat(identifierTree.name()).isEqualTo("b"));
  }

  @Test
  void shouldParseComplexBinaryExpression() {
    var binaryExpression = (BinaryExpressionTree) TestGoConverterSingleFile.parseStatement("a + b + c");
    assertThat(binaryExpression.leftOperand()).isInstanceOfSatisfying(BinaryExpressionTree.class, subBinaryExpression -> {
      assertThat(subBinaryExpression.leftOperand()).isInstanceOfSatisfying(IdentifierTree.class,
        identifierA -> assertThat(identifierA.name()).isEqualTo("a"));
      assertThat(subBinaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class,
        identifierB -> assertThat(identifierB.name()).isEqualTo("b"));
    });
    assertThat(binaryExpression.rightOperand()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierC -> assertThat(identifierC.name()).isEqualTo("c"));
  }

  @Test
  void testParseStarExpression() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc foo() {*string}");
    List<StarExpressionTree> starExpressionsList = tree.descendants().filter(StarExpressionTree.class::isInstance).map(StarExpressionTree.class::cast).toList();
    assertThat(starExpressionsList).hasSize(1);
    StarExpressionTree starExpressionTree = starExpressionsList.get(0);
    assertThat(starExpressionTree.operand()).isInstanceOfSatisfying(IdentifierTree.class,
      identifierTree -> assertThat(identifierTree.name()).isEqualTo("string"));
  }

  @Test
  void testParseFunctionDeclarationWithReceiver() {
    Tree tree = TestGoConverterSingleFile.parse("package main\nfunc (m MyType) foo(i int) {}");
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("foo");
    assertThat(functionDeclaration.name().type()).isEqualTo("func(i int)");
    assertThat(functionDeclaration.typeParameters()).isNull();
    assertThat(functionDeclaration.receiver()).isInstanceOfSatisfying(NativeTree.class, nativeTree -> assertThat(nativeTree.nativeKind())
      .isInstanceOfSatisfying(StringNativeKind.class,
        stringNativeKind -> assertThat(stringNativeKind.kind()).isEqualTo("Recv(FieldList)")));
    assertThat(functionDeclaration.formalParameters()).hasSize(1);
    assertThat(functionDeclaration.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
  }

  @Test
  void testParseExternalFunctionDeclaration() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      //go:noescape
      func externalFunction()
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("externalFunction");
    assertThat(functionDeclaration.name().type()).isEqualTo("func()");
    assertThat(functionDeclaration.formalParameters()).isEmpty();
    assertThat(functionDeclaration.body()).isNull();
  }

  enum Result {
    LITERAL_ONE,
    LITERAL_TWO,
    METHOD_FOO
  }

  static Stream<Arguments> testParseVariableDeclarationDetailed() {
    return Stream.of(
      arguments("var a = 1             ", List.of("a"), List.of(Result.LITERAL_ONE), null, false),
      arguments("var a int             ", List.of("a"), List.of(), "int", false),
      arguments("var a int = 1         ", List.of("a"), List.of(Result.LITERAL_ONE), "int", false),
      arguments("var a, b = foo()      ", List.of("a", "b"), List.of(Result.METHOD_FOO), null, false),
      arguments("var a, b int = foo()  ", List.of("a", "b"), List.of(Result.METHOD_FOO), "int", false),
      arguments("var a, b = 1, 2       ", List.of("a", "b"), List.of(Result.LITERAL_ONE, Result.LITERAL_TWO), null, false),
      arguments("var a, b int = 1, 2   ", List.of("a", "b"), List.of(Result.LITERAL_ONE, Result.LITERAL_TWO), "int", false),
      arguments("const a = 1           ", List.of("a"), List.of(Result.LITERAL_ONE), null, true),
      arguments("const a int           ", List.of("a"), List.of(), "int", true),
      arguments("const a int = 1       ", List.of("a"), List.of(Result.LITERAL_ONE), "int", true),
      arguments("const a, b = foo()    ", List.of("a", "b"), List.of(Result.METHOD_FOO), null, true),
      arguments("const a, b int = foo()", List.of("a", "b"), List.of(Result.METHOD_FOO), "int", true),
      arguments("const a, b = 1, 2     ", List.of("a", "b"), List.of(Result.LITERAL_ONE, Result.LITERAL_TWO), null, true),
      arguments("const a, b int = 1, 2 ", List.of("a", "b"), List.of(Result.LITERAL_ONE, Result.LITERAL_TWO), "int", true),
      arguments("a := 1                ", List.of("a"), List.of(Result.LITERAL_ONE), null, false),
      arguments("a, b := foo()         ", List.of("a", "b"), List.of(Result.METHOD_FOO), null, false),
      arguments("a, b := 1, 2          ", List.of("a", "b"), List.of(Result.LITERAL_ONE, Result.LITERAL_TWO), null, false));
  }

  @ParameterizedTest
  @MethodSource
  void testParseVariableDeclarationDetailed(String code, List<String> variableNames, List<Result> variableValues,
    @Nullable String type, boolean isVal) {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      func foo() {
        %s
      }
      """.formatted(code));
    List<Tree> variableDeclarations = tree.descendants().filter(VariableDeclarationTree.class::isInstance).toList();
    var variableDeclaration = (VariableDeclarationTree) variableDeclarations.get(0);

    assertThat(variableDeclaration.identifiers()).hasSize(variableNames.size());
    for (IdentifierTree identifierTree : variableDeclaration.identifiers()) {
      assertThat(variableNames).contains(identifierTree.name());
    }

    assertThat(variableDeclaration.initializers()).hasSize(variableValues.size());
    for (int i = 0; i < variableValues.size(); i++) {
      Result result = variableValues.get(i);
      switch (result) {
        case LITERAL_ONE:
          assertThat(variableDeclaration.initializers().get(i)).isInstanceOfSatisfying(IntegerLiteralTree.class,
            integerLiteralTree -> assertThat(integerLiteralTree.getIntegerValue()).isEqualTo(1));
          break;
        case LITERAL_TWO:
          assertThat(variableDeclaration.initializers().get(i)).isInstanceOfSatisfying(IntegerLiteralTree.class,
            integerLiteralTree -> assertThat(integerLiteralTree.getIntegerValue()).isEqualTo(2));
          break;
        case METHOD_FOO:
          assertThat(variableDeclaration.initializers().get(i)).isInstanceOfSatisfying(FunctionInvocationTree.class,
            functionInvocationTree -> {
              assertThat(functionInvocationTree.memberSelect()).isInstanceOfSatisfying(IdentifierTree.class,
                identifierTree -> assertThat(identifierTree.name()).isEqualTo("foo"));
            });
      }
    }

    assertThat(variableDeclaration.isVal()).isEqualTo(isVal);

    if (type == null) {
      assertThat(variableDeclaration.type()).isNull();
    } else {
      assertThat(variableDeclaration.type()).isInstanceOfSatisfying(IdentifierTree.class,
        identifier -> assertThat(identifier.name()).isEqualTo(type));
    }
  }

  @Test
  void testParseFunctionDeclarationWithTypeParameter() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      func funWithTypeParameter[T any]() {}
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(functionDeclaration.name().name()).isEqualTo("funWithTypeParameter");
    assertThat(functionDeclaration.name().type()).isEqualTo("func[T any]()");
    assertThat(functionDeclaration.formalParameters()).isEmpty();
    assertThat(functionDeclaration.typeParameters()).isInstanceOfSatisfying(NativeTree.class,
      nativeTree -> assertThat(nativeTree.nativeKind()).isInstanceOfSatisfying(StringNativeKind.class,
        stringNativeKind -> assertThat(stringNativeKind.kind()).isEqualTo("TypeParams(FieldList)")));
  }

  @Test
  void testParseImports() {
    Tree tree = TestGoConverterSingleFile.parse("""
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
  void shouldNotFailWithParseError() {
    var parseResult = TestGoConverterSingleFile.parseAndReturnTreeOrError("$!#@");
    assertThat(parseResult.isError()).isTrue();
    assertThat(parseResult.error()).isEqualTo("foo.go:1:1: illegal character U+0024 '$'");
  }

  @Test
  void shouldFailOnInvalidCommand() {
    var command = new GoParseCommand(tempDir);
    command.getCommand().set(0, "invalid-command");
    GoConverter converter = new GoConverter(command);
    var filenameToContentMap = Map.of("foo.go", "package main\nfunc foo() {}");
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse(filenameToContentMap, "moduleName"));
    assertThat(e).hasMessageContaining("Cannot run program \"invalid-command\"");
  }

  @Test
  void shouldParseAcceptedBigFile() {
    var code = """
      package main
      func foo() {
      }
      """;
    String bigCode = code + new String(new char[700_000 - code.length()]).replace("\0", "\n");
    Tree tree = TestGoConverterSingleFile.parse(bigCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  void bigFileShouldBeSilentlyRejectedInParse() {
    var code = """
      package main
      func foo() {
      }
      """;
    String bigCode = code + new String(new char[1_500_000]).replace("\0", "\n");
    TreeOrError treeOrError = TestGoConverterSingleFile.parseAndReturnTreeOrError(bigCode);
    assertThat(treeOrError.isError()).isTrue();
    assertThat(treeOrError.error()).isEqualTo("The file size is too big and should be excluded, its size is 1500028 (maximum allowed is 1500000 bytes)");
  }

  @Test
  void shouldThrowExceptionOnInvalidExecutablePath() {
    assertThatThrownBy(() -> DefaultCommand.getBytesFromResource("invalid-exe-path"))
      .isInstanceOf(InitializationException.class)
      .hasMessage("invalid-exe-path binary not found on class path");
  }

  @Test
  void shouldParseFunctionWithTypeDetectionDatabaseSql() {
    var tree = TestGoConverterSingleFile.parse("""
      package main
      import (
          "database/sql"
          "strconv"
      )
      func selectFromTable(id int) *sql.Rows {
          driverName := "mysql"
          dataSource := "dataSource"
          var db, _ = sql.Open(driverName, dataSource)
          var rows, _ = db.Query("SELECT * FROM table WHERE id = " + strconv.Itoa(id))
          return rows
      }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "id").type()).isEqualTo("int");
    assertThat(getIdentifierByName(functionDeclaration, "driverName").type()).isEqualTo("string");
    assertThat(getIdentifierByName(functionDeclaration, "db").type()).isEqualTo("*database/sql.DB");
  }

  @Test
  void shouldParseFunctionWithTypeDetectionNativeType() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      import "fmt"

      func main() {
        n := 42
        fmt.Println(n)
      }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "n").type()).isEqualTo("int");
  }

  @Test
  void shouldParseFunctionWithTypeDetectionHttpCookie() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      import (
          	"fmt"
            "net/http"
      )
      func printCookie(id int) {
          	cookie := http.Cookie{}
            fmt.Println(cookie)
      }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "cookie").type()).isEqualTo("net/http.Cookie");
  }

  @Test
  void shouldParseFunctionWithTypeDetectionBeegoServerWeb() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      import "github.com/beego/beego/v2/server/web"

      type MainController struct {
        web.Controller
      }

      func (ctrl *MainController) getSessionStore() {
        session, _ := ctrl.Ctx.Session()
      }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "session").type()).isEqualTo("github.com/beego/beego/v2/server/web/session.Store");
  }

  @Test
  void shouldParseFunctionWithTypeDetectionLocalType() {
    Tree tree = TestGoConverterSingleFile.parse("""
       package main
       import "fmt"

       type Person struct {
         name string
         age int
       }

       func main() {
         person := Person{"John", 42}
         fmt.Println(person.name)
       }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "person").type()).isEqualTo("main.Person");
  }

  @Test
  void shouldParseFunctionWithUnresolvedPackages() {
    Tree tree = TestGoConverterSingleFile.parse("""
       package main
       import (
         "errors"
         "github.com/rs/zerolog/log"
       )

       func main() {
         sublogger := log.With().Str("component", "foo").Logger()
         err := errors.New("an error occurred")
         sublogger.Error().Err(err).Msg("hello world")
       }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);

    assertThat(getIdentifierByName(functionDeclaration, "sublogger").type()).isEqualTo("UNKNOWN");
    assertThat(getIdentifierByName(functionDeclaration, "err").type()).isEqualTo("error");
  }

  @Test
  void shouldParseFunctionDeclarationCfgWithIfStatements() {
    Tree tree = TestGoConverterSingleFile.parse("""
       package main
       import (
         "fmt"
       )

       func main() {
          a := 1
          if a == 1 {
            fmt.Println("a is 1")
          } else {
            fmt.Println("a is not 1")
          }
       }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);
    ControlFlowGraph cfg = functionDeclaration.cfg();
    assertThat(cfg).isNotNull();
    assertThat(cfg.blocks()).hasSize(4);
    Block entry = cfg.entryBlock();
    assertThat(entry.nodes()).hasSize(2);
    // a := 1
    assertThat(entry.nodes().get(0)).isInstanceOf(VariableDeclarationTree.class);
    // a == 1
    assertThat(entry.nodes().get(1)).isInstanceOf(BinaryExpressionTree.class);
    assertThat(entry.successors()).hasSize(2);
    // fmt.Println("a is 1")
    Block thenPart = entry.successors().get(0);
    assertThat(thenPart.nodes()).hasSize(1);
    assertThat(getStringDescendant(thenPart.nodes().get(0))).contains("a is 1");
    assertThat(thenPart.successors()).hasSize(1);
    // fmt.Println("a is not 1")
    Block elsePart = entry.successors().get(1);
    assertThat(elsePart.nodes()).hasSize(1);
    assertThat(getStringDescendant(elsePart.nodes().get(0))).contains("a is not 1");
    assertThat(elsePart.successors()).hasSize(1);

    Block endBlock = thenPart.successors().get(0);
    assertThat(endBlock).isSameAs(elsePart.successors().get(0));
    assertThat(endBlock.successors()).isEmpty();
    assertThat(endBlock.nodes()).isEmpty();
  }

  @Test
  void shouldParseFunctionDeclarationCfgWithSwitchStatements() {
    Tree tree = TestGoConverterSingleFile.parse("""
      package main
      import (
        "fmt"
      )
      func main() {
        a := 1
        switch a {
        case 1:
          fmt.Println("a is 1")
        case 2:
          fmt.Println("a is 2")
        default:
          fmt.Println("a is neither 1 nor 2")
        }
      }
      """);
    List<Tree> functionDeclarations = tree.descendants().filter(FunctionDeclarationTree.class::isInstance).toList();
    assertThat(functionDeclarations).hasSize(1);
    FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) functionDeclarations.get(0);
    ControlFlowGraph cfg = functionDeclaration.cfg();
    assertThat(cfg).isNotNull();
    assertThat(cfg.blocks()).hasSize(7);
    Block entry = cfg.entryBlock();
    assertThat(entry.nodes()).hasSize(2);
    // a := 1
    assertThat(entry.nodes().get(0)).isInstanceOf(VariableDeclarationTree.class);
    // switch a
    assertThat(entry.nodes().get(1)).isInstanceOf(IdentifierTree.class);
    // CFG for switch is like a chain of if-else statements.
    assertThat(entry.successors()).hasSize(2);
    // fmt.Println("a is 1")
    Block case1 = entry.successors().get(0);
    assertThat(case1.nodes()).hasSize(1);
    assertThat(getStringDescendant(case1.nodes().get(0))).contains("a is 1");
    assertThat(case1.successors()).hasSize(1);
    // Intermediate node for case 2
    Block case2Intermediate = entry.successors().get(1);
    assertThat(case2Intermediate.nodes()).isEmpty();
    assertThat(case2Intermediate.successors()).hasSize(2);
    // fmt.Println("a is 2")
    Block case2 = case2Intermediate.successors().get(0);
    assertThat(case2.nodes()).hasSize(1);
    assertThat(getStringDescendant(case2.nodes().get(0))).contains("a is 2");
    assertThat(case2.successors()).hasSize(1);
    // Intermediate node for case default
    Block caseDefaultIntermediate = case2Intermediate.successors().get(1);
    assertThat(caseDefaultIntermediate.nodes()).isEmpty();
    assertThat(caseDefaultIntermediate.successors()).hasSize(1);
    // fmt.Println("a is neither 1 nor 2")
    Block defaultCase = caseDefaultIntermediate.successors().get(0);
    assertThat(defaultCase.nodes()).hasSize(1);
    assertThat(getStringDescendant(defaultCase.nodes().get(0))).contains("a is neither 1 nor 2");
    assertThat(defaultCase.successors()).hasSize(1);

    // End block
    Block endBlock = defaultCase.successors().get(0);
    assertThat(endBlock)
      .isSameAs(case2.successors().get(0))
      .isSameAs(defaultCase.successors().get(0));
    assertThat(endBlock.successors()).isEmpty();
  }

  @Test
  void shouldParseFunctionDeclarationCfgWithInnerFunction() {
    Tree tree = TestGoConverterSingleFile.parse("""
       package main
       import (
         "fmt"
       )

      func main() {
          var counter int = 1

          func(str string) {
              fmt.Println("Hi", str, "I'm an anonymous function")
          }("Ricky")

          funcVar := func(str string) {
              x := 5
          }

          funcVar("Bob")
      }
      """);
    List<FunctionDeclarationTree> functionDeclarations = tree.descendants()
      .filter(FunctionDeclarationTree.class::isInstance)
      .map(FunctionDeclarationTree.class::cast)
      .toList();
    assertThat(functionDeclarations).hasSize(3);

    var funcMain = functionDeclarations.get(0);
    var cfgMain = funcMain.cfg();
    assertThat(cfgMain).isNotNull();
    assertThat(cfgMain.blocks()).hasSize(1);
    Block entry = cfgMain.entryBlock();
    assertThat(entry.nodes()).hasSize(4);
    assertThat(entry.nodes().get(0)).isInstanceOf(VariableDeclarationTree.class);
    assertThat(entry.nodes().get(1).children().get(0)).isInstanceOf(FunctionInvocationTree.class);
    assertThat(entry.nodes().get(2)).isInstanceOf(VariableDeclarationTree.class);
    assertThat(getStringDescendant(entry.nodes().get(3))).contains("Bob");

    var funcRicky = functionDeclarations.get(1);
    var cfgRicky = funcRicky.cfg();
    assertThat(cfgRicky).isNotNull();
    assertThat(cfgRicky.blocks()).hasSize(1);
    var entryRicky = cfgRicky.entryBlock();
    assertThat(entryRicky.nodes()).hasSize(1);
    assertThat(entryRicky.nodes().get(0).children().get(0)).isInstanceOf(FunctionInvocationTree.class);

    var funcVar = functionDeclarations.get(2);
    var cfgVar = funcVar.cfg();
    assertThat(cfgVar).isNotNull();
    assertThat(cfgVar.blocks()).hasSize(1);
    var entryVar = cfgVar.entryBlock();
    assertThat(entryVar.nodes()).hasSize(1);
    assertThat(entryVar.nodes().get(0)).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void shouldCallDebugTypeCheckOnCommand() {
    var command = new GoParseCommand(tempDir);
    var converter = new GoConverter(command);

    converter.debugTypeCheck();

    assertThat(command.command).contains("-debug_type_check");
  }

  @Test
  void shouldBeInitializedIfCommandCreated() {
    var converter = new GoConverter(new GoParseCommand(tempDir));

    assertThat(converter.isInitialized()).isTrue();
  }

  @Test
  void shouldBeNotInitializedIfUnsupportedPlatform() {
    var unsupportedPlatform = new TestPlatformInfo("unsupported-os", "unsupported-arch");

    var converter = new GoConverter(tempDir, unsupportedPlatform);

    assertThat(converter)
      .isNotNull()
      .returns(false, from(GoConverter::isInitialized));
  }

  private Optional<String> getStringDescendant(Tree tree) {
    return tree.descendants()
      .filter(StringLiteralTree.class::isInstance)
      .map(StringLiteralTree.class::cast)
      .map(StringLiteralTree::content)
      .findFirst();
  }

  private static IdentifierTree getIdentifierByName(FunctionDeclarationTree functionDeclaration, String name) {
    return functionDeclaration.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .filter(t -> name.equals(t.name()))
      .findFirst()
      .orElse(null);
  }

  private List<ReturnTree> getReturnsList(Tree tree) {
    return tree.descendants()
      .filter(ReturnTree.class::isInstance)
      .map(ReturnTree.class::cast)
      .toList();
  }

  private void checkIntegerValue(ReturnTree returnTree, String s) {
    IntegerLiteralTree integerLiteralTree = (IntegerLiteralTree) returnTree.expressions().get(0);
    assertThat(integerLiteralTree.getNumericPart()).isEqualTo(s);
  }
}
