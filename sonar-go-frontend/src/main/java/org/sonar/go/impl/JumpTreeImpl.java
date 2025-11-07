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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class JumpTreeImpl extends BaseTreeImpl implements JumpTree {
  private final IdentifierTree label;
  private final Token keyword;
  private final JumpKind kind;

  public JumpTreeImpl(TreeMetaData metaData, Token keyword, JumpKind kind, @Nullable IdentifierTree label) {
    super(metaData);
    this.label = label;
    this.keyword = keyword;
    this.kind = kind;
  }

  @CheckForNull
  @Override
  public IdentifierTree label() {
    return label;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public JumpKind kind() {
    return kind;
  }

  @Override
  public List<Tree> children() {
    return label == null ? Collections.emptyList() : Collections.singletonList(label);
  }
}
