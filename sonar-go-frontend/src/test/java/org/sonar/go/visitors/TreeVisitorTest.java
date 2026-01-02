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
package org.sonar.go.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

class TreeVisitorTest {

  private static class DummyNativeKind implements NativeKind {
  }

  private final IdentifierTree var1 = TreeCreationUtils.identifier("var1");
  private final LiteralTree number1 = TreeCreationUtils.literal("1");
  private final BinaryExpressionTree binary = TreeCreationUtils.binary(BinaryExpressionTree.Operator.PLUS, var1, number1);
  private final BinaryExpressionTree binminus = TreeCreationUtils.binary(BinaryExpressionTree.Operator.MINUS, var1, var1);

  private final DummyNativeKind nkind = new DummyNativeKind();
  private final NativeTree nativeNode = TreeCreationUtils.simpleNative(nkind, List.of(binary, binminus));

  private final TreeVisitor<TreeContext> visitor = new TreeVisitor<>();

  @Test
  void visitSimpleTree() {
    List<Tree> visited = spy(new ArrayList<>());
    List<Tree> visitedAfter = spy(new ArrayList<>());
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.registerOnLeaveTree(Tree.class, (ctx, tree) -> visitedAfter.add(tree));
    visitor.scan(new TreeContext(), binary);

    InOrder inOrder = Mockito.inOrder(visited, visitedAfter);
    inOrder.verify(visited).add(binary);
    inOrder.verify(visited).add(var1);
    inOrder.verify(visitedAfter).add(var1);
    inOrder.verify(visited).add(number1);
    inOrder.verify(visitedAfter).add(number1);
    inOrder.verify(visitedAfter).add(binary);
    inOrder.verifyNoMoreInteractions();

    assertThat(visited).containsExactly(binary, var1, number1);
    assertThat(visitedAfter).containsExactly(var1, number1, binary);
  }

  @Test
  void visitNativeTree() {
    List<Tree> visited = new ArrayList<>();
    List<Tree> visitedAfter = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.registerOnLeaveTree(Tree.class, (ctx, tree) -> visitedAfter.add(tree));
    visitor.scan(new TreeContext(), nativeNode);
    assertThat(visited).containsExactly(nativeNode, binary, var1, number1, binminus, var1, var1);
    assertThat(visitedAfter).containsExactly(var1, number1, binary, var1, var1, binminus, nativeNode);
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
