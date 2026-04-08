/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Collections;
import java.util.List;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.UnaryExpressionTree;

public class UnaryExpressionTreeImpl extends BaseTreeImpl implements UnaryExpressionTree {

  private final Operator operator;
  private final Tree operand;

  public UnaryExpressionTreeImpl(TreeMetaData metaData, Operator operator, Tree operand) {
    super(metaData);
    this.operator = operator;
    this.operand = operand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree operand() {
    return operand;
  }

  @Override
  public List<Tree> children() {
    return Collections.singletonList(operand);
  }

}
