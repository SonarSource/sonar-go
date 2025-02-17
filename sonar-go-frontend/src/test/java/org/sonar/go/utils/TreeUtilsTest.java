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
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TreeUtilsTest {

  @Test
  void shouldCheckIsNotSemicolon() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var result = TreeUtils.IS_NOT_SEMICOLON.test(initializer);
    assertThat(result).isTrue();
  }

  @Test
  void shouldCheckIsSemicolon() {
    var initializer = new NativeTreeImpl(mock(TreeMetaData.class), new StringNativeKind("Semicolon"), List.of());
    var result = TreeUtils.IS_NOT_SEMICOLON.test(initializer);
    assertThat(result).isFalse();
  }

  @Test
  void shouldGetOnlyIdentifierNames() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = new IdentifierTreeImpl(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), List.of(id), null, List.of(initializer), false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = new IdentifierTreeImpl(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierNames(List.of(initializer, id, variable, body, id2));

    assertThat(names).containsOnly("foo", "bar");
  }

  @Test
  void shouldGetOnlyIdentifierName() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = new IdentifierTreeImpl(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), List.of(id), null, List.of(initializer), false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = new IdentifierTreeImpl(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierName(List.of(initializer, id, variable, body, id2));

    assertThat(names).isEqualTo("foo.bar");
  }
}
