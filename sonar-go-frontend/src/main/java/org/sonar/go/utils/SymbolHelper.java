/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonar.go.utils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.plugins.go.api.HasSymbol;
import org.sonar.plugins.go.api.Tree;

public class SymbolHelper {

  private SymbolHelper() {
  }

  public static Usage getDeclaration(Symbol symbol) {
    return symbol.getUsages().stream()
      .filter(usage -> usage.type() == Usage.UsageType.DECLARATION)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Symbol has no declaration: " + symbol));
  }

  /**
  * If the argument is an identifier with a symbol, then return the safe value of the symbol if it exists.
  * Otherwise, just return the argument itself.
  */
  @CheckForNull
  public static Tree unpackToSafeSymbolValueIfExisting(@Nullable Tree argument) {
    if (argument instanceof HasSymbol hasSymbol && hasSymbol.symbol() != null) {
      var safeValue = hasSymbol.symbol().getSafeValue();
      if (safeValue != null) {
        argument = safeValue;
      }
    }
    return argument;
  }
}
