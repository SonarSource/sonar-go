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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.impl.TypeImpl.UNKNOWN_TYPE;

class ArrayTypeTreeImplTest {
  private static final IdentifierTree TYPE_IDENTIFIER = TreeCreationUtils.identifier("byte", "byte");
  private static final TreeMetaData META_DATA = null;

  @Test
  void testArrayType() {
    var index = TreeCreationUtils.integerLiteral("1");
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(META_DATA, index, TYPE_IDENTIFIER);
    assertThat(tree.children()).containsExactly(index, TYPE_IDENTIFIER);
    assertThat(tree.length()).isSameAs(index);
    assertThat(tree.element()).isSameAs(TYPE_IDENTIFIER);
    Type type = tree.type();
    assertThat(type.type()).isEqualTo("[1]byte");
    assertThat(type.packageName()).isEmpty();
  }

  @Test
  void testArrayTypeNoLength() {
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(META_DATA, null, TYPE_IDENTIFIER);
    assertThat(tree.children()).containsExactly(TYPE_IDENTIFIER);
    assertThat(tree.length()).isNull();
    assertThat(tree.element()).isSameAs(TYPE_IDENTIFIER);
    Type type = tree.type();
    assertThat(type.type()).isEqualTo("[]byte");
    assertThat(type.packageName()).isEmpty();
  }

  @Test
  void testArrayTypeEllipsis() {
    var index = new EllipsisTreeImpl(META_DATA, null, null);
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(META_DATA, index, TYPE_IDENTIFIER);
    Type type = tree.type();
    assertThat(type.type()).isEqualTo("[...]byte");
    assertThat(type.packageName()).isEmpty();
  }

  @Test
  void testArrayTypeIdentifier() {
    var size = TreeCreationUtils.identifier("size");
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(META_DATA, size, TYPE_IDENTIFIER);
    Type type = tree.type();
    assertThat(type.type()).isEqualTo("[size]byte");
    assertThat(type.packageName()).isEmpty();
  }

  @Test
  void testUnknownType() {
    var erroneousSize = TreeCreationUtils.block(List.of());
    ArrayTypeTreeImpl tree = new ArrayTypeTreeImpl(META_DATA, erroneousSize, TYPE_IDENTIFIER);
    Type type = tree.type();
    assertThat(type).isEqualTo(UNKNOWN_TYPE);
  }
}
