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
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.GoStatementTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class GoStatementTreeImpl extends BaseTreeImpl implements GoStatementTree {
  private final Token goToken;
  private final FunctionInvocationTree functionInvocationTree;

  public GoStatementTreeImpl(TreeMetaData metaData, Token goToken, FunctionInvocationTree functionInvocationTree) {
    super(metaData);
    this.goToken = goToken;
    this.functionInvocationTree = functionInvocationTree;
  }

  @Override
  public Token goToken() {
    return goToken;
  }

  @Override
  public FunctionInvocationTree functionInvocation() {
    return functionInvocationTree;
  }

  @Override
  public List<Tree> children() {
    return List.of(functionInvocationTree);
  }
}
