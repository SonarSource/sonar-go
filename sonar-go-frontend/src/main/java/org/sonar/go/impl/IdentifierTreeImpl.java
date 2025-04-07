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
import org.sonar.go.symbols.Symbol;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class IdentifierTreeImpl extends BaseTreeImpl implements IdentifierTree {

  public static final String UNKNOWN_TYPE = "UNKNOWN";
  public static final String UNKNOWN_PACKAGE = "UNKNOWN";

  private final String name;
  private final String type;
  private final String packageName;
  private final int id;
  private Symbol symbol;

  public IdentifierTreeImpl(TreeMetaData metaData, String name, String type, String packageName, int id) {
    super(metaData);
    this.name = name;
    this.type = type;
    this.packageName = packageName;
    this.id = id;
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
  public String packageName() {
    return packageName;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }

  @CheckForNull
  @Override
  public Symbol symbol() {
    return symbol;
  }

  public void setSymbol(Symbol symbol) {
    if (this.symbol != null) {
      throw new IllegalArgumentException("A symbol is already set");
    }
    this.symbol = symbol;
  }
}
