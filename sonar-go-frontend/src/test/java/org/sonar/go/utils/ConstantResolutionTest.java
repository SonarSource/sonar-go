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
package org.sonar.go.utils;

import java.math.BigInteger;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeContext;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.sonar.go.utils.ConstantResolution.resolveAsStringConstant;
import static org.sonar.go.utils.ParseUtils.parse;
import static org.sonar.go.utils.ParseUtils.parseFile;
import static org.sonar.go.utils.ParseUtils.parseStatements;
import static org.sonar.plugins.go.api.BinaryExpressionTree.Operator.PLUS;
import static org.sonar.plugins.go.api.BinaryExpressionTree.Operator.TIMES;

class ConstantResolutionTest {
  private static final Tree HELLO = new StringLiteralTreeImpl(null, "\"Hello\"");
  private static final Tree WORLD = new StringLiteralTreeImpl(null, "\"World\"");

  @Test
  void simpleStringConstantResolution() {
    assertThat(resolveAsStringConstant(HELLO)).isEqualTo("Hello");
  }

  @Test
  void parenthesisConstantResolution() {
    Token leftParenthesis = new TokenImpl(new TextRangeImpl(1, 1, 1, 6), "(", Token.Type.OTHER);
    Token rightParenthesis = new TokenImpl(new TextRangeImpl(1, 1, 1, 6), ")", Token.Type.OTHER);
    Tree parenthesized = new ParenthesizedExpressionTreeImpl(null, HELLO, leftParenthesis, rightParenthesis);
    assertThat(resolveAsStringConstant(parenthesized)).isEqualTo("Hello");
  }

