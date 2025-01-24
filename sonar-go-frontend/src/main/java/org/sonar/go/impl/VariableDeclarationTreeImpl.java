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
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.VariableDeclarationTree;

public class VariableDeclarationTreeImpl extends BaseTreeImpl implements VariableDeclarationTree {

  private final IdentifierTree identifier;
  private final Tree type;
  private final Tree initializer;
  private final boolean isVal;

  public VariableDeclarationTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type, @Nullable Tree initializer, boolean isVal) {
    super(metaData);
    this.identifier = identifier;
    this.type = type;
    this.initializer = initializer;
    this.isVal = isVal;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @CheckForNull
  @Override
  public Tree initializer() {
    return initializer;
  }

  @Override
  public boolean isVal() {
    return isVal;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(identifier);

    if (type != null) {
      children.add(type);
    }

    if (initializer != null) {
      children.add(initializer);
    }

    return children;
  }

}
