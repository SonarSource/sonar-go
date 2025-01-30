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
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;

class MethodMatchersTest {

  @Test
  void shouldMatchMethodCallWithPackageName() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withName("foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  @Test
  void shouldNotMatchWithUnrelatedImports() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withName("foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "something/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isEmpty();
  }

  @Test
  void shouldMatchWithMultipleMethodNames() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withNames(Set.of("foo", "bar", "baz"))
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
      .withName("foo")
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
      .withName("foo")
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
      .withName("foo")
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
      .withName("somethingElse")
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
      .withName("foo")
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
      .withName("foo")
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
      .withName("foo")
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
      .withName("foo")
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
      .withName("foo")
      .withAnyParameters()
      .build();

    Tree methodCall = parseAndFeedImportsToMatcher("sonar.foo()", "com/sonar", matcher);

    Optional<IdentifierTree> matches = matcher.matches(methodCall.children().get(0));
    assertThat(matches).isEmpty();
  }

  @Test
  void canProvideImportsDirectly() {
    MethodMatchers matcher = MethodMatchers.create()
      .ofType("com/sonar")
      .withName("foo")
      .withAnyParameters()
      .build();

    matcher.addImports(Set.of("com/sonar"));

    TopLevelTree topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main

      func main() {
        sonar.foo()
      }
      """);

    var mainFunc = topLevelTree.declarations().get(1);
    BlockTree mainBlock = (BlockTree) mainFunc.children().get(1);
    Tree methodCall = mainBlock.statementOrExpressions().get(0).children().get(0);

    Optional<IdentifierTree> matches = matcher.matches(methodCall);
    assertThat(matches).isPresent();
    assertThat(matches.get().name()).isEqualTo("foo");
  }

  public static Tree parseAndFeedImportsToMatcher(String code, String importedType, MethodMatchers matcher) {
    TopLevelTree topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main

      import("%s")

      func main() {
        %s
      }
      """.formatted(importedType, code));
    matcher.addImports(topLevelTree);
    var mainFunc = topLevelTree.declarations().get(2);
    BlockTree mainBlock = (BlockTree) mainFunc.children().get(1);
    return mainBlock.statementOrExpressions().get(0).children().get(0);
  }

}
