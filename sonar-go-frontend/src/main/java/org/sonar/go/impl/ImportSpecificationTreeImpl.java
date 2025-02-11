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
import javax.annotation.Nullable;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ImportSpecificationTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class ImportSpecificationTreeImpl extends BaseTreeImpl implements ImportSpecificationTree {

  @Nullable
  private final IdentifierTree name;
  private final StringLiteralTree path;
  private final List<Tree> children = new ArrayList<>();

  public ImportSpecificationTreeImpl(TreeMetaData metaData, @Nullable IdentifierTree name, StringLiteralTree path) {
    super(metaData);
    this.name = name;
    this.path = path;

    if (name != null) {
      children.add(name);
    }
    children.add(path);
  }

  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public StringLiteralTree path() {
    return path;
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
