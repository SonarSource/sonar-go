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
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.ExceptionHandlingTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class ExceptionHandlingTreeImpl extends BaseTreeImpl implements ExceptionHandlingTree {

  private final Tree tryBlock;
  private final List<CatchTree> catchBlocks;
  private final Tree finallyBlock;
  private final Token tryKeyword;

  public ExceptionHandlingTreeImpl(TreeMetaData metaData, Tree tryBlock, Token tryKeyword, List<CatchTree> catchBlocks, @Nullable Tree finallyBlock) {
    super(metaData);
    this.tryBlock = tryBlock;
    this.catchBlocks = catchBlocks;
    this.finallyBlock = finallyBlock;
    this.tryKeyword = tryKeyword;
  }

  @Override
  public Tree tryBlock() {
    return tryBlock;
  }

  @Override
  public List<CatchTree> catchBlocks() {
    return catchBlocks;
  }

  @CheckForNull
  @Override
  public Tree finallyBlock() {
    return finallyBlock;
  }

  @Override
  public Token tryKeyword() {
    return tryKeyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();

    children.add(tryBlock);
    children.addAll(catchBlocks);

    if (finallyBlock != null) {
      children.add(finallyBlock);
    }

    return children;
  }
}
