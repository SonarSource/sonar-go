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

import java.util.List;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.LeftRightHandSide;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.sonar.go.utils.TreeUtils.IS_NOT_EMPTY_NATIVE_TREE;

public class LeftRightHandSideImpl extends BaseTreeImpl implements LeftRightHandSide {
  private final List<Tree> children;

  public LeftRightHandSideImpl(TreeMetaData metaData, List<Tree> children) {
    super(metaData);
    this.children = children;
  }

  @Override
  public List<Tree> children() {
    return children;
  }

  @Override
  public List<IdentifierTree> extractIdentifiers() {
    return children.stream()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .toList();
  }

  @Override
  public List<Tree> getChildrenSkipEmptyNativeTrees() {
    return children.stream()
      .filter(IS_NOT_EMPTY_NATIVE_TREE)
      .toList();
  }
}
