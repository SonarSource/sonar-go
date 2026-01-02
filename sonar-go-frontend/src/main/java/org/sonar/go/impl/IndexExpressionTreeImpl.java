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

import java.util.List;
import org.sonar.plugins.go.api.IndexExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class IndexExpressionTreeImpl extends BaseTreeImpl implements IndexExpressionTree {
  private final Tree expression;
  private final Tree index;

  public IndexExpressionTreeImpl(TreeMetaData metaData, Tree expression, Tree index) {
    super(metaData);
    this.expression = expression;
    this.index = index;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public Tree index() {
    return index;
  }

  @Override
  public List<Tree> children() {
    return List.of(expression, index);
  }
}