  @Test
  void binaryConstantResolution() {
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, WORLD);
    assertThat(resolveAsStringConstant(binary)).isEqualTo("HelloWorld");
  }

  @Test
  void binaryOtherOperatorConstantResolution() {
    BinaryExpressionTree binary = TreeCreationUtils.binary(TIMES, HELLO, WORLD);
    assertThat(resolveAsStringConstant(binary)).isNull();
  }

  @Test
  void binaryNestedConstantResolution() {
    Tree comma = new StringLiteralTreeImpl(null, "\", \"");
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, comma);
    BinaryExpressionTree binaryNested = TreeCreationUtils.binary(PLUS, binary, WORLD);
    assertThat(resolveAsStringConstant(binaryNested)).isEqualTo("Hello, World");
  }

  @Test
  void unresolvedConstantResolution() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    assertThat(resolveAsStringConstant(tree)).isNull();
  }

  @Test
  void binaryWithUnresolvedConstantConstantResolution() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, tree);
    assertThat(resolveAsStringConstant(binary)).isNull();
  }

  @Test
  void effectivelyFinalIdentifierConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, HELLO, Usage.UsageType.DECLARATION));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isEqualTo("Hello");
  }

  @Test
  void parameterIdentifierCannotBeResolved() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, HELLO, Usage.UsageType.PARAMETER));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isNull();
  }

  @Test
  void reAssignedIdentifierConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, HELLO, Usage.UsageType.DECLARATION));
    symbol.getUsages().add(new Usage(id, WORLD, Usage.UsageType.ASSIGNMENT));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isNull();
  }

  @Test
  void noDeclarationAndSingleAssignmentIdentifierConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, WORLD, Usage.UsageType.ASSIGNMENT));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isEqualTo("World");
  }

  @Test
  void noValueInDeclarationConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, null, Usage.UsageType.DECLARATION));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isNull();
  }

  @Test
  void multipleDeclarationsIdentifierConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, HELLO, Usage.UsageType.DECLARATION));
    symbol.getUsages().add(new Usage(id, WORLD, Usage.UsageType.DECLARATION));
    id.setSymbol(symbol);
    assertThat(resolveAsStringConstant(id)).isNull();
  }

  @Test
  void identifierWithNoSymbolConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    assertThat(resolveAsStringConstant(id)).isNull();
  }

  @Test
  void nullConstantResolution() {
    assertThat(resolveAsStringConstant(null)).isNull();
  }

  @Test
  void aliasIdentifierConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    symbol.getUsages().add(new Usage(id, HELLO, Usage.UsageType.DECLARATION));
    id.setSymbol(symbol);

    IdentifierTree idAlias = TreeCreationUtils.identifier("IDAlias");
    Symbol symbolAlias = new Symbol("type");
    symbolAlias.getUsages().add(new Usage(idAlias, id, Usage.UsageType.DECLARATION));
    idAlias.setSymbol(symbolAlias);

    assertThat(resolveAsStringConstant(idAlias)).isEqualTo("Hello");
  }

  @Test
  void binaryInDeclarationConstantConstantResolution() {
    IdentifierTree id = TreeCreationUtils.identifier("ID");
    Symbol symbol = new Symbol("type");
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, WORLD);
    symbol.getUsages().add(new Usage(id, binary, Usage.UsageType.DECLARATION));
    id.setSymbol(symbol);

    assertThat(resolveAsStringConstant(id)).isEqualTo("HelloWorld");
  }

  @Test
  void simpleStringIsConstantString() {
    assertThat(ConstantResolution.isConstantString(HELLO)).isTrue();
  }

  @Test
  void binaryNestedIsConstantString() {
    Tree comma = new StringLiteralTreeImpl(null, "\", \"");
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, comma);
    BinaryExpressionTree binaryNested = TreeCreationUtils.binary(PLUS, binary, WORLD);
    assertThat(ConstantResolution.isConstantString(binaryNested)).isTrue();
  }

  @Test
  void unresolvedIsNotConstantString() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    assertThat(ConstantResolution.isConstantString(tree)).isFalse();
  }

  @Test
  void binaryWithUnresolvedConstantIsNoConstantString() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, tree);
    assertThat(ConstantResolution.isConstantString(binary)).isFalse();
  }

  @Test
  void shouldHandleRecursiveAssignments() {
    var root = (TopLevelTree) TestGoConverterSingleFile.parse("""
      package main
      func main() {
        a := "a"
        b := a
        c := b
        b = c
      }
      """);

    TreeContext ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, root);

    var main = ((FunctionDeclarationTree) root.declarations().get(1)).body();
    assertThat(resolveAsStringConstant(((VariableDeclarationTree) main.statementOrExpressions().get(0)).identifiers().get(0))).isEqualTo("a");
    assertThat(resolveAsStringConstant(((VariableDeclarationTree) main.statementOrExpressions().get(1)).identifiers().get(0))).isNull();
    assertThat(resolveAsStringConstant(((VariableDeclarationTree) main.statementOrExpressions().get(2)).identifiers().get(0))).isNull();
    assertThat(resolveAsStringConstant(((AssignmentExpressionTree) main.statementOrExpressions().get(3)).leftHandSide())).isNull();
  }

  @Test
  void shouldNotGoIntoInfiniteRecursionToResolveConstant() {
    var root = (TopLevelTree) TestGoConverterSingleFile.parse("""
      package main
      func main(a string) {
        var b string
        a = a + "t"
        b = a
      }
      """);

    TreeContext ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, root);

    var functionMain = (FunctionDeclarationTree) root.declarations().get(1);
    var body = functionMain.body();
    var variableAssignmentB = (AssignmentExpressionTree) body.statementOrExpressions().get(2);
    var refB = variableAssignmentB.leftHandSide();
    assertThatNoException().isThrownBy(() -> {
      var result = ConstantResolution.resolveAsStringConstant(refB);
      assertThat(result).isNull();
    });
  }

  @Test
  void shouldResolveConstantValueWithLessThanTwentyLevelOfIdentifier() {
    var body = parseStatements(buildConsecutiveIdentifierAssignment(15));

    TreeContext ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, body);

    var lastVariableDeclaration = (VariableDeclarationTree) body.statementOrExpressions().get(body.statementOrExpressions().size() - 1);
    var lastIdentifier = lastVariableDeclaration.identifiers().get(0);
    var value = ConstantResolution.resolveAsStringConstant(lastIdentifier);
    assertThat(value).isEqualTo("bob");
  }

  @Test
  void shouldNotResolveConstantValueWithMoreThanTwentyLevelOfIdentifier() {
    var body = parseStatements(buildConsecutiveIdentifierAssignment(25));

    TreeContext ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, body);

    var lastVariableDeclaration = (VariableDeclarationTree) body.statementOrExpressions().get(body.statementOrExpressions().size() - 1);
    var lastIdentifier = lastVariableDeclaration.identifiers().get(0);
    var value = ConstantResolution.resolveAsStringConstant(lastIdentifier);
    assertThat(value).isNull();
  }

  private static String buildConsecutiveIdentifierAssignment(int amountOfIdentifier) {
    StringBuilder sb = new StringBuilder("var x0 = \"bob\"\n");
    for (int i = 0; i < amountOfIdentifier; i++) {
      sb.append(("var x%d = x%d\n").formatted(i + 1, i));
    }
    return sb.toString();
  }

  @Test
  void functionParametersShouldNotBeConsideredEffectivelyFinalAndResolved() {
    var topLevelTree = ParseUtils.parseFile("""
      package main
      func main(a string) {
        a = "value"
      }
      """);

    TreeContext ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, topLevelTree);

    var mainFunc = (FunctionDeclarationTree) topLevelTree.declarations().get(1);
    var assignementA = (AssignmentExpressionTree) mainFunc.body().statementOrExpressions().get(0);
    var valueA = ConstantResolution.resolveAsStringConstant(assignementA.leftHandSide());
    assertThat(valueA).isNull();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    2*2,4
    2 + 3 * 2,8
    (2 + 3) * 2,10
    2*x,null
    x*2,null
    x*y,null
    true & false,null
    2 / 5,0
    2 - 5,-3
    """, nullValues = {"null"})
  void shouldEvaluateArithmeticExpressions(String expression, @Nullable BigInteger expected) {
    var tree = ((VariableDeclarationTree) parse("x := %s".formatted(expression))).initializers().get(0);

    assertThat(ConstantResolution.evaluateArithmeticExpression(tree)).isEqualTo(expected);
  }

  @Test
  void shouldEvaluateArithmeticExpressionsWithVariables() {
    var tree = parseFile("""
      package main
      func main() {
        var a = 2
        var b = 3
        var x = a + b * 2
      }
      """);
    var ctx = new TreeContext();
    new SymbolVisitor<>().scan(ctx, tree);
    var mainFunc = (FunctionDeclarationTree) tree.declarations().get(1);
    var variableX = (VariableDeclarationTree) mainFunc.body().statementOrExpressions().get(2);
    var treeX = variableX.initializers().get(0);

    assertThat(ConstantResolution.evaluateArithmeticExpression(treeX)).isEqualTo(BigInteger.valueOf(8));
  }
}
