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
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class VariableDeclarationTreeImplTest {

  private class TypeNativeKind implements NativeKind {
  }

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree variableType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    VariableDeclarationTreeImpl variableTreeX = new VariableDeclarationTreeImpl(meta, identifierTreeX, null, null, false);
    VariableDeclarationTreeImpl variableTreeXCopy = new VariableDeclarationTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, null, false);
    VariableDeclarationTreeImpl valueTreeX = new VariableDeclarationTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, null, true);
    VariableDeclarationTreeImpl variableTreeXTyped = new VariableDeclarationTreeImpl(meta, identifierTreeX, variableType, null, false);
    VariableDeclarationTreeImpl variableTreeY = new VariableDeclarationTreeImpl(meta, identifierTreeY, variableType, null, false);

    assertThat(variableTreeXTyped.children()).hasSize(2);
    assertThat(variableTreeX.children()).hasSize(1);
    assertThat(variableTreeX.type()).isNull();
    assertThat(variableTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(variableTreeX, variableTreeXCopy)).isTrue();
    assertThat(areEquivalent(variableTreeX, valueTreeX)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeXTyped)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeY)).isFalse();
    assertThat(areEquivalent(variableTreeXTyped, variableTreeY)).isFalse();
  }
}
