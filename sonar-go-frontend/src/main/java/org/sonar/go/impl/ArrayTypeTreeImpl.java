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
import org.sonar.plugins.go.api.ArrayTypeTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class ArrayTypeTreeImpl extends BaseTreeImpl implements ArrayTypeTree {
  @Nullable
  private final Tree ellipsis;
  private final Tree element;
  private final List<Tree> children;

  public ArrayTypeTreeImpl(TreeMetaData metaData, @Nullable Tree ellipsis, Tree element) {
    super(metaData);
    this.ellipsis = ellipsis;
    this.element = element;

    children = new ArrayList<>();
    if (ellipsis != null) {
      children.add(ellipsis);
    }
    children.add(element);
  }

  @CheckForNull
  @Override
  public Tree length() {
    return ellipsis;
  }

  @Override
  public Tree element() {
    return element;
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
