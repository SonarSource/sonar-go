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
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.Tree;

public class TreeVisitor<C extends TreeContext> {

  private List<ConsumerFilter<C, ?>> consumersOnEnterTree;
  private List<ConsumerFilter<C, ?>> consumersOnLeaveTree;

  public TreeVisitor() {
    consumersOnEnterTree = new ArrayList<>();
    consumersOnLeaveTree = new ArrayList<>();
  }

  public void scan(C ctx, @Nullable Tree root) {
    if (root != null) {
      ctx.before(root);
      before(ctx, root);
      visit(ctx, root);
      after(ctx, root);
    }
  }

  private void visit(C ctx, @Nullable Tree node) {
    if (node != null) {
      ctx.enter(node);
      callConsumers(ctx, node, consumersOnEnterTree);
      node.children().forEach(child -> visit(ctx, child));
      callConsumers(ctx, node, consumersOnLeaveTree);
      ctx.leave(node);
    }
  }

  private void callConsumers(C ctx, Tree node, List<ConsumerFilter<C, ?>> consumerList) {
    for (ConsumerFilter<C, ?> consumer : consumerList) {
      consumer.accept(ctx, node);
    }
  }

  protected void before(C ctx, Tree root) {
    // default behaviour is to do nothing
  }

  protected void after(C ctx, Tree root) {
    // default behaviour is to do nothing
  }

  public <T extends Tree> TreeVisitor<C> register(Class<T> cls, BiConsumer<C, T> visitor) {
    consumersOnEnterTree.add(new ConsumerFilter<>(cls, visitor));
    return this;
  }

  public <T extends Tree> TreeVisitor<C> registerOnLeaveTree(Class<T> cls, BiConsumer<C, T> visitor) {
    consumersOnLeaveTree.add(new ConsumerFilter<>(cls, visitor));
    return this;
  }

  private static class ConsumerFilter<C extends TreeContext, T extends Tree> {

    private final Class<T> cls;

    private final BiConsumer<C, T> delegate;

    private ConsumerFilter(Class<T> cls, BiConsumer<C, T> delegate) {
      this.cls = cls;
      this.delegate = delegate;
    }

    private void accept(C ctx, Tree node) {
      if (cls.isAssignableFrom(node.getClass())) {
        delegate.accept(ctx, cls.cast(node));
      }
    }
  }
}
