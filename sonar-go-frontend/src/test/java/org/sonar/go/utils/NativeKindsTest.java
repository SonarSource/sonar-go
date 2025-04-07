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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.TreeMetaData;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.go.utils.NativeKinds.isCompositeLit;
import static org.sonar.go.utils.NativeKinds.isKeyValueExpr;

class NativeKindsTest {
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
  void shouldNotBeStringNativeKindWhenAnotherNativeKind() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKind(tree, s -> s.endsWith("A"));
    assertThat(result).isFalse();
  }

  @Test
  void shouldNotBeStringNativeKindWhenTreeIsNull() {
    var result = NativeKinds.isStringNativeKind(null, s -> s.endsWith("A"));
    assertThat(result).isFalse();
  }

  @Test
  void shouldReturnMethodReceiver() {
    var tree = (TopLevelTree) TestGoConverter.parse("""
      package main
      func (ctrl *MyController) users() {}
      """);
    var methodReceiver = tree.descendants()
      .filter(NativeKinds::isMethodReceiverTreeIdentifier)
      .findFirst()
      .get();
    assertThat(((IdentifierTree) methodReceiver.children().get(0)).name()).isEqualTo("ctrl");
  }

  @Test
  void shouldNotFindMethodReceiver() {
    var tree = (TopLevelTree) TestGoConverter.parse("""
      package main
      func users() {}
      """);
    var methodReceiver = tree.descendants()
      .filter(NativeKinds::isMethodReceiverTreeIdentifier)
      .findFirst();
    assertThat(methodReceiver).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    (CompositeLit),true
    (Literal),false
    """)
  void shouldFindCompositeLit(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), new StringNativeKind(kind), emptyList());
    assertThat(isCompositeLit(tree)).isEqualTo(shouldFind);
  }

  @Test
  void shouldNotFindCompositeLitForUnrelatedNativeNode() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    assertThat(isCompositeLit(tree)).isFalse();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    (KeyValueExpr),true
    (Expr),false
    """)
  void shouldFindKeyValueExpr(String kind, boolean shouldFind) {
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), new StringNativeKind(kind), emptyList());
    assertThat(isKeyValueExpr(tree)).isEqualTo(shouldFind);
  }

  @Test
  void shouldNotFindKeyValueExprForUnrelatedNativeNode() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    assertThat(isKeyValueExpr(tree)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Type([]Subtype)",
    "Type([12]Subtype)",
  })
  void shouldFindStringNativeKind(String stringKind) {
    var kind = new StringNativeKind(stringKind);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "Type", "Subtype");
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Other([]Subtype)",
    "Type([]Other)",
    "Other([]Other)",
  })
  void shouldNotFindStringNativeKindWithDifferentTypeSubtype(String stringKind) {
    var kind = new StringNativeKind(stringKind);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "Type", "Subtype");
    assertThat(result).isFalse();
  }

  @Test
  void shouldNotFindStringNativeKind() {
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), mock(), List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "Type", "Subtype");
    assertThat(result).isFalse();
  }
}
