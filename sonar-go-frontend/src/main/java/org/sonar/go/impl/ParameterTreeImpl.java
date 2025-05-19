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
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.sonar.go.utils.ExpressionUtils.getTypeOfTree;

public class ParameterTreeImpl extends BaseTreeImpl implements ParameterTree {

  private final IdentifierTree identifier;
  private final Tree type;

  public ParameterTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type) {
    super(metaData);
    this.identifier = identifier;
    this.type = type;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public Type type() {
    return getTypeOfTree(type);
  }

  @Override
  public Tree typeTree() {
    return type;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(identifier);
    if (type != null) {
      children.add(type);
    }
    return children;
  }
}
