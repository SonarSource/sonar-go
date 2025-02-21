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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SymbolTest {

  @Test
  void testCreateSimpleSymbol() {
    var symbol = new Symbol("my_type", Scope.PACKAGE);
    assertThat(symbol.getType()).isEqualTo("my_type");
    assertThat(symbol.getScope()).isSameAs(Scope.PACKAGE);
    assertThat(symbol.getUsages()).isEmpty();
  }

  @Test
  void testCreateSymbolNullType() {
    var symbol = new Symbol("UNKNOWN", Scope.FUNCTION);
    assertThat(symbol.getType()).isEqualTo("UNKNOWN");
    assertThat(symbol.getScope()).isSameAs(Scope.FUNCTION);
    assertThat(symbol.getUsages()).isEmpty();
  }

  @Test
  void testCreateSymbolWithUsages() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var declaration = new Usage(mock(), null, Usage.UsageType.DECLARATION);
    var assignment = new Usage(mock(), null, Usage.UsageType.ASSIGNMENT);
    var reference = new Usage(mock(), null, Usage.UsageType.REFERENCE);
    symbol.getUsages().addAll(List.of(declaration, assignment, reference));

    assertThat(symbol.getUsages()).hasSize(3);
    assertThat(symbol.getUsages().get(0)).isSameAs(declaration);
    assertThat(symbol.getUsages().get(1)).isSameAs(assignment);
    assertThat(symbol.getUsages().get(2)).isSameAs(reference);
  }
}
