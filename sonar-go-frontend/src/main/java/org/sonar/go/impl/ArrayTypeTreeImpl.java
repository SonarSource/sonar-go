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
import org.sonar.plugins.go.api.ArrayTypeTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.sonar.go.impl.TypeImpl.UNKNOWN_TYPE;
import static org.sonar.go.utils.ExpressionUtils.getTypeOfTree;

public class ArrayTypeTreeImpl extends BaseTreeImpl implements ArrayTypeTree {
  @Nullable
  private final Tree ellipsis;
  private final Tree element;
  private final List<Tree> children;
  private final Type type;

  public ArrayTypeTreeImpl(TreeMetaData metaData, @Nullable Tree ellipsis, Tree element) {
    super(metaData);
    this.ellipsis = ellipsis;
    this.element = element;

    children = new ArrayList<>();
    if (ellipsis != null) {
      children.add(ellipsis);
    }
    children.add(element);

    type = computeArrayType();
  }

  @CheckForNull
  @Override
  public Tree length() {
    return ellipsis;
  }

  @Override
  public Tree element() {
    return element;
  }

  @Override
  public Type type() {
    return type;
  }

  private Type computeArrayType() {
    String lengthValue;
    if (ellipsis == null) {
      // Slice type: []int
      lengthValue = "";
    } else if (ellipsis instanceof IntegerLiteralTree lengthAsInt) {
      // [1]int
      lengthValue = lengthAsInt.value();
    } else if (ellipsis instanceof EllipsisTreeImpl) {
      // [...]int
      lengthValue = "...";
    } else if (ellipsis instanceof IdentifierTree lengthAsIdentifier) {
      // const size = 10
      // [size]int
      lengthValue = lengthAsIdentifier.name();
    } else {
      return UNKNOWN_TYPE;
    }
    return new TypeImpl(String.format("[%s]%s", lengthValue, getTypeOfTree(element).type()), "");
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
