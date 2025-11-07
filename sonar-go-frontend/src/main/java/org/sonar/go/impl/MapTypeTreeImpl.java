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

import java.util.List;
import org.sonar.plugins.go.api.MapTypeTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.sonar.go.utils.ExpressionUtils.getTypeOfTree;

public class MapTypeTreeImpl extends BaseTreeImpl implements MapTypeTree {
  private final Tree key;
  private final Tree value;
  private final Type type;

  public MapTypeTreeImpl(TreeMetaData metaData, Tree key, Tree value) {
    super(metaData);
    this.key = key;
    this.value = value;
    this.type = computeMapType();
  }

  @Override
  public List<Tree> children() {
    return List.of(key, value);
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
  public Type type() {
    return type;
  }

  private Type computeMapType() {
    var keyType = getTypeOfTree(key);
    var valueType = getTypeOfTree(value);
    return new TypeImpl(String.format("map[%s]%s", keyType.type(), valueType.type()), "");
  }
}
