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

import org.junit.jupiter.api.Test;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.ParseUtils.parse;

class AddressOfTest {

  @Test
  void shouldParseSimpleAddressOf() {
    var addressOf = AddressOf.of((NativeTree) parse("&sonar"));
    assertThat(addressOf).isPresent();
    AddressOf address = addressOf.get();
    assertThat(address.operand()).isInstanceOfSatisfying(IdentifierTree.class, identifier -> assertThat(identifier.name()).isEqualTo("sonar"));
  }

  @Test
  void shouldNotParseOtherNativeTree() {
    var addressOf = AddressOf.of((NativeTree) parse("1 | 2"));
    assertThat(addressOf).isEmpty();
  }

  @Test
  void erroneouslyConsiderOtherUnaryOperator() {
    var addressOf = AddressOf.of((NativeTree) parse("^sonar"));
    // AddressOf is mapped to UnaryExpr, but it should not be considered as AddressOf in the future.
    assertThat(addressOf).isPresent();
  }
}
