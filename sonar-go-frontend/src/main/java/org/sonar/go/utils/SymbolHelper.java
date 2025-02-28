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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.StringLiteralTree;
import org.sonar.go.api.Tree;
import org.sonar.go.symbols.GoNativeType;
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
      var usage = symbol.getUsages().get(i);
      if (usage.type() == Usage.UsageType.ASSIGNMENT || usage.type() == Usage.UsageType.DECLARATION) {
        var value = usage.value();
        if (value instanceof FunctionInvocationTree functionInvocation) {
          return Optional.of(TreeUtils.methodFqn(functionInvocation));
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<Tree> getLastAssignedValue(Symbol symbol) {
    for (int i = symbol.getUsages().size() - 1; i >= 0; i--) {
      var usage = symbol.getUsages().get(i);
      if (usage.type() == Usage.UsageType.ASSIGNMENT || usage.type() == Usage.UsageType.DECLARATION) {
        return Optional.ofNullable(usage.value());
      }
    }
    return Optional.empty();
  }

  @CheckForNull
  public static String resolveStringValue(@Nullable Tree tree) {
    if (tree instanceof StringLiteralTree stringLiteralTree) {
      return stringLiteralTree.content();
    } else if (tree instanceof IdentifierTree identifierTree) {
      var symbol = identifierTree.symbol();
      if (symbol != null && symbol.getType().equals(GoNativeType.STRING)) {
        return resolveStringValue(symbol.getSafeValue());
      }
    }
    return null;
  }

  /**
   * If the provided value is an identifier, try to get the symbol and call {@link SymbolHelper#getLastAssignedValue(Symbol)} on it. If it gets a value return it.
   * Otherwise, just return the provided tree.
   */
  @CheckForNull
  public static Tree resolveValue(@Nullable Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      var symbol = identifierTree.symbol();
      if (symbol != null) {
        Optional<Tree> value = SymbolHelper.getLastAssignedValue(symbol);
        if (value.isPresent()) {
          return value.get();
        }
      }
    }
    return tree;
  }
}
