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
package org.sonar.go.symbols;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
  public static final String UNKNOWN_TYPE = "UNKNOWN";

  private final String type;
  private final Scope scope;
  private final List<Usage> usages;

  public Symbol(String type, Scope scope) {
    this.type = type;
    this.scope = scope;
    this.usages = new ArrayList<>();
  }

  public String getType() {
    return type;
  }

  public Scope getScope() {
    return scope;
  }

  public List<Usage> getUsages() {
    return usages;
  }
}
