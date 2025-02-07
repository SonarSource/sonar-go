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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.CompositeLiteralTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeLiteralTreeImplTest {

  @Test
  void simple_composite_literal() {
    TreeMetaData meta = null;
    Tree identifierTree = new IdentifierTreeImpl(meta, "x");
    List<Tree> elements = new ArrayList<>();

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, identifierTree, elements);
    assertThat(tree.children()).containsExactly(identifierTree);
    assertThat(tree.type()).isEqualTo(identifierTree);
    assertThat(tree.elements()).isNotNull();
    assertThat(tree.elements()).isEmpty();
  }

  @Test
  void composite_literal_with_elements() {
    TreeMetaData meta = null;
    Tree identifierTree = new IdentifierTreeImpl(meta, "x");
    Tree el1 = new IdentifierTreeImpl(meta, "x");
    Tree el2 = new LiteralTreeImpl(meta, "x");
    List<Tree> elements = new ArrayList<>();
    elements.add(el1);
    elements.add(el2);

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, identifierTree, elements);
    assertThat(tree.children()).containsExactly(identifierTree, el1, el2);
    assertThat(tree.type()).isEqualTo(identifierTree);
    assertThat(tree.elements()).isNotNull();
    assertThat(tree.elements()).hasSize(2);
    assertThat(tree.elements().get(0)).isEqualTo(el1);
    assertThat(tree.elements().get(1)).isEqualTo(el2);
  }

  @Test
  void composite_literal_with_null_type() {
    TreeMetaData meta = null;
    List<Tree> elements = new ArrayList<>();

    CompositeLiteralTree tree = new CompositeLiteralTreeImpl(meta, null, elements);
    assertThat(tree.children()).isEmpty();
    assertThat(tree.type()).isNull();
  }
}
