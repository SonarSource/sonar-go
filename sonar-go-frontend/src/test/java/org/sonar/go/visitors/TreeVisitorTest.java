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
package org.sonar.go.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.utils.TreeCreationUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TreeVisitorTest {

  private class DummyNativeKind implements NativeKind {
  }

  private IdentifierTree var1 = TreeCreationUtils.identifier("var1");
  private LiteralTree number1 = new LiteralTreeImpl(null, "1");
  private BinaryExpressionTree binary = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, var1, number1);
  private BinaryExpressionTree binminus = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.MINUS, null, var1, var1);

  private DummyNativeKind nkind = new DummyNativeKind();
  private NativeTree nativeNode = new NativeTreeImpl(null, nkind, Arrays.asList(binary, binminus));

  private TreeVisitor<TreeContext> visitor = new TreeVisitor<>();

  @Test
  void visitSimpleTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), binary);
    assertThat(visited).containsExactly(binary, var1, number1);
  }

  @Test
  void visitNativeTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), nativeNode);
    assertThat(visited).containsExactly(nativeNode, binary, var1, number1, binminus, var1, var1);
  }

  @Test
  void ancestors() {
    Map<Tree, List<Tree>> ancestors = new HashMap<>();
    visitor.register(Tree.class, (ctx, tree) -> ancestors.put(tree, new ArrayList<Tree>(ctx.ancestors())));
    visitor.scan(new TreeContext(), binary);
    assertThat(ancestors.get(binary)).isEmpty();
    assertThat(ancestors.get(var1)).containsExactly(binary);
    assertThat(ancestors.get(number1)).containsExactly(binary);
  }
}
