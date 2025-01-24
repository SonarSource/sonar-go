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
import org.sonar.go.api.CatchTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class CatchTreeImpl extends BaseTreeImpl implements CatchTree {

  private final Tree catchParameter;
  private final Tree catchBlock;
  private final Token keyword;

  public CatchTreeImpl(TreeMetaData metaData, @Nullable Tree catchParameter, Tree catchBlock, Token keyword) {
    super(metaData);
    this.catchParameter = catchParameter;
    this.catchBlock = catchBlock;
    this.keyword = keyword;
  }

  @CheckForNull
  @Override
  public Tree catchParameter() {
    return catchParameter;
  }

  @Override
  public Tree catchBlock() {
    return catchBlock;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();

    if (catchParameter != null) {
      children.add(catchParameter);
    }
    children.add(catchBlock);

    return children;
  }
}
