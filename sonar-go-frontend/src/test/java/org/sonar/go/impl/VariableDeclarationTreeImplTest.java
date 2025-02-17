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
import java.util.List;
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
    VariableDeclarationTreeImpl variableTreeX = new VariableDeclarationTreeImpl(meta, List.of(identifierTreeX), null, Collections.emptyList(), false);
    VariableDeclarationTreeImpl variableTreeXCopy = new VariableDeclarationTreeImpl(meta, List.of(new IdentifierTreeImpl(meta, "x")), null, Collections.emptyList(), false);
    VariableDeclarationTreeImpl valueTreeX = new VariableDeclarationTreeImpl(meta, List.of(new IdentifierTreeImpl(meta, "x")), null, Collections.emptyList(), true);
    VariableDeclarationTreeImpl variableTreeXTyped = new VariableDeclarationTreeImpl(meta, List.of(identifierTreeX), variableType, Collections.emptyList(), false);
    VariableDeclarationTreeImpl variableTreeY = new VariableDeclarationTreeImpl(meta, List.of(identifierTreeY), variableType, Collections.emptyList(), false);

    assertThat(variableTreeXTyped.children()).hasSize(2);
    assertThat(variableTreeX.children()).hasSize(1);
    assertThat(variableTreeX.type()).isNull();
    assertThat(variableTreeX.identifiers()).containsExactly(identifierTreeX);
    assertThat(areEquivalent(variableTreeX, variableTreeXCopy)).isTrue();
    assertThat(areEquivalent(variableTreeX, valueTreeX)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeXTyped)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeY)).isFalse();
    assertThat(areEquivalent(variableTreeXTyped, variableTreeY)).isFalse();
  }
}
