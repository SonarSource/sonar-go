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
import org.sonar.go.api.StarExpressionTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class StarExpressionTreeImplTest {

  @Test
  void simple_star_expression() {
    TreeMetaData meta = null;
    Tree identifierTree = new IdentifierTreeImpl(meta, "x");

    StarExpressionTree tree = new StarExpressionTreeImpl(meta, identifierTree);
    assertThat(tree.children()).containsExactly(identifierTree);
    assertThat(tree.operand()).isEqualTo(identifierTree);
  }
}
