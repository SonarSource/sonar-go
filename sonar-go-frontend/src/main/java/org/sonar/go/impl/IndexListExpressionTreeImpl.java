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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.plugins.go.api.IndexListExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class IndexListExpressionTreeImpl extends BaseTreeImpl implements IndexListExpressionTree {
  private final List<Tree> children;

  private final Tree expression;
  private final List<Tree> indices;

  public IndexListExpressionTreeImpl(TreeMetaData metaData, Tree expression, List<Tree> indices) {
    super(metaData);

    List<Tree> currentChildren = new ArrayList<>();

    this.expression = expression;
    currentChildren.add(expression);

    this.indices = List.copyOf(indices);
    currentChildren.addAll(indices);
    this.children = Collections.unmodifiableList(currentChildren);
  }

  @Override
  public List<Tree> children() {
    return children;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public List<Tree> indices() {
    return indices;
  }
}
