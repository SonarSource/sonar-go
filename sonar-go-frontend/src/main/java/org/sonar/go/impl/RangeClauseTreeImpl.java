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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.utils.TreeUtils;
import org.sonar.plugins.go.api.RangeClauseTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class RangeClauseTreeImpl extends BaseTreeImpl implements RangeClauseTree {

  @Nullable
  private final Tree key;
  @Nullable
  private final Tree value;
  private final Tree rangedExpression;
  private final boolean isDeclaration;

  public RangeClauseTreeImpl(TreeMetaData metaData, @Nullable Tree key, @Nullable Tree value, Tree rangedExpression, boolean isDeclaration) {
    super(metaData);
    this.key = key;
    this.value = value;
    this.rangedExpression = rangedExpression;
    this.isDeclaration = isDeclaration;
  }

  @CheckForNull
  @Override
  public Tree key() {
    return key;
  }

  @CheckForNull
  @Override
  public Tree value() {
    return value;
  }

  @Override
  public Tree rangedExpression() {
    return rangedExpression;
  }

  @Override
  public boolean isDeclaration() {
    return isDeclaration;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    TreeUtils.addToListIfNotNull(children, key);
    TreeUtils.addToListIfNotNull(children, value);
    children.add(rangedExpression);
    return children;
  }
}
