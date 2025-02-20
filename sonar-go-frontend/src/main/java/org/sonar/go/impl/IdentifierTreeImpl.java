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
import javax.annotation.CheckForNull;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class IdentifierTreeImpl extends BaseTreeImpl implements IdentifierTree {

  public static final String UNKNOWN_TYPE = "UNKNOWN";

  private final String name;

  private final String type;

  public IdentifierTreeImpl(TreeMetaData metaData, String name, String type) {
    super(metaData);
    this.name = name;
    this.type = type;
  }

  public String name() {
    return name;
  }

  @CheckForNull
  @Override
  public String type() {
    return type;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }
}
