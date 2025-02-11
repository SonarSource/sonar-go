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
import org.sonar.go.api.KeyValueTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class KeyValueTreeImpl extends BaseTreeImpl implements KeyValueTree {
  private final Tree key;
  private final Tree value;

  public KeyValueTreeImpl(TreeMetaData metaData, Tree key, Tree value) {
    super(metaData);
    this.key = key;
    this.value = value;
  }

  @Override
  public Tree key() {
    return key;
  }

  @Override
  public Tree value() {
    return value;
  }

  @Override
  public List<Tree> children() {
    return List.of(key, value);
  }
}
