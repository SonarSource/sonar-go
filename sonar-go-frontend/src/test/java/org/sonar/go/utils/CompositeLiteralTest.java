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
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeLiteralTest {

  @Test
  void shouldParseEmptyCompositeLiteral() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    assertThat(compositeLiteral.elements()).isEmpty();
    assertThat(compositeLiteral.getKeyValuesElements()).isEmpty();
  }

  @Test
  void shouldParseCompositeLiteralWithKeyValues() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("Composite{ Key: \"value\" }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    List<Tree> elements = compositeLiteral.elements();
    assertThat(elements).hasSize(1);
    assertThat(elements.get(0)).isInstanceOfSatisfying(NativeTree.class, nativeTree -> assertThat(nativeTree.nativeKind().toString()).contains("KeyValue"));
    Optional<KeyValue> keyValuesElements = compositeLiteral.getKeyValuesElements().findFirst();
    assertThat(keyValuesElements).isPresent();
    KeyValue keyValue = keyValuesElements.get();
    assertThat(keyValue.key()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Key"));
    assertThat(keyValue.value()).isInstanceOfSatisfying(StringLiteralTree.class, stringLiteral -> assertThat(stringLiteral.content()).isEqualTo("value"));
  }

  @Test
  void shouldParseCompositeLiteralWithMultipleKeyValues() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("Composite{ Key: \"value\", Key2: \"value2\" }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();

    assertThat(compositeLiteral.type()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("Composite"));
    // Two key values and one comma.
    assertThat(compositeLiteral.elements()).hasSize(3);
    assertThat(compositeLiteral.getKeyValuesElements()).hasSize(2);
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isFalse();
  }

  @Test
  void shouldNotParseOtherNativeTree() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("1 | 2"));
    assertThat(compositeLiteralOptional).isEmpty();
  }

  @Test
  void shouldBePossibleToTestType() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("sonar.Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isTrue();
    assertThat(compositeLiteral.hasType("notSonar", "Composite")).isFalse();
    assertThat(compositeLiteral.hasType("sonar", "notComposite")).isFalse();
  }

  @Test
  void cannotTestNestedMemberSelect() {
    var compositeLiteralOptional = CompositeLiteral.of((NativeTree) parse("com.sonar.Composite{ }"));
    assertThat(compositeLiteralOptional).isPresent();
    CompositeLiteral compositeLiteral = compositeLiteralOptional.get();
    assertThat(compositeLiteral.hasType("sonar", "Composite")).isFalse();
  }

  public static Tree parse(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main
      func main() {
      """ + code + """
      }
      """);
    var mainFunc = topLevelTree.declarations().get(1);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    var expressionStatement = (NativeTree) mainBlock.statementOrExpressions().get(0);
    return expressionStatement.children().get(0);
  }
}
