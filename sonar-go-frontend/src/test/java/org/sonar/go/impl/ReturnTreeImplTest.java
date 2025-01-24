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

import org.junit.jupiter.api.Test;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class ReturnTreeImplTest {
  @Test
  void test() {
    TreeMetaData meta = null;
    TokenImpl returnKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "return", Token.Type.KEYWORD);
    ReturnTreeImpl returnWithoutValue = new ReturnTreeImpl(meta, returnKeyword, null);

    assertThat(returnWithoutValue.children()).isEmpty();
    assertThat(returnWithoutValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithoutValue.body()).isNull();

    ReturnTreeImpl returnWithValue = new ReturnTreeImpl(meta, returnKeyword, new LiteralTreeImpl(meta, "foo"));
    assertThat(returnWithValue.children()).hasSize(1);
    assertThat(returnWithValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithValue.body()).isInstanceOf(LiteralTree.class);

    assertThat(areEquivalent(returnWithoutValue, new ReturnTreeImpl(meta, returnKeyword, null))).isTrue();
    assertThat(areEquivalent(returnWithoutValue, returnWithValue)).isFalse();

  }
}
