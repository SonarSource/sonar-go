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
package org.sonar.go.symbols;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.TextRanges;
import org.sonar.go.utils.TreeCreationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SymbolTest {

  @Test
  void testCreateSimpleSymbol() {
    var symbol = new Symbol("my_type");
    assertThat(symbol.getType()).isEqualTo("my_type");
    assertThat(symbol.getUsages()).isEmpty();
  }

  @Test
  void testCreateSymbolNullType() {
    var symbol = new Symbol("UNKNOWN");
    assertThat(symbol.getType()).isEqualTo("UNKNOWN");
    assertThat(symbol.getUsages()).isEmpty();
  }

  @Test
  void testCreateSymbolWithUsages() {
    var symbol = new Symbol("my_type");
    var declaration = new Usage(mock(), null, Usage.UsageType.DECLARATION);
    var assignment = new Usage(mock(), null, Usage.UsageType.ASSIGNMENT);
    var reference = new Usage(mock(), null, Usage.UsageType.REFERENCE);
    symbol.getUsages().addAll(List.of(declaration, assignment, reference));

    assertThat(symbol.getUsages()).hasSize(3);
    assertThat(symbol.getUsages().get(0)).isSameAs(declaration);
    assertThat(symbol.getUsages().get(1)).isSameAs(assignment);
    assertThat(symbol.getUsages().get(2)).isSameAs(reference);
  }

  @Test
  void getSafeValueShouldReturnValueForSingleDeclaration() {
    var symbol = new Symbol("my_type");
    var value = new IntegerLiteralTreeImpl(mock(), "42");
    var declaration = new Usage(mock(), value, Usage.UsageType.DECLARATION);
    symbol.getUsages().add(declaration);
    assertThat(symbol.getSafeValue()).isEqualTo(value);
  }

  @Test
  void getSafeValueShouldReturnValueWhenThereIsNoAssignment() {
    var symbol = new Symbol("my_type");
    var value = new IntegerLiteralTreeImpl(mock(), "42");
    var declaration = new Usage(mock(), value, Usage.UsageType.DECLARATION);
    var reference = new Usage(mock(), null, Usage.UsageType.REFERENCE);
    symbol.getUsages().addAll(List.of(declaration, reference));
    assertThat(symbol.getSafeValue()).isEqualTo(value);
  }

  @Test
  void getSafeValueShouldReturnNullWhenThereIsAnAssignment() {
    var symbol = new Symbol("my_type");
    var value = new IntegerLiteralTreeImpl(mock(), "42");
    var declaration = new Usage(mock(), value, Usage.UsageType.DECLARATION);
    var assignment = new Usage(mock(), null, Usage.UsageType.ASSIGNMENT);
    symbol.getUsages().addAll(List.of(declaration, assignment));
    assertThat(symbol.getSafeValue()).isNull();
  }

  @Test
  void testGetUsagesBefore() {
    var symbol = new Symbol("my_type");
    var declaration = new Usage(TreeCreationUtils.identifier("my_id", TextRanges.range(1, 1, 1, 1)), null, Usage.UsageType.DECLARATION);
    var assignment = new Usage(TreeCreationUtils.identifier("my_id", TextRanges.range(2, 1, 2, 1)), null, Usage.UsageType.ASSIGNMENT);
    var reference = new Usage(TreeCreationUtils.identifier("my_id", TextRanges.range(3, 1, 3, 1)), null, Usage.UsageType.REFERENCE);
    symbol.getUsages().addAll(List.of(declaration, assignment, reference));

    assertThat(symbol.getUsages()).hasSize(3);
    assertThat(symbol.getUsagesBeforeLine(3)).hasSize(2);
    assertThat(symbol.getUsagesBefore(TreeCreationUtils.identifier("other_id", TextRanges.range(3, 1, 3, 1)))).hasSize(2);
  }
}
