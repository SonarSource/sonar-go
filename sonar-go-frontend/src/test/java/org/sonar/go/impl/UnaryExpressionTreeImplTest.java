/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.UnaryExpressionTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class UnaryExpressionTreeImplTest {

  private class TypeNativeKind implements NativeKind {
  }

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree condition = TreeCreationUtils.identifier("x");
    Tree negCondition = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree negConditionCopy = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree nativeTree = new NativeTreeImpl(meta, new TypeNativeKind(), Arrays.asList(condition));
    Tree negNative = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, nativeTree);

    assertThat(negCondition.children()).containsExactly(condition);
    assertThat(areEquivalent(negCondition, negConditionCopy)).isTrue();
    assertThat(areEquivalent(negNative, negCondition)).isFalse();
  }

}
