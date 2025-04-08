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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class LoopTreeImpl extends BaseTreeImpl implements LoopTree {

  private final Tree condition;
  private final Tree body;
  private final LoopKind kind;
  private final Token keyword;

  public LoopTreeImpl(TreeMetaData metaData, @Nullable Tree condition, Tree body, LoopKind kind, Token keyword) {
    super(metaData);
    this.condition = condition;
    this.body = body;
    this.kind = kind;
    this.keyword = keyword;
  }

  @CheckForNull
  @Override
  public Tree condition() {
    return condition;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public LoopKind kind() {
    return kind;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (condition != null) {
      children.add(condition);
    }
    children.add(body);
    return children;
  }
}
