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
package org.sonar.go.utils;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.*;
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
  void shouldCheckIsNotEmptyNativeTree() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var result = TreeUtils.IS_NOT_EMPTY_NATIVE_TREE.test(initializer);
    assertThat(result).isTrue();
  }

  @Test
  void shouldCheckIsEmptyNativeTree() {
    var initializer = new NativeTreeImpl(mock(TreeMetaData.class), new StringNativeKind(""), List.of());
    var result = TreeUtils.IS_NOT_EMPTY_NATIVE_TREE.test(initializer);
    assertThat(result).isFalse();
  }

  @Test
  void shouldGetOnlyIdentifierNames() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), List.of(id), null, List.of(initializer), false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierNames(List.of(initializer, id, variable, body, id2));

    assertThat(names).containsOnly("foo", "bar");
  }

  @Test
  void shouldGetOnlyIdentifierName() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), List.of(id), null, List.of(initializer), false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierName(List.of(initializer, id, variable, body, id2));

    assertThat(names).isEqualTo("foo.bar");
  }

  @Test
  void shouldRetrieveFirstIdentifierOfSimpleMemberSelect() {
    var prefix = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var suffix = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");
    var memberSelect = TreeCreationUtils.memberSelect(prefix, suffix);
    var result = TreeUtils.retrieveFirstIdentifier(memberSelect);
    assertThat(result).containsSame(prefix);
  }

  @Test
  void shouldRetrieveFirstIdentifierOfComposedMemberSelect() {
    var identifier1 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var identifier2 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");
    var identifier3 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "baz");
    var innerMemberSelect = TreeCreationUtils.memberSelect(identifier1, identifier2);
    var memberSelect = TreeCreationUtils.memberSelect(innerMemberSelect, identifier3);
    var result = TreeUtils.retrieveFirstIdentifier(memberSelect);
    assertThat(result).containsSame(identifier1);
  }

  @Test
  void shouldRetrieveFirstIdentifierOfComposedFunctionCallMemberSelect() {
    var identifier1 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var identifier2 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");
    var functionCall = TreeCreationUtils.simpleFunctionCall(identifier1);
    var memberSelect = TreeCreationUtils.memberSelect(functionCall, identifier2);
    var result = TreeUtils.retrieveFirstIdentifier(memberSelect);
    assertThat(result).containsSame(identifier1);
  }

  @Test
  void shouldGetNullWhenExpressionIsNotIdentifierOrMemberSelectOrFunctionCall() {
    var literal = TreeCreationUtils.literal("foo");
    var result = TreeUtils.retrieveFirstIdentifier(literal);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldRetrieveLastIdentifierOfSimpleMemberSelect() {
    var prefix = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var suffix = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");
    var memberSelect = TreeCreationUtils.memberSelect(prefix, suffix);
    var result = TreeUtils.retrieveLastIdentifier(memberSelect);
    assertThat(result).containsSame(suffix);
  }

  @Test
  void shouldRetrieveLastIdentifierOfComposedMemberSelect() {
    var identifier1 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var identifier2 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "bar");
    var identifier3 = TreeCreationUtils.identifier(mock(TreeMetaData.class), "baz");
    var innerMemberSelect = TreeCreationUtils.memberSelect(identifier1, identifier2);
    var memberSelect = TreeCreationUtils.memberSelect(innerMemberSelect, identifier3);
    var result = TreeUtils.retrieveLastIdentifier(memberSelect);
    assertThat(result).containsSame(identifier3);
  }

  @Test
  void shouldRetrieveIdentifierAsLastIdentifier() {
    var identifier = TreeCreationUtils.identifier(mock(TreeMetaData.class), "foo");
    var result = TreeUtils.retrieveLastIdentifier(identifier);
    assertThat(result).containsSame(identifier);
  }

  @Test
  void shouldRetrieveNothingForLastIdentifierWithOtherKindOfTree() {
    var integerLiteral = TreeCreationUtils.integerLiteral("42");
    var result = TreeUtils.retrieveLastIdentifier(integerLiteral);
    assertThat(result).isEmpty();
  }
}
