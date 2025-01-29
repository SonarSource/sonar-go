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
package org.sonar.go.checks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.go.checks.NativeKinds.isFromCallExpr;
import static org.sonar.go.checks.NativeKinds.isFromSelectorExpr;
import static org.sonar.go.checks.NativeKinds.isFun;
import static org.sonar.go.checks.NativeKinds.isX;

class NativeKindsTest {
  @ParameterizedTest
  @CsvSource(textBlock = """
    X(CallExpr),true
    X(SelectorExpr),true
    Fun(SelectorExpr),false
    """)
  void shouldFindXKinds(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(null, new StringNativeKind(kind), emptyList());
    assertThat(isX(tree)).isEqualTo(shouldFind);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    X(CallExpr),false
    X(SelectorExpr),false
    Fun(SelectorExpr),true
    """)
  void shouldFindFunKinds(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(null, new StringNativeKind(kind), emptyList());
    assertThat(isFun(tree)).isEqualTo(shouldFind);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    X(CallExpr),false
    X(SelectorExpr),true
    Fun(SelectorExpr),true
    """)
  void shouldFindSelectorSubExpr(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(null, new StringNativeKind(kind), emptyList());
    assertThat(isFromSelectorExpr(tree)).isEqualTo(shouldFind);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    X(CallExpr),true
    X(SelectorExpr),false
    Fun(SelectorExpr),false
    """)
  void shouldFindCallSubExpr(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(null, new StringNativeKind(kind), emptyList());
    assertThat(isFromCallExpr(tree)).isEqualTo(shouldFind);
  }

  @Test
  void shouldBeStringNativeKindOfTypeA() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "TypeA");
    assertThat(result).isTrue();
  }

  @Test
  void shouldNotBeStringNativeKindOfTypeWhenAnotherType() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "AnotherType");
    assertThat(result).isFalse();
  }

  @Test
  void shouldBeStringNativeKindOfTypeWhenAnotherNativeKind() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "TypeA");
    assertThat(result).isFalse();
  }

  @Test
  void shouldBeStringNativeKindWithPredicateOnA() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKind(tree, s -> s.endsWith("A"));
    assertThat(result).isTrue();
  }

  @Test
  void shouldNotBeStringNativeKindWhenPredicateFalse() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKind(tree, str -> false);
    assertThat(result).isFalse();
  }

  @Test
  void shouldBeStringNativeKindWhenAnotherNativeKind() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKind(tree, s -> s.endsWith("A"));
    assertThat(result).isFalse();
  }

  @Test
  void shouldIdentifyFunctionComingFromStatements() {
    var kind = new StringNativeKind("X(CallExpr)");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isFunctionCall(tree);
    assertThat(result).isTrue();
  }

  @Test
  void shouldIdentifyFunctionComingFromExpression() {
    var kind = new StringNativeKind("[](CallExpr)");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isFunctionCall(tree);
    assertThat(result).isTrue();
  }

  @Test
  void shouldNotBeAFunctionWithUnrelatedNativeKind() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isFunctionCall(tree);
    assertThat(result).isFalse();
  }

  @Test
  void shouldNotBeAFunctionWithUnrelatedKind() {
    var kind = new StringNativeKind("[](Array)");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isFunctionCall(tree);
    assertThat(result).isFalse();
  }
}
