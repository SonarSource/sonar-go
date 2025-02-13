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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.KeyValueTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeLiteralTest {

  @Test
  void shouldParseEmptyCompositeLiteral() {
    var compositeLiteralOptional = CompositeLiteral.of(parseCompositeLiteral("Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    assertThat(compositeLiteral.elements()).isEmpty();
    assertThat(compositeLiteral.getKeyValuesElements()).isEmpty();
  }

  @Test
  void shouldParseCompositeLiteralWithKeyValues() {
    var compositeLiteralOptional = CompositeLiteral.of(parseCompositeLiteral("Composite{ Key: \"value\" }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

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
    var compositeLiteralOptional = CompositeLiteral.of(parseCompositeLiteral("Composite{ Key: \"value\", Key2: \"value2\" }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

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
    var element1 = (CompositeLiteralTree) compositeLiteral.elements().get(0);
    assertThat(element1.type()).isNull();
    assertThat(element1.elements()).hasSize(2);
    var element2 = (CompositeLiteralTree) compositeLiteral.elements().get(1);
    assertThat(element2.type()).isNull();
    assertThat(element2.elements()).hasSize(2);
  }

  @Test
  void shouldBePossibleToTestType() {
    var compositeLiteralOptional = CompositeLiteral.of(parseCompositeLiteral("sonar.Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isTrue();
    assertThat(compositeLiteral.hasType("notSonar", "Composite")).isFalse();
    assertThat(compositeLiteral.hasType("sonar", "notComposite")).isFalse();
  }

  @Test
  void cannotTestNestedMemberSelect() {
    var compositeLiteralOptional = CompositeLiteral.of(parseCompositeLiteral("com.sonar.Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isFalse();
  }

  public static CompositeLiteralTree parseCompositeLiteral(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main
      func main() {
      """ + code + """
      }
      """);
    var mainFunc = topLevelTree.declarations().get(1);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    var expressionStatement = (NativeTree) mainBlock.statementOrExpressions().get(0);
    return (CompositeLiteralTree) expressionStatement.children().get(0);
  }
}
