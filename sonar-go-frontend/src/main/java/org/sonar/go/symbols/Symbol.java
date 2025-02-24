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
import javax.annotation.CheckForNull;
import org.sonar.go.api.Tree;

public class Symbol {
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

  /**
   * Returns the value of the symbol if it is safe to do so.
   * A value is considered safe if it is assigned in the declaration and not other value is assigned afterward.
   */
  @CheckForNull
  public Tree getSafeValue() {
    var declarationAndAssignments = usages.stream()
      .filter(usage -> usage.type() == Usage.UsageType.DECLARATION || usage.type() == Usage.UsageType.ASSIGNMENT)
      .toList();
    if (declarationAndAssignments.size() != 1) {
      return null;
    }
    return declarationAndAssignments.get(0).value();
  }
}
