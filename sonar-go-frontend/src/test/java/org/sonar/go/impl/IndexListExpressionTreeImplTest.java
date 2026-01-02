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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexListExpressionTreeImplTest {
  @Test
  void testIndexListExpression() {
    var a = TreeCreationUtils.identifier("a");
    var index1 = TreeCreationUtils.literal("1");
    var index2 = TreeCreationUtils.literal("2");
    var indexExpressionTree = new IndexListExpressionTreeImpl(null, a, List.of(index1, index2));
    assertThat(indexExpressionTree.expression()).isEqualTo(a);
    assertThat(indexExpressionTree.indices()).containsExactly(index1, index2);
    assertThat(indexExpressionTree.children()).containsExactly(a, index1, index2);
  }

  @Test
  void testIndexListExpressionIndicesAreCopied() {
    var a = TreeCreationUtils.identifier("a");
    var index1 = TreeCreationUtils.literal("1");
    var index2 = TreeCreationUtils.literal("2");
    List<Tree> indices = new ArrayList<>();
    indices.add(index1);
    indices.add(index2);
    var indexExpressionTree = new IndexListExpressionTreeImpl(null, a, indices);
    assertThat(indexExpressionTree.indices()).containsExactly(index1, index2);
    indices.remove(0);
    assertThat(indexExpressionTree.indices()).containsExactly(index1, index2);
  }

  @Test
  void testIndexListExpressionChildrenAreImmutable() {
    var a = TreeCreationUtils.identifier("a");
    var index1 = TreeCreationUtils.literal("1");
    var index2 = TreeCreationUtils.literal("2");
    var indexExpressionTree = new IndexListExpressionTreeImpl(null, a, List.of(index1));
    List<Tree> children = indexExpressionTree.children();
    assertThatThrownBy(() -> children.add(index2))
      .isInstanceOf(UnsupportedOperationException.class);
  }
}
