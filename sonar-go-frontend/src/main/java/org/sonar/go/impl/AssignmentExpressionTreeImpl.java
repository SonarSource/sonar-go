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

import java.util.Arrays;
import java.util.List;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class AssignmentExpressionTreeImpl extends BaseTreeImpl implements AssignmentExpressionTree {

  private final Operator operator;
  private final Tree leftHandSide;
  private final Tree statementOrExpression;

  public AssignmentExpressionTreeImpl(TreeMetaData metaData, Operator operator, Tree leftHandSide, Tree statementOrExpression) {
    super(metaData);
    this.operator = operator;
    this.leftHandSide = leftHandSide;
    this.statementOrExpression = statementOrExpression;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree leftHandSide() {
    return leftHandSide;
  }

  @Override
  public Tree statementOrExpression() {
    return statementOrExpression;
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(leftHandSide, statementOrExpression);
  }
}
