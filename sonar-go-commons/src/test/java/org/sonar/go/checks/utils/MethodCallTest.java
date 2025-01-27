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
package org.sonar.go.checks.utils;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;

class MethodCallTest {

  @Test
  void shouldParseSimpleMethodCall() {
    var methodCall = MethodCall.of((NativeTree) parse("foo()"));
    assertThat(methodCall).isNotNull();
    assertThat(methodCall.methodFqn()).isEqualTo("foo");
    assertThat(methodCall.args()).isEmpty();
    assertThat(methodCall.is("foo")).isTrue();
  }

  @Test
  void shouldParseMethodCallWithPackageName() {
    var methodCall = MethodCall.of((NativeTree) parse("com.sonar.foo()"));
    assertThat(methodCall).isNotNull();
    assertThat(methodCall.methodFqn()).isEqualTo("com.sonar.foo");
    assertThat(methodCall.args()).isEmpty();
    assertThat(methodCall.is("com.sonar.foo")).isTrue();
  }

  @Test
  void shouldParseMethodCallWithArguments() {
    var methodCall = MethodCall.of((NativeTree) parse("foo(bar.test, again)"));
    assertThat(methodCall).isNotNull();
    assertThat(methodCall.methodFqn()).isEqualTo("foo");
    assertThat(methodCall.args()).isEqualTo(List.of("bar.test", "again"));
    assertThat(methodCall.is("foo")).isTrue();
  }

  @Test
  void testIsMethod() {
    var methodCall = MethodCall.of((NativeTree) parse("foo(bar.test, again)"));
    assertThat(methodCall).isNotNull();
    assertThat(methodCall.methodFqn()).isEqualTo("foo");
    assertThat(methodCall.args()).isEqualTo(List.of("bar.test", "again"));
    assertThat(methodCall.is("foo")).isTrue();
    assertThat(methodCall.is("foo", "bar.test")).isTrue();
    assertThat(methodCall.is("foo", "bar.test", "again")).isTrue();
    assertThat(methodCall.is("foo", "again")).isFalse();
    assertThat(methodCall.is("foo", "bar.test", "again", "unkknown")).isFalse();
  }

  private Tree parse(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main
      func main() {
      """ + code + """
      }
      """);
    var mainFunc = topLevelTree.declarations().get(1);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    return mainBlock.statementOrExpressions().get(0);
  }
}
