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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.symbols.GoNativeType;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolHelperTest {

  @Test
  void shouldReturnCurrentTreeIfNotAnIdentifier() {
    var stringLiteral = TreeCreationUtils.stringLiteral("\"my_string\"");
    var resolvedValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(stringLiteral);
    assertThat(resolvedValue).isSameAs(stringLiteral);
  }

  @Test
  void shouldReturnCurrentIdentifierIfNoSymbolIsPresent() {
    var identifier = TreeCreationUtils.identifier("a");
    var resolvedValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(identifier);
    assertThat(resolvedValue).isSameAs(identifier);
  }

  @Test
  void shouldReturnCurrentIdentifierIfNoVlueIsAssigned() {
    var symbol = new Symbol("int");
    var identifier = TreeCreationUtils.identifier("a");
    symbol.getUsages().add(new Usage(identifier, null, Usage.UsageType.DECLARATION));
    identifier.setSymbol(symbol);
    var resolvedValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(identifier);
    assertThat(resolvedValue).isSameAs(identifier);
  }

  @Test
  void shouldReturnAssignedValue() {
    var symbol = new Symbol("int");
    var value = TreeCreationUtils.integerLiteral("5");
    var identifier = TreeCreationUtils.identifier("a");
    symbol.getUsages().add(new Usage(identifier, value, Usage.UsageType.DECLARATION));
    identifier.setSymbol(symbol);
    var resolvedValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(identifier);
    assertThat(resolvedValue).isSameAs(value);
  }

  @Test
  void shouldUnpackSymbolToSafeValue() {
    var symbol = new Symbol(GoNativeType.STRING);
    var identifier = TreeCreationUtils.identifier("myVar");
    var literalValueAssignment = TreeCreationUtils.literal("some_value");
    identifier.setSymbol(symbol);
    var declaration = new Usage(identifier, literalValueAssignment, Usage.UsageType.DECLARATION);
    var reference = new Usage(identifier, null, Usage.UsageType.REFERENCE);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(reference);

    var safeSymbolValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(identifier);
    assertThat(safeSymbolValue).isSameAs(literalValueAssignment);
  }

  @Test
  void shouldNotUnpackSymbolWhenSafeValueIsNull() {
    var symbol = new Symbol(GoNativeType.STRING);
    var identifier = TreeCreationUtils.identifier("myVar");
    identifier.setSymbol(symbol);
    var literalValueAssignment = TreeCreationUtils.literal("some_value");
    var declaration = new Usage(identifier, literalValueAssignment, Usage.UsageType.DECLARATION);
    var literalValueAssignment2 = TreeCreationUtils.literal("some_value_2");
    var assignment = new Usage(identifier, literalValueAssignment2, Usage.UsageType.ASSIGNMENT);
    var reference = new Usage(identifier, null, Usage.UsageType.REFERENCE);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(assignment);
    symbol.getUsages().add(reference);

    var safeSymbolValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(identifier);
    assertThat(safeSymbolValue).isSameAs(identifier);
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnOriginalTreeWhenTryingToUnpack(Tree argument) {
    var safeSymbolValue = SymbolHelper.unpackToSafeSymbolValueIfExisting(argument);
    assertThat(safeSymbolValue).isSameAs(argument);
  }

  static Stream<Arguments> shouldReturnOriginalTreeWhenTryingToUnpack() {
    Tree nullTree = null;

    return Stream.of(
      Arguments.of(TreeCreationUtils.identifier("myVar")),
      Arguments.of(nullTree),
      Arguments.of(new LiteralTreeImpl(null, "nil")));
  }

  @Test
  void shouldReturnSymbolsDeclaration() {
    var symbol = new Symbol("my_type");
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifier = TreeCreationUtils.identifier("my_func");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifier), Usage.UsageType.DECLARATION);
    symbol.getUsages().add(declaration);

    var symbolsDeclaration = SymbolHelper.getDeclaration(symbol);

    assertThat(symbolsDeclaration).isEqualTo(declaration);
  }
}
