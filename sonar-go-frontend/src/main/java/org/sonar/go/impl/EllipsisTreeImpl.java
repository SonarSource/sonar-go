/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.EllipsisTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class EllipsisTreeImpl extends BaseTreeImpl implements EllipsisTree {

  private final Token ellipsis;
  @Nullable
  private final Tree element;

  public EllipsisTreeImpl(TreeMetaData metaData, Token ellipsis, @Nullable Tree element) {
    super(metaData);
    this.ellipsis = ellipsis;
    this.element = element;
  }

  @CheckForNull
  @Override
  public Tree element() {
    return element;
  }

  @Override
  public Token ellipsis() {
    return ellipsis;
  }

  @Override
  public List<Tree> children() {
    if (element == null) {
      return List.of();
    }
    return List.of(element);
  }
}
