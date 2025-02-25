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
package org.sonar.go.utils;

import java.util.Optional;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;

public class SymbolHelper {

  private SymbolHelper() {
  }

  /**
   * Provide the last assigned method call name to the variable.
   * If there is none, then return null. E.g.:
   * <pre>
   * {@code
   * a = foo()      -> foo
   * a = foo.bar()  -> foo.bar
   * a = 5          -> null
   * }
   * </pre>
   */
  public static Optional<String> getLastAssignedMethodCall(Symbol symbol) {
    for (int i = symbol.getUsages().size() - 1; i >= 0; i--) {
      Usage usage = symbol.getUsages().get(i);
      if (usage.type() == Usage.UsageType.ASSIGNMENT || usage.type() == Usage.UsageType.DECLARATION) {
        var value = usage.value();
        if (value instanceof FunctionInvocationTree functionInvocation) {
          return Optional.of(TreeUtils.methodFqn(functionInvocation));
        }
      }
    }
    return Optional.empty();
  }
}
