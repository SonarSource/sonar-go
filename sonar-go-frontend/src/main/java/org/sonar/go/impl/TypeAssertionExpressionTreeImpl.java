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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.TypeAssertionExpressionTree;

public class TypeAssertionExpressionTreeImpl extends BaseTreeImpl implements TypeAssertionExpressionTree {
  private final Tree expression;
  @Nullable
  private final Tree type;
  private final List<Tree> children = new ArrayList<>();

  public TypeAssertionExpressionTreeImpl(TreeMetaData metaData, Tree expression, @Nullable Tree type) {
    super(metaData);
    this.expression = expression;
    children.add(expression);
    this.type = type;
    if (type != null) {
      children.add(type);
    }
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> children() {
    return new ArrayList<>(children);
  }
}
