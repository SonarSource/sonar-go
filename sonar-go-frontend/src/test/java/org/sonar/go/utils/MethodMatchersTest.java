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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MethodMatchersTest {

  @Test
  void shouldMatchMethodCallWithPackageName() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldThrowExceptionWhenMethodCallWithoutPackageName() {
    assertThatIllegalArgumentException().isThrownBy(() -> MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("foo"));
  }

  @Test
  void shouldNotMatchWithUnrelatedImports() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "something/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchWithMultipleMethodNamesCollection() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames(Set.of("sonar.foo", "sonar.bar", "sonar.baz"))
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.bar()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("bar");
  }

  @Test
  void shouldMatchWithMultipleMethodNamesVararg() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo", "sonar.bar", "sonar.baz")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.bar()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("bar");
  }

  @Test
  void shouldMatchWithMultipleMethodNamesWithPrefix() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withPrefixAndNames("sonar", "foo", "bar", "baz")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.bar()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("bar");
  }

  @Test
  void shouldMatchWithParameterPredicate() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withParameters(p -> p.size() == 1)
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldMatchWithMultiplesParameterPredicateWithOr() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withParameters(p -> false)
      .withParameters(p -> true)
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldNotMatchWithParameterPredicate() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withParameters(p -> p.size() == 7)
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/sonar", matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/sonar", matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/somethingElse", matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher("my.sonar.foo(\"bar\")", "com/somethingElse", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldNotMatchWithWrongPackageName() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("sonar/foo")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchSingleImport() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo(\"bar\")", "sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldNotMatchOtherNativeNode() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("sonar.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall.children().get(0));
    assertThat(matches).isEmpty();
  }

  public static Tree parseAndFeedImportsToMatcher(String code, String importedType, MethodMatchers matcher) {
    TopLevelTree topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main

      import("%s")

      func main() {
        %s
      }
      """.formatted(importedType, code));
    matcher.validateTypeInTree(topLevelTree);
    var mainFunc = topLevelTree.declarations().get(2);
    BlockTree mainBlock = (BlockTree) mainFunc.children().get(1);
    return mainBlock.statementOrExpressions().get(0).children().get(0);
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
  void shouldMatchChainCall() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("a.b.c.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("a.b.c.foo(\"bar\")", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldNotMatchChainCall() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames("a.b.c.foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("""
      a.foo()
      b.foo()
      c.foo()
      a.b.foo()
      b.c.foo()
      a.b.c.d.foo()
      a.b.c.bar()
      """,
      "com/sonar",
      matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher("receiver.a.foo(\"bar\")", "com/sonar", matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher(code, "com/sonar", matcher);

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

    Tree methodCall = parseAndFeedImportsToMatcher("receiver.foo()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }
}
