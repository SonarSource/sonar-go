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
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.CompositeLiteralTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.KeyValueTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class CompositeLiteralTreeImpl extends BaseTreeImpl implements CompositeLiteralTree {
  @Nullable
  private final Tree type;
  private final List<Tree> elements;

  public CompositeLiteralTreeImpl(TreeMetaData metaData, @Nullable Tree type, List<Tree> elements) {
    super(metaData);
    this.type = type;
    this.elements = elements;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> elements() {
    return elements;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (type != null) {
      children.add(type);
    }
    children.addAll(elements);
    return children;
  }

  @Override
  public Stream<KeyValueTree> getKeyValuesElements() {
    return elements.stream()
      .filter(KeyValueTree.class::isInstance)
      .map(KeyValueTree.class::cast);
  }

  @Override
  public boolean hasType(String packageName, String typeName) {
    if (type instanceof MemberSelectTree memberSelectTree) {
      return typeName.equals(memberSelectTree.identifier().type())
        && memberSelectTree.expression() instanceof IdentifierTree identifierTree
        && packageName.equals(identifierTree.packageName());
    } else if (type instanceof IdentifierTree identifierTree) {
      return typeName.equals(identifierTree.type())
        && packageName.equals(identifierTree.packageName());
    }
    return false;
  }
}
