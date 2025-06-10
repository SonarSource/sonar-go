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
package org.sonar.go.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.CompositeLiteralTree;
import org.sonar.plugins.go.api.ExpressionStatementTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.KeyValueTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeLiteralTreeImplTest {

  @Test
  void testSimpleCompositeLiteral() {
    TreeMetaData meta = null;
    Tree identifierTree = TreeCreationUtils.identifier("x", "y");
    List<Tree> elements = new ArrayList<>();

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, identifierTree, elements);
    assertThat(tree.children()).containsExactly(identifierTree);
    assertThat(tree.type()).isEqualTo(identifierTree);
    assertThat(tree.elements()).isNotNull();
    assertThat(tree.elements()).isEmpty();
  }

  @Test
  void testCompositeLiteralWithElements() {
    TreeMetaData meta = null;
    Tree identifierTree = TreeCreationUtils.identifier("x", "y");
    Tree el1 = TreeCreationUtils.identifier("x", "y");
    Tree el2 = new LiteralTreeImpl(meta, "x");
    List<Tree> elements = new ArrayList<>();
    elements.add(el1);
    elements.add(el2);

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, identifierTree, elements);
    assertThat(tree.children()).containsExactly(identifierTree, el1, el2);
    assertThat(tree.type()).isEqualTo(identifierTree);
    assertThat(tree.elements()).isNotNull();
    assertThat(tree.elements()).hasSize(2);
    assertThat(tree.elements().get(0)).isEqualTo(el1);
    assertThat(tree.elements().get(1)).isEqualTo(el2);
  }

  @Test
  void testCompositeLiteralWithNullType() {
    TreeMetaData meta = null;
    List<Tree> elements = new ArrayList<>();

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, null, elements);
    assertThat(tree.children()).isEmpty();
    assertThat(tree.type()).isNull();
  }

  @Test
  void shouldParseEmptyCompositeLiteral() {
    var compositeLiteral = parseCompositeLiteral("Composite{ }");
    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    assertThat(compositeLiteral.elements()).isEmpty();
    assertThat(compositeLiteral.getKeyValuesElements()).isEmpty();
  }

  @Test
  void shouldParseCompositeLiteralWithKeyValues() {
    var compositeLiteral = parseCompositeLiteral("Composite{ Key: \"value\" }");

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    List<Tree> elements = compositeLiteral.elements();
    assertThat(elements).hasSize(1);
    assertThat(elements.get(0)).isInstanceOf(KeyValueTree.class);
    Optional<KeyValueTree> keyValuesElements = compositeLiteral.getKeyValuesElements().findFirst();
    assertThat(keyValuesElements).isPresent();
    KeyValueTree keyValue = keyValuesElements.get();
    assertThat(keyValue.key()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Key"));
    assertThat(keyValue.value()).isInstanceOfSatisfying(StringLiteralTree.class, stringLiteral -> assertThat(stringLiteral.content()).isEqualTo("value"));
  }

  @Test
  void shouldParseCompositeLiteralWithMultipleKeyValues() {
    var compositeLiteral = parseCompositeLiteral("Composite{ Key: \"value\", Key2: \"value2\" }");

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    assertThat(compositeLiteral.elements()).hasSize(2);
    assertThat(compositeLiteral.getKeyValuesElements()).hasSize(2);
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isFalse();
  }

  @Test
  void shouldParseInnerCompositeLiteralWithoutType() {
    var compositeLiteral = parseCompositeLiteral("""
      []map[string]int{
          {"one": 1, "two": 2},
          {"three": 3, "four": 4},
      }""");
    assertThat(compositeLiteral.type()).isNotNull();
    assertThat(compositeLiteral.elements()).hasSize(2);
    assertThat(compositeLiteral.getKeyValuesElements()).isEmpty();

    var element1 = (CompositeLiteralTree) compositeLiteral.elements().get(0);
    assertThat(element1.type()).isNull();
    assertThat(element1.elements()).hasSize(2);
    var subElements1 = element1.getKeyValuesElements().toList();
    assertThat(subElements1).hasSize(2);
    assertThat(subElements1.get(0).key()).isInstanceOfSatisfying(StringLiteralTree.class, string -> assertThat(string.content()).isEqualTo("one"));
    assertThat(subElements1.get(0).value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.getIntegerValue()).isEqualTo(1));
    assertThat(subElements1.get(1).key()).isInstanceOfSatisfying(StringLiteralTree.class, string -> assertThat(string.content()).isEqualTo("two"));
    assertThat(subElements1.get(1).value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.getIntegerValue()).isEqualTo(2));

    var element2 = (CompositeLiteralTree) compositeLiteral.elements().get(1);
    assertThat(element2.type()).isNull();
    assertThat(element2.elements()).hasSize(2);
    var subElements2 = element2.getKeyValuesElements().toList();
    assertThat(subElements2).hasSize(2);
    assertThat(subElements2.get(0).key()).isInstanceOfSatisfying(StringLiteralTree.class, string -> assertThat(string.content()).isEqualTo("three"));
    assertThat(subElements2.get(0).value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.getIntegerValue()).isEqualTo(3));
    assertThat(subElements2.get(1).key()).isInstanceOfSatisfying(StringLiteralTree.class, string -> assertThat(string.content()).isEqualTo("four"));
    assertThat(subElements2.get(1).value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.getIntegerValue()).isEqualTo(4));
  }

  static Stream<Arguments> shouldMatchType() {
    return Stream.of(
      Arguments.of("http.Server{ }", "import \"net/http\""),
      Arguments.of("Server{ }", "import . \"net/http\""));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMatchType(String code, String importInstruction) {

    var topLevelTree = (TopLevelTree) TestGoConverter.parse("""
      package main

      %s

      func main() {
      %s
      }
      """.formatted(importInstruction, code));
    var mainFunc = topLevelTree.declarations().get(2);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    var expressionStatement = (ExpressionStatementTree) mainBlock.statementOrExpressions().get(0);
    var compositeLiteral = (CompositeLiteralTree) expressionStatement.expression();

    assertThat(compositeLiteral.hasType("net/http", "net/http.Server")).isTrue();
    assertThat(compositeLiteral.hasType("http", "net/http.Server")).isFalse();
    assertThat(compositeLiteral.hasType("net/http", "Server")).isFalse();
    assertThat(compositeLiteral.hasType("http", "Server")).isFalse();
    assertThat(compositeLiteral.hasType("", "")).isFalse();
  }

  @Test
  void cannotTestNestedMemberSelect() {
    var compositeLiteral = parseCompositeLiteral("com.sonar.Composite{ }");
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isFalse();
    assertThat(compositeLiteral.hasType("com.sonar", "Composite")).isFalse();
    assertThat(compositeLiteral.hasType("com", "sonar.Composite")).isFalse();
    assertThat(compositeLiteral.hasType("com", "Composite")).isFalse();
  }

  public static CompositeLiteralTree parseCompositeLiteral(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverter.parse("""
      package main

      import "net/http"

      func main() {
      %s
      }
      """.formatted(code));
    var mainFunc = topLevelTree.declarations().get(2);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    var expressionStatement = (ExpressionStatementTree) mainBlock.statementOrExpressions().get(0);
    return (CompositeLiteralTree) expressionStatement.expression();
  }
}
