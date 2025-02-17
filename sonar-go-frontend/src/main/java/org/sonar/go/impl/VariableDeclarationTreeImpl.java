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

  private final List<IdentifierTree> identifiers;
  private final Tree type;
  private final List<Tree> initializers;
  private final boolean isVal;

  public VariableDeclarationTreeImpl(TreeMetaData metaData, List<IdentifierTree> identifiers, @Nullable Tree type, List<Tree> initializers, boolean isVal) {
    super(metaData);
    this.identifiers = identifiers;
    this.type = type;
    this.initializers = initializers;
    this.isVal = isVal;
  }

  @Override
  public List<IdentifierTree> identifiers() {
    return identifiers;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> initializers() {
    return initializers;
  }

  @Override
  public boolean isVal() {
    return isVal;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>(identifiers);
    if (type != null) {
      children.add(type);
    }
    children.addAll(initializers);
    return children;
  }
}
