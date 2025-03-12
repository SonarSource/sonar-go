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

import java.util.Collections;
import java.util.List;
import org.sonar.go.api.PlaceHolderTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class PlaceHolderTreeImpl extends IdentifierTreeImpl implements PlaceHolderTree {
  private final Token placeHolderToken;

  public PlaceHolderTreeImpl(TreeMetaData metaData, Token placeHolderToken) {
    super(metaData, "_", UNKNOWN_TYPE, UNKNOWN_PACKAGE, 0);
    this.placeHolderToken = placeHolderToken;
  }

  @Override
  public Token placeHolderToken() {
    return placeHolderToken;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }
}
