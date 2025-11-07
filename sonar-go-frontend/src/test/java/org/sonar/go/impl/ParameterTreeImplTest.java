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
package org.sonar.go.impl;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class ParameterTreeImplTest {

  private class TypeNativeKind implements NativeKind {
  }

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree parameterType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = TreeCreationUtils.identifier("x");
    IdentifierTree identifierTreeY = TreeCreationUtils.identifier("y");
    ParameterTreeImpl parameterTreeX = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXCopy = new ParameterTreeImpl(meta, TreeCreationUtils.identifier("x"), null);
    ParameterTreeImpl parameterTreeXTyped = new ParameterTreeImpl(meta, identifierTreeX, parameterType);
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, parameterType);

    assertThat(parameterTreeXTyped.children()).hasSize(2);
    assertThat(parameterTreeX.children()).hasSize(1);
    assertThat(parameterTreeX.typeTree()).isNull();
    assertThat(parameterTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(parameterTreeX, parameterTreeXCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeX, parameterTreeXTyped)).isFalse();
    assertThat(areEquivalent(parameterTreeX, parameterTreeY)).isFalse();
    assertThat(areEquivalent(parameterTreeXTyped, parameterTreeY)).isFalse();
  }

  @Test
  void test_default_value() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = TreeCreationUtils.identifier("x");
    IdentifierTree identifierTreeY = TreeCreationUtils.identifier("y");
    ParameterTreeImpl parameterTreeXDefault1 = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXDefault1Copy = new ParameterTreeImpl(meta, TreeCreationUtils.identifier("x"), null);
    ParameterTreeImpl parameterTreeXDefault2 = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXDefaultNative = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, null);

    assertThat(parameterTreeXDefault1.children()).hasSize(1);
    assertThat(parameterTreeXDefault2.children()).hasSize(1);
    assertThat(parameterTreeXDefaultNative.children()).hasSize(1);
    assertThat(parameterTreeY.children()).hasSize(1);

    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault1Copy)).isTrue();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault2)).isTrue();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefaultNative)).isTrue();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeY)).isFalse();
  }

  @Test
  void test_modifiers() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = TreeCreationUtils.identifier("x");
    IdentifierTree identifierTreeY = TreeCreationUtils.identifier("y");

    ParameterTreeImpl parameterTreeXPublic = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXPublicCopy = new ParameterTreeImpl(meta, TreeCreationUtils.identifier("x"), null);
    ParameterTreeImpl parameterTreeXPrivate = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXNative = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeNoMod = new ParameterTreeImpl(meta, identifierTreeY, null);

    assertThat(parameterTreeXPublic.children()).hasSize(1);
    assertThat(parameterTreeXPrivate.children()).hasSize(1);
    assertThat(parameterTreeXNative.children()).hasSize(1);
    assertThat(parameterTreeNoMod.children()).hasSize(1);

    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPublicCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPrivate)).isTrue();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXNative)).isTrue();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeNoMod)).isFalse();
  }

  static Stream<Arguments> shouldVerifyType() {
    return Stream.of(
      arguments("a int", of("int")),
      arguments("a ...int", of("...int")),
      arguments("req *http.Request", of("*net/http.Request")),
      arguments("w http.ResponseWriter, req *http.Request", of("net/http.ResponseWriter", "*net/http.Request")),
      arguments("c *fiber.Ctx", of("*github.com/gofiber/fiber/v2.Ctx")),
      arguments("c fiber.Ctx", of("github.com/gofiber/fiber/v2.Ctx")),
      arguments("c ...*fiber.Ctx", of("...*github.com/gofiber/fiber/v2.Ctx")),
      arguments("a any", of("any"))
    // TODO SONARGO-511 Unnamed parameters should be mapped to "ParameterTree"
    // arguments("int", of("int")),
    // arguments("any", of("any"))
    );
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifyType(String params, List<String> expectedParameters) {
    var code = """
      package main

      import (
        "net/http"
        "github.com/gofiber/fiber/v2"
      )

      func foo(%s) {
      }""".formatted(params);
    var tree = (TopLevelTree) TestGoConverterSingleFile.parse(code);

    var functionTree = tree.descendants()
      .filter(FunctionDeclarationTree.class::isInstance)
      .map(FunctionDeclarationTree.class::cast)
      .findFirst()
      .get();

    SoftAssertions.assertSoftly(softly -> {
      int i = 0;
      for (String expectedParameter : expectedParameters) {
        var parameterType = (ParameterTree) functionTree.formalParameters().get(i);
        softly.assertThat(parameterType.type().type()).isEqualTo(expectedParameter);
        i++;
      }
    });
  }

  // artificial cases for coverage
  @Test
  void shouldReturnUnknownTypeForNotEllipsisTree() {
    Tree typeTree = new NativeTreeImpl(null, new StringNativeKind("fake"), List.of());
    var parameter = new ParameterTreeImpl(null, null, typeTree);

    var parameterType = parameter.type();

    assertThat(parameterType.type()).isEqualTo("UNKNOWN");
    assertThat(parameterType.packageName()).isEqualTo("UNKNOWN");
  }

  @Test
  void shouldReturnUnknownTypeForNonNativeTree() {
    Tree typeTree = new VariableDeclarationTreeImpl(null, List.of(), null, List.of(), true);
    var parameter = new ParameterTreeImpl(null, null, typeTree);

    var parameterType = parameter.type();

    assertThat(parameterType.type()).isEqualTo("UNKNOWN");
    assertThat(parameterType.packageName()).isEqualTo("UNKNOWN");
  }

  @Test
  void shouldReturnUnknownTypeForNotStringNativeKind() {
    Tree varDecl = new VariableDeclarationTreeImpl(null, List.of(), null, List.of(), true);
    Tree typeTree = new NativeTreeImpl(null, new StringNativeKind("Type(Ellipsis)"), List.of(varDecl));
    var parameter = new ParameterTreeImpl(null, null, typeTree);

    var parameterType = parameter.type();

    assertThat(parameterType.type()).isEqualTo("UNKNOWN");
    assertThat(parameterType.packageName()).isEqualTo("UNKNOWN");
  }

  class FakeNativeKind implements NativeKind {
  }
}
