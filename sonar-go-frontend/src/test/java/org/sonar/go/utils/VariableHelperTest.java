/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;

class VariableHelperTest {

  @Test
  void shouldProvideSingleVariableWithValue() {
    var identifier = TreeCreationUtils.identifier("a");
    var value = new IntegerLiteralTreeImpl(null, "1");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier), null, List.of(value), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(1);
    var variable = variables.get(0);
    assertThat(variable.identifier()).isSameAs(identifier);
    assertThat(variable.value()).isSameAs(value);
  }

  @Test
  void shouldProvideSingleVariableNoValue() {
    var identifier = TreeCreationUtils.identifier("a");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier), null, List.of(), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(1);
    var variable = variables.get(0);
    assertThat(variable.identifier()).isSameAs(identifier);
    assertThat(variable.value()).isNull();
  }

  @Test
  void shouldProvideMultipleVariablesWithValues() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var value1 = new IntegerLiteralTreeImpl(null, "1");
    var value2 = new IntegerLiteralTreeImpl(null, "2");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(value1, value2), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isSameAs(value1);
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isSameAs(value2);
  }

  @Test
  void shouldProvideMultipleVariablesNoValues() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isNull();
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isNull();
  }

  @Test
  void shouldProvideMultipleVariablesSingleValue() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var value = new IntegerLiteralTreeImpl(null, "1");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(value), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isSameAs(value);
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isSameAs(value);
  }
}
