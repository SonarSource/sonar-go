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

import java.util.ArrayList;
import java.util.Comparator;
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
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.sonar.go.testing.TextRangeGoAssert.assertThat;

class MethodMatchersTest {

  static Stream<Arguments> shouldMatchMethodCallWithPackageName() {
    return of(
      Arguments.of("rand.Intn()", "Intn", "Intn"),
      Arguments.of("rand.Intn(\"bar\")", "Intn", "Intn"));
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
  void shouldMatchOnMethodChain() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        GetRandObjLocal().Intn(42)
      }

      func GetRandObjLocal() *rand.Rand {
        return rand.New(rand.NewSource(42))
      }
      """);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isNotEmpty();
  }

  @Test
  void shouldNotMatchOnMethodChainWhenNameIsWrongButVariableTypeInCorrect() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        GetRandObjLocal().IntX(42)
      }

      func GetRandObjLocal() *rand.Rand {
        return rand.New(rand.NewSource(42))
      }
      """);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchOnMethodChainWhenMethodReturnsTuple() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        GetRandObjLocal().Intn(42)
      }

      func GetRandObjLocal() (*rand.Rand, int) {
        return rand.New(rand.NewSource(42)), 42
      }
      """);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchOnMultipleMethodChain() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofTypes(List.of("math/rand"))
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        first().second().Intn(42)
      }

      func first() *B {
        return &B{}
      }

      type B struct {
      }

      func (b *B) second() *rand.Rand {
        return rand.New(rand.NewSource(42))
      }
      """);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isNotEmpty();
  }

  static Stream<Arguments> shouldNotMatchOnIrrelevantMultipleMethodChain() {
    return of(
      arguments("""
        // returned type doesn't match
        package main
        import "math/rand"
        func main() {
          first().second().Intn(42)
        }

        func first() *B {
          return &B{}
        }

        type B struct {
        }

        // This method returns an int, not a *rand.Rand
        func (b *B) second() int {
          return 42
        }
        """, """
        // invalid method name
        package main
        import "math/rand"
        func main() {
          // This method doesn't exist
          first().second().FunctionThatDoesntExist(42)
        }

        func first() *B {
          return &B{}
        }

        type B struct {
        }

        func (b *B) second() *rand.Rand {
          return rand.New(rand.NewSource(42))
        }
        """, """
        // method returns a tuple
        package main
        import "math/rand"
        func main() {
          // invalid call, first() returns a tuple
          first().second().Intn(42)
        }

        func first() (*B, int) {
          return &B{}, 42
        }

        type B struct {
        }

        func (b *B) second() *rand.Rand {
          return rand.New(rand.NewSource(42))
        }
        """));
  }

  @ParameterizedTest
  @MethodSource
  void shouldNotMatchOnIrrelevantMultipleMethodChain(String code) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofTypes(List.of("math/rand"))
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();
    var methodCall = parseCode(code);
    var matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchFunctionWhenFirstIdentifierIsNotPresent() {
    // This test is for coverage of line `if (optFirstIdentifier.isPresent()) {` in `matchesFunctionInvocation()`
    MethodMatchers matcher = MethodMatchers.create()
      .ofTypes(List.of("math/rand"))
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    Tree memberSelect = new MemberSelectTreeImpl(null, null, null);
    var methodCall = new FunctionInvocationTreeImpl(null, memberSelect, List.of(), List.of());
    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchFunctionWhenFunctionNameTreeIsIdentifierTree() {
    // This test is for coverage of line `} else if (functionNameTree instanceof IdentifierTree identifierTree` in `matchesFunctionInvocation()`
    MethodMatchers matcher = MethodMatchers.create()
      .ofTypes(List.of("math/rand"))
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Int", "Intn")
      .withAnyParameters()
      .build();

    Tree memberSelect = new IdentifierTreeImpl(null, "name", "someType", "packageName", 1);
    var methodCall = new FunctionInvocationTreeImpl(null, memberSelect, List.of(), List.of());
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
    return of(
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

  @Test
  void shouldThrowExceptionWhenSetReceiverButReceiverIsNotDefined() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      // no receiver set here
      .withNames("foo")
      .withAnyParameters()
      .build();

    var throwable = catchThrowable(() -> matcher.setReceiverName("receiver"));
    assertThat(throwable)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Setting receiver name, when MethodMatcher is not configured to expect receiver, doesn't make sense.");
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
    return of(
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
    return of(
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
    return of(
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

  static Stream<Arguments> shouldMatchMethodWithDifferentKindOfImport() {
    return of(
      arguments("math/rand", null, "import . \"math/rand\"", "Intn", "Intn()"),
      arguments("crypto", "crypto.Hash", "import . \"crypto\"", "Hash.New", "Hash.New()"),
      arguments("crypto", "crypto.Hash", "import \"crypto\"", "New", "crypto.Hash.New()"),
      arguments("crypto", "crypto.Hash", "import c \"crypto\"", "New", "c.Hash.New()"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMatchMethodWithDifferentKindOfImport(String type, String variableTypeIn, String importInstruction, String methodName, String methodCallInstruction) {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType(type)
      .withVariableTypeIn(variableTypeIn)
      .withNames(methodName)
      .withAnyParameters()
      .build();

    var code = """
      package main
      %s
      func main() {
        %s
      }
      """.formatted(importInstruction, methodCallInstruction);
    var functionInvocation = parseCode(code);

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

  @Test
  void shouldCreateMatcherWith2ParametersPredicatesAndFirstFails() {
    // To cover lines in withNumberOfParameters()
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Int")
      .withParameters(p -> false)
      .withNumberOfParameters(1)
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        rand.Int(42)
      }
      """);
    var matches = matcher.matches(methodCall);
    assertThat(matches).isNotEmpty();
  }

  @Test
  void shouldCreateMatcherWith2ParametersPredicatesAndFirstFailsNumberOfParameterDoesntMatch() {
    // To cover lines in withNumberOfParameters()
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Int")
      .withParameters(p -> false)
      .withNumberOfParameters(1)
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        rand.Int(42, 42)
      }
      """);
    var matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchWhenNoParametersPredicate() {
    // To cover `if (parametersTypesPredicate == null) {` in build()
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withNames("Intn")
      .build();

    var methodCall = parseCode("""
      package main
      import "math/rand"
      func main() {
        rand.Intn(42)
      }
      """);

    var matches = matcher.matches(methodCall);
    assertThat(matches).isNotEmpty();
  }

  @Test
  void shouldMatchOnNestedReceivers() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("math/rand")
      .withVariableTypeIn("math/rand.Rand")
      .withNames("Intn")
      .build();

    var tree = parseCodeAndReturnTopLevelTree("""
      package main
      import "math/rand"
      func main() {
        getRand().Intn(42) // 1st match
      }

      func getRand() *rand.Rand {
        return rand.New(rand.NewSource(42))
      }

      type Wrapper struct {
        Rand *rand.Rand
      }

      func (w *Wrapper) nextIntn(n int) int {
        return w.Rand.Intn(n) // 2nd match
      }

      type WrappingWrapper struct {
        Inner Wrapper
      }

      func (ww *WrappingWrapper) nextIntn(n int) int {
        return ww.Inner.Rand.Intn(n) // 3rd match
      }

      func wrapperSupplier() WrappingWrapper {
        return WrappingWrapper{Inner: Wrapper{Rand: rand.New(rand.NewSource(42))}}
      }

      func useWrapper() int {
        return wrapperSupplier().Inner.Rand.Intn(42) // 4th match
      }
      """);

    var matches = new ArrayList<IdentifierTree>();
    var visitor = new TreeVisitor<>();
    visitor.register(FunctionInvocationTree.class, (ctx, fn) -> {
      matcher.matches(fn).ifPresent(matches::add);
    });
    visitor.scan(mock(), tree);
    matches.sort(Comparator.comparingInt(t -> t.textRange().start().line()));

    assertThat(matches).hasSize(4);
    assertThat(matches.get(0).metaData().textRange())
      .hasRange(4, 12, 4, 16);
    assertThat(matches.get(1).metaData().textRange())
      .hasRange(16, 16, 16, 20);
    assertThat(matches.get(2).metaData().textRange())
      .hasRange(24, 23, 24, 27);
    assertThat(matches.get(3).metaData().textRange())
      .hasRange(32, 38, 32, 42);
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
    var topLevelTree = (TopLevelTree) TestGoConverterSingleFile.parse("""
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
    TopLevelTree topLevelTree = (TopLevelTree) TestGoConverterSingleFile.parse(wholeCode);
    new SymbolVisitor<>().scan(mock(), topLevelTree);
    return topLevelTree;
  }

  private static List<IdentifierTree> applyMatcherToAllFunctionInvocation(Tree tree, MethodMatchers matcher) {
    var functionCalls = new ArrayList<FunctionInvocationTree>();
    var methodVisitor = new TreeVisitor<>();
    methodVisitor.register(FunctionInvocationTree.class, (ctx, functionInvocationTree) -> functionCalls.add(functionInvocationTree));
    methodVisitor.scan(mock(), tree);
    return functionCalls.stream()
      .map(matcher::matches)
      .flatMap(Optional::stream)
      .toList();
  }
}
