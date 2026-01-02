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
import org.sonar.plugins.go.api.SliceTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class SliceTreeImpl extends BaseTreeImpl implements SliceTree {

  private final Tree expression;
  private final Tree low;
  private final Tree high;
  private final Tree max;
  private final boolean slice3;

  public SliceTreeImpl(TreeMetaData metaData, Tree expression, @Nullable Tree low, @Nullable Tree high, @Nullable Tree max, boolean slice3) {
    super(metaData);
    this.expression = expression;
    this.low = low;
    this.high = high;
    this.max = max;
    this.slice3 = slice3;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @CheckForNull
  @Override
  public Tree low() {
    return low;
  }

  @CheckForNull
  @Override
  public Tree high() {
    return high;
  }

  @CheckForNull
  @Override
  public Tree max() {
    return max;
  }

  @Override
  public boolean slice3() {
    return slice3;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(expression);
    addToListIfNotNull(children, low);
    addToListIfNotNull(children, high);
    addToListIfNotNull(children, max);
    return children;
  }

  private static void addToListIfNotNull(List<Tree> children, @Nullable Tree tree) {
    if (tree != null) {
      children.add(tree);
    }
  }
}
