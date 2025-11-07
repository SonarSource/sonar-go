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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.ReturnTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class ReturnTreeImplTest {
  @Test
  void test() {
    TreeMetaData meta = null;
    TokenImpl returnKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "return", Token.Type.KEYWORD);
    ReturnTreeImpl returnWithoutValue = new ReturnTreeImpl(meta, returnKeyword, Collections.emptyList());

    assertThat(returnWithoutValue.children()).isEmpty();
    assertThat(returnWithoutValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithoutValue.expressions()).isEmpty();

    var literal = new LiteralTreeImpl(meta, "foo");
    ReturnTreeImpl returnWithValue = new ReturnTreeImpl(meta, returnKeyword, List.of(literal));
    assertThat(returnWithValue.children()).hasSize(1);
    assertThat(returnWithValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithValue.expressions()).containsExactly(literal);

    assertThat(areEquivalent(returnWithoutValue, new ReturnTreeImpl(meta, returnKeyword, Collections.emptyList()))).isTrue();
    assertThat(areEquivalent(returnWithoutValue, returnWithValue)).isFalse();
  }

  @Test
  void testSimpleEmptyReturn() {
    var returnTree = TestGoConverterSingleFile.parseAndRetrieve(ReturnTree.class, """
      package main

      func foo() {
        return
      }
      """);
    assertThat(returnTree).isNotNull();
    assertThat(returnTree.expressions()).isEmpty();
  }

  @Test
  void testNamedReturn() {
    var returnTree = TestGoConverterSingleFile.parseAndRetrieve(ReturnTree.class, """
      package main

      func foo() (name string) {
        name := "foo"
        return
      }
      """);
    assertThat(returnTree).isNotNull();
    assertThat(returnTree.expressions()).isEmpty();
  }

  @Test
  void testSimpleReturnValue() {
    var returnTree = TestGoConverterSingleFile.parseAndRetrieve(ReturnTree.class, """
      package main

      func foo() {
        return "foo"
      }
      """);
    assertThat(returnTree).isNotNull();
    assertThat(returnTree.expressions()).hasSize(1);
    assertThat(returnTree.expressions().get(0))
      .isInstanceOfSatisfying(StringLiteralTree.class, str -> assertThat(str.content()).isEqualTo("foo"));
  }

  @Test
  void testMultipleReturnValue() {
    var returnTree = TestGoConverterSingleFile.parseAndRetrieve(ReturnTree.class, """
      package main

      func foo() {
        return "foo", 42
      }
      """);
    assertThat(returnTree).isNotNull();
    assertThat(returnTree.expressions()).hasSize(2);
    assertThat(returnTree.expressions().get(0))
      .isInstanceOfSatisfying(StringLiteralTree.class, str -> assertThat(str.content()).isEqualTo("foo"));
    assertThat(returnTree.expressions().get(1))
      .isInstanceOfSatisfying(IntegerLiteralTree.class, number -> assertThat(number.getIntegerValue()).isEqualTo(42));
  }
}
