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
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayTypeTreeImplTest {

  @Test
  void testArrayType() {
    TreeMetaData meta = null;
    var identifier = TreeCreationUtils.identifier("byte");
    var index = TreeCreationUtils.integerLiteral("1");
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(meta, index, identifier);
    assertThat(tree.children()).containsExactly(index, identifier);
    assertThat(tree.length()).isSameAs(index);
    assertThat(tree.element()).isSameAs(identifier);
  }

  @Test
  void testArrayTypeNoLength() {
    TreeMetaData meta = null;
    var identifier = TreeCreationUtils.identifier("byte");
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(meta, null, identifier);
    assertThat(tree.children()).containsExactly(identifier);
    assertThat(tree.length()).isNull();
    assertThat(tree.element()).isSameAs(identifier);
  }
}
