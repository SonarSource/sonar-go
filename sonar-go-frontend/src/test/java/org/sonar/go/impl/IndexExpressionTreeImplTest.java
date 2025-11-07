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
package org.sonar.go.impl;

import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.LiteralTree;

import static org.assertj.core.api.Assertions.assertThat;

class IndexExpressionTreeImplTest {
  @Test
  void testIndexExpression() {
    IdentifierTree a = TreeCreationUtils.identifier("a");
    LiteralTree index = TreeCreationUtils.literal("1");
    IndexExpressionTreeImpl indexExpressionTree = new IndexExpressionTreeImpl(null, a, index);
    assertThat(indexExpressionTree.expression()).isEqualTo(a);
    assertThat(indexExpressionTree.index()).isEqualTo(index);
    assertThat(indexExpressionTree.children()).containsExactly(a, index);
  }
}
