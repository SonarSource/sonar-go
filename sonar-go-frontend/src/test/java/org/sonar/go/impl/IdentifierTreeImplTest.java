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

import org.junit.jupiter.api.Test;
import org.sonar.go.symbols.Scope;
import org.sonar.go.symbols.Symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentifierTreeImplTest {

  @Test
  void testCreateSimpleIdentifier() {
    var identifier = new IdentifierTreeImpl(null, "name", "my_type", "my_package");
    var symbol = new Symbol("type", Scope.PACKAGE);
    identifier.setSymbol(symbol);

    assertThat(identifier.name()).isEqualTo("name");
    assertThat(identifier.type()).isEqualTo("my_type");
    assertThat(identifier.packageName()).isEqualTo("my_package");
    assertThat(identifier.children()).isEmpty();
    assertThat(identifier.symbol()).isSameAs(symbol);
  }

  @Test
  void shouldThrowExceptionWhenSettingSymbolTwice() {
    var identifier = new IdentifierTreeImpl(null, "name", "my_type", "my_package");
    var symbol = new Symbol("type", Scope.BLOCK);
    identifier.setSymbol(symbol);

    assertThatThrownBy(() -> identifier.setSymbol(symbol))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A symbol is already set");
  }

}
