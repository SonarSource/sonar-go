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
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class MemberSelectTreeImpl extends BaseTreeImpl implements MemberSelectTree {

  private final Tree expression;
  private final IdentifierTree identifier;

  public MemberSelectTreeImpl(TreeMetaData metaData, Tree expression, IdentifierTree identifier) {
    super(metaData);
    this.expression = expression;
    this.identifier = identifier;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(expression);
    children.add(identifier);
    return children;
  }
}
