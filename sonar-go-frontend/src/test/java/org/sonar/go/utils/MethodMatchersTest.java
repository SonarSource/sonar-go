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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.sonar.go.testing.TextRangeGoAssert.assertThat;

class MethodMatchersTest {

  static Stream<Arguments> shouldMatchMethodCallWithPackageName() {
    return Stream.of(
      Arguments.of("rand.Intn()", "Intn", "Intn"),
      Arguments.of("rand.Intn(\"bar\")", "Intn", "Intn"),
      Arguments.of("rand.Intn.foo.bar(\"bar\")", "Intn.foo.bar", "bar"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMatchMethodCallWithPackageName(String code, String methodLookingFor, String methodFound) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames(methodLookingFor)
      .withAnyParameters()
      .build();

    Tree methodCall = parse(code, "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo(methodFound);
  }

  @Test
  void shouldNotMatchForDifferentPackage() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames(Set.of("Intn", "Int63", "Int63n"))
      .withAnyParameters()
      .build();

    Tree methodCall = parse("other.Int63()", "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWithUnrelatedImports() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("sonar.foo()", "something/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchWithMultipleMethodNamesCollection() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames(Set.of("Intn", "Int63", "Int63n"))
      .withAnyParameters()
      .build();

    Tree methodCall = parse("rand.Int63()", "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("Int63");
  }

  @Test
  void shouldMatchWithMultipleMethodNamesVararg() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn", "Int63", "Int63n")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("rand.Int63()", "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("Int63");
  }

  @Test
  void shouldMatchWithParameterPredicate() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .withParameters(p -> p.size() == 1)
      .build();

    Tree methodCall = parse("rand.Intn(\"bar\")", "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("Intn");
  }

  @Test
  void shouldMatchWithMultiplesParameterPredicateWithOr() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .withParameters(p -> false)
      .withParameters(p -> true)
      .build();

    Tree methodCall = parse("rand.Intn(\"bar\")", "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("Intn");
  }

  @Test
  void shouldNotMatchWithParameterPredicate() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withParameters(p -> p.size() == 7)
      .build();

    Tree methodCall = parse("sonar.foo(\"bar\")", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWithWrongName() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.somethingElse")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("sonar.foo(\"bar\")", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWithWrongType() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/somethingElse")
      .withNames("somethingElse.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("sonar.foo(\"bar\")", "com/somethingElse");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchNestedMemberSelect() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("my.sonar.foo(\"bar\")", "com/somethingElse");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWithWrongPackageName() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("org/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("sonar.foo(\"bar\")", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchOtherNativeNode() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("sonar.foo()", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall.children().get(0));
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWhenTreeIsNull() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Optional<IdentifierTree> matches = matcher.matches(null);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchChainCall() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("a.b.c.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parse("""
      a.foo()
      b.foo()
      c.foo()
      a.b.foo()
      b.c.foo()
      a.b.c.d.foo()
      a.b.c.bar()
      """,
      "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchReceiver() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withReceiver()
      .withNames("a.foo")
      .withAnyParameters()
      .build();

    matcher.setReceiverName("receiver");

    Tree methodCall = parse("receiver.a.foo(\"bar\")", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  static Stream<Arguments> shouldNotMatchReceiver() {
    return Stream.of(
      arguments(null, "receiver.a.foo(\"bar\")"),
      arguments("somethingElse", "receiver.a.foo(\"bar\")"),
      arguments("receiver", "receiver.a.somethingElse(\"bar\")"),
      arguments("receiver", """
        go func() {
          receiver.a.foo()
        }()"""));
  }

  @ParameterizedTest
  @MethodSource
  void shouldNotMatchReceiver(String receiver, String code) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withReceiver()
      .withNames("a.foo")
      .withAnyParameters()
      .build();

    matcher.setReceiverName(receiver);

    Tree methodCall = parse(code, "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchReceiverAndFunction() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withReceiver()
      .withNames("foo")
      .withAnyParameters()
      .build();

    matcher.setReceiverName("receiver");

    Tree methodCall = parse("receiver.foo()", "com/sonar");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "*sql.DB",
    "sql.DB",
    "*sql.Tx",
    "sql.Tx"
  })
  void shouldMatchWithVariableOfTypeInParameters(String type) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("database/sql")
      .withVariableTypeIn("database/sql.DB", "database/sql.Tx")
      .withNames("Query")
      .withAnyParameters()
      .build();

    var topLevelTree = parseFunction("""
       func main(x %s) {
         x.Query()
       }
      """.formatted(type), "database/sql");

    List<IdentifierTree> matches = applyMatcherToAllFunctionInvocation(topLevelTree, matcher);
    assertThat(matches).hasSize(1);
    var matchedIdentifier = matches.get(0);
    assertThat(matchedIdentifier.identifier()).isEqualTo("Query");
    assertThat(matchedIdentifier.textRange()).hasRange(6, 5, 6, 10);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "var x sql.DB",
    "var x sql.DB = sql.Open()",
    "var x sql.DB = 1",
  })
  void shouldMatchWithVariableOfTypeInVariableDeclaration(String type) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("database/sql")
      .withVariableTypeIn("database/sql.DB")
      .withNames("Query")
      .withAnyParameters()
      .build();

    var topLevelTree = parseFunction("""
       func main() {
         %s
         x.Query()
       }
      """.formatted(type), "database/sql");

    List<IdentifierTree> matches = applyMatcherToAllFunctionInvocation(topLevelTree, matcher);
    assertThat(matches).hasSize(1);
    var matchedIdentifier = matches.get(0);
    assertThat(matchedIdentifier.identifier()).isEqualTo("Query");
    assertThat(matchedIdentifier.textRange()).hasRange(7, 5, 7, 10);
  }

  @Test
  void shouldNotMatchWithIdentifierWithoutSymbol() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withVariableTypeIn("sonar.Connection")
      .withNames("Query")
      .withAnyParameters()
      .build();

    var topLevelTree = parseFunction("""
       func main() {
         x.Query()
       }
      """, "com/sonar");

    List<IdentifierTree> matches = applyMatcherToAllFunctionInvocation(topLevelTree, matcher);
    assertThat(matches).isEmpty();
  }

  static Stream<Arguments> testMatchWithParameterValuePredicate() {
    return Stream.of(
      arguments("rand.Intn(\"bar\", 2)", true),
      arguments("rand.Intn(\"bar\", 2, 3)", true),
      arguments("rand.Intn(\"bar\")", false),
      arguments("rand.Intn()", false),
      arguments("rand.Intn(3, 3)", false),
      arguments("rand.Intn(\"bar\", \"baz\")", false));
  }

  @ParameterizedTest
  @MethodSource
  void testMatchWithParameterValuePredicate(String code, boolean shouldMatch) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .withAnyParameters()
      .withParameterAtIndexMatching(0, StringLiteralTree.class::isInstance)
      .withParameterAtIndexMatching(1, IntegerLiteralTree.class::isInstance)
      .build();

    parseAndCheckMatch(matcher, code, shouldMatch);
  }

  static Stream<Arguments> testMatchNumberOfParameter() {
    return Stream.of(
      arguments("rand.Intn(\"bar\", 2)", true),
      arguments("rand.Intn(\"bar\", 2, 3)", false),
      arguments("rand.Intn(\"bar\")", false),
      arguments("rand.Intn()", false),
      arguments("rand.Intn(3, 3)", true),
      arguments("rand.Intn(\"bar\", \"baz\")", true));
  }

  @ParameterizedTest
  @MethodSource
  void testMatchNumberOfParameter(String code, boolean shouldMatch) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .withNumberOfParameters(2)
      .build();

    parseAndCheckMatch(matcher, code, shouldMatch);
  }

  // Currently we fail all matches because we don't have yet the detection of types
  static Stream<Arguments> testMatchNumberOfParameterWithAdditionalTypePredicate() {
    return Stream.of(
      arguments("rand.Intn(\"bar\", 2)", true),
      arguments("rand.Intn(\"bar\", 2, 3)", false),
      arguments("rand.Intn(\"bar\")", false),
      arguments("rand.Intn()", false),
      arguments("rand.Intn(3, 3)", true),
      arguments("rand.Intn(\"bar\", \"baz\")", true));
  }

  @ParameterizedTest
  @MethodSource
  void testMatchNumberOfParameterWithAdditionalTypePredicate(String code, boolean shouldMatch) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .withNumberOfParameters(2)
      .withParameters(listType -> listType.size() >= 2 && listType.get(0).equals("string") && listType.get(0).equals("int"))
      .build();

    parseAndCheckMatch(matcher, code, shouldMatch);
  }

  @ParameterizedTest
  @CsvSource({
    "rand.Intn(), true",
    "rand.Int63n(), true",
    "rand.Read(), false",
    "rand.Uint32(), false"
  })
  void testMatchNamesByPredicate(String code, boolean shouldMatch) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNamesMatching(functionName -> functionName.startsWith("Int"))
      .withAnyParameters()
      .build();

    parseAndCheckMatch(matcher, code, shouldMatch);
  }

  static Stream<Arguments> shouldMatchMethodWithDIfferentKindOfImport() {
    return Stream.of(
      arguments("math/rand", "import . \"math/rand\"", "Intn", "Intn()"),
      arguments("crypto", "import . \"crypto\"", "Hash.New", "Hash.New()"),
      arguments("crypto", "import \"crypto\"", "Hash.New", "crypto.Hash.New()"),
      arguments("crypto", "import c \"crypto\"", "Hash.New", "c.Hash.New()"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMatchMethodWithDIfferentKindOfImport(String type, String importInstruction, String methodName, String methodCallInstruction) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType(type)
      .withNames(methodName)
      .withAnyParameters()
      .build();

    var functionInvocation = parseCode("""
      package main
      %s
      func main() {
        %s
      }
      """.formatted(importInstruction, methodCallInstruction));

    var result = matcher.matches(functionInvocation);
    assertThat(result).isNotEmpty();
  }

  @Test
  void shouldNotMatchWithReceiverWhenReceiverIsNotSetForWildcardImport() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withReceiver()
      .withNames("Int")
      .withAnyParameters()
      .build();

    var topLevelTree = parseCodeAndReturnTopLevelTree("""
      package main
      import (
        . "math/rand"
      )
      func main() {
        num := Int()
      }
      """);

    var matches = applyMatcherToAllFunctionInvocation(topLevelTree, matcher);
    assertThat(matches).isEmpty();
  }

  private static void parseAndCheckMatch(MethodMatchers matcher, String code, boolean shouldMatch) {
    Tree methodCall = parse(code, "math/rand");

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    if (shouldMatch) {
      assertThat(matches).isPresent();
    } else {
      assertThat(matches).isEmpty();
    }
  }

  public static TopLevelTree parseFunction(String functionCode, String importedType) {
    var topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main

      import("%s")

      %s
      """.formatted(importedType, functionCode));
    new SymbolVisitor<>().scan(mock(), topLevelTree);
    return topLevelTree;
  }

  public static Tree parse(String code, String importedType) {
    return parseCode("""
      package main

      import("%s")

      func main() {
        %s
      }
      """.formatted(importedType, code));
  }

  private static Tree parseCode(String wholeCode) {
    var topLevelTree = parseCodeAndReturnTopLevelTree(wholeCode);
    var mainFunc = topLevelTree.declarations().get(2);
    BlockTree mainBlock = (BlockTree) mainFunc.children().get(1);
    return mainBlock.statementOrExpressions().get(0).children().get(0);
  }

  private static TopLevelTree parseCodeAndReturnTopLevelTree(String wholeCode) {
    TopLevelTree topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse(wholeCode);
    new SymbolVisitor<>().scan(mock(), topLevelTree);
    return topLevelTree;
  }

  private static List<IdentifierTree> applyMatcherToAllFunctionInvocation(Tree tree, MethodMatchers matcher) {
    var functionCalls = new ArrayList<FunctionInvocationTree>();
    var methodVisiter = new TreeVisitor<>();
    methodVisiter.register(FunctionInvocationTree.class, (ctx, functionInvocationTree) -> functionCalls.add(functionInvocationTree));
    methodVisiter.scan(mock(), tree);
    return functionCalls.stream()
      .map(matcher::matches)
      .flatMap(Optional::stream)
      .toList();
  }
}
