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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ModifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class ParameterTreeImplTest {

  private class TypeNativeKind implements NativeKind {
  }

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree parameterType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    ParameterTreeImpl parameterTreeX = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXCopy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null);
    ParameterTreeImpl parameterTreeXTyped = new ParameterTreeImpl(meta, identifierTreeX, parameterType);
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, parameterType);

    assertThat(parameterTreeXTyped.children()).hasSize(2);
    assertThat(parameterTreeX.children()).hasSize(1);
    assertThat(parameterTreeX.type()).isNull();
    assertThat(parameterTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(parameterTreeX, parameterTreeXCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeX, parameterTreeXTyped)).isFalse();
    assertThat(areEquivalent(parameterTreeX, parameterTreeY)).isFalse();
    assertThat(areEquivalent(parameterTreeXTyped, parameterTreeY)).isFalse();
  }

  @Test
  void test_default_value() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    IdentifierTree defaultValue1 = new IdentifierTreeImpl(meta, "1");
    ParameterTreeImpl parameterTreeXDefault1 = new ParameterTreeImpl(meta, identifierTreeX, null, defaultValue1);
    ParameterTreeImpl parameterTreeXDefault1Copy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, new IdentifierTreeImpl(meta, "1"));
    ParameterTreeImpl parameterTreeXDefault2 = new ParameterTreeImpl(meta, identifierTreeX, null, new IdentifierTreeImpl(meta, "2"));
    ParameterTreeImpl parameterTreeXDefaultNative = new ParameterTreeImpl(meta, identifierTreeX, null, new NativeTreeImpl(meta, new TypeNativeKind(), null));
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, null);

    assertThat(parameterTreeXDefault1.children()).hasSize(2);
    assertThat(parameterTreeXDefault2.children()).hasSize(2);
    assertThat(parameterTreeXDefaultNative.children()).hasSize(2);
    assertThat(parameterTreeY.children()).hasSize(1);
    assertThat(parameterTreeXDefault1.defaultValue()).isEqualTo(defaultValue1);
    assertThat(parameterTreeY.defaultValue()).isNull();

    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault1Copy)).isTrue();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault2)).isFalse();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefaultNative)).isFalse();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeY)).isFalse();
  }

  @Test
  void test_modifiers() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    Tree publicModifier = new ModifierTreeImpl(meta, ModifierTree.Kind.PUBLIC);

    ParameterTreeImpl parameterTreeXPublic = new ParameterTreeImpl(meta, identifierTreeX, null, null,
      Collections.singletonList(publicModifier));
    ParameterTreeImpl parameterTreeXPublicCopy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"),
      null, null, Collections.singletonList(new ModifierTreeImpl(meta, ModifierTree.Kind.PUBLIC)));
    ParameterTreeImpl parameterTreeXPrivate = new ParameterTreeImpl(meta, identifierTreeX,
      null, null, Collections.singletonList(new ModifierTreeImpl(meta, ModifierTree.Kind.PRIVATE)));
    ParameterTreeImpl parameterTreeXNative = new ParameterTreeImpl(meta, identifierTreeX, null, null,
      Collections.singletonList(new NativeTreeImpl(meta, new TypeNativeKind(), null)));
    ParameterTreeImpl parameterTreeNoMod = new ParameterTreeImpl(meta, identifierTreeY, null);

    assertThat(parameterTreeXPublic.children()).hasSize(2);
    assertThat(parameterTreeXPrivate.children()).hasSize(2);
    assertThat(parameterTreeXNative.children()).hasSize(2);
    assertThat(parameterTreeNoMod.children()).hasSize(1);
    assertThat(parameterTreeXPublic.modifiers()).hasSize(1);
    assertThat(parameterTreeXPublic.modifiers().get(0)).isEqualTo(publicModifier);
    assertThat(parameterTreeNoMod.modifiers()).isEmpty();

    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPublicCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPrivate)).isFalse();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXNative)).isFalse();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeNoMod)).isFalse();
  }

  @Test
  void test_null_identifier() {
    TreeMetaData meta = null;
    Tree publicModifier = new ModifierTreeImpl(meta, ModifierTree.Kind.PUBLIC);
    ParameterTreeImpl parameterTreePublic = new ParameterTreeImpl(meta, null, null, null,
      Collections.singletonList(publicModifier));

    assertThat(parameterTreePublic.children()).hasSize(1);
    assertThat(parameterTreePublic.identifier()).isNull();
  }

}
