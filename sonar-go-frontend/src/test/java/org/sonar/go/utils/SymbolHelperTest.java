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
import org.junit.jupiter.api.Test;
import org.sonar.go.symbols.GoNativeType;
import org.sonar.go.symbols.Scope;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolHelperTest {

  @Test
  void shouldReturnDeclarationMethodName() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifier = TreeCreationUtils.identifier("my_func");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifier), Usage.UsageType.DECLARATION);
    symbol.getUsages().add(declaration);

    Optional<String> lastAssignedMethodCall = SymbolHelper.getLastAssignedMethodCall(symbol);
    assertThat(lastAssignedMethodCall).contains("my_func");
  }

  @Test
  void shouldReturnNullWhenDeclarationHasNoValue() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var declaration = new Usage(usageIdentifier, null, Usage.UsageType.DECLARATION);
    symbol.getUsages().add(declaration);

    Optional<String> lastAssignedMethodCall = SymbolHelper.getLastAssignedMethodCall(symbol);
    assertThat(lastAssignedMethodCall).isEmpty();
  }

  @Test
  void shouldReturnDeclarationMethodNameIgnoringReference() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifier = TreeCreationUtils.identifier("my_func");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifier), Usage.UsageType.DECLARATION);
    var reference = new Usage(usageIdentifier, null, Usage.UsageType.REFERENCE);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(reference);

    Optional<String> lastAssignedMethodCall = SymbolHelper.getLastAssignedMethodCall(symbol);
    assertThat(lastAssignedMethodCall).contains("my_func");
  }

  @Test
  void shouldReturnLastAssignmentMethodName() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifierDeclaration = TreeCreationUtils.identifier("my_func_declaration");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifierDeclaration), Usage.UsageType.DECLARATION);
    var functionIdentifierAssignment = TreeCreationUtils.identifier("my_func_assignment");
    var assignment = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifierAssignment), Usage.UsageType.ASSIGNMENT);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(assignment);

    Optional<String> lastAssignedMethodCall = SymbolHelper.getLastAssignedMethodCall(symbol);
    assertThat(lastAssignedMethodCall).contains("my_func_assignment");
  }

  @Test
  void shouldReturnLastDeclarationAsAssignmentIsNotFunctionInvocation() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifierDeclaration = TreeCreationUtils.identifier("my_func_declaration");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifierDeclaration), Usage.UsageType.DECLARATION);
    var literalValueAssignment = TreeCreationUtils.literal("some_value");
    var assignment = new Usage(usageIdentifier, literalValueAssignment, Usage.UsageType.ASSIGNMENT);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(assignment);

    Optional<String> lastAssignedMethodCall = SymbolHelper.getLastAssignedMethodCall(symbol);
    assertThat(lastAssignedMethodCall).contains("my_func_declaration");
  }

  @Test
  void shouldReturnLastAssignedValue() {
    var symbol = new Symbol("my_type", Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    var functionIdentifier = TreeCreationUtils.identifier("my_func");
    var declaration = new Usage(usageIdentifier, TreeCreationUtils.simpleFunctionCall(functionIdentifier), Usage.UsageType.DECLARATION);
    var literalValueAssignment = TreeCreationUtils.literal("some_value");
    var assignment = new Usage(usageIdentifier, literalValueAssignment, Usage.UsageType.ASSIGNMENT);
    symbol.getUsages().add(declaration);
    symbol.getUsages().add(assignment);

    var lastAssignedValue = SymbolHelper.getLastAssignedValue(symbol);
    assertThat(lastAssignedValue).isPresent();
  }

  @Test
  void shouldReturnEmptyIfNoAssignedValue() {
    var symbol = new Symbol("my_type", Scope.BLOCK);

    var lastAssignedValue = SymbolHelper.getLastAssignedValue(symbol);

    assertThat(lastAssignedValue).isEmpty();
  }

  @Test
  void shouldResolveStringValueFromStringLiteral() {
    var stringLiteral = TreeCreationUtils.stringLiteral("\"my_string\"");
    var resolvedValue = SymbolHelper.resolveStringValue(stringLiteral);

    assertThat(resolvedValue).isEqualTo("my_string");
  }

  @Test
  void shouldResolveStringValueFromSymbol() {
    var symbol = new Symbol(GoNativeType.STRING, Scope.BLOCK);
    var usageIdentifier = TreeCreationUtils.identifier("a");
    usageIdentifier.setSymbol(symbol);
    var stringLiteral = TreeCreationUtils.stringLiteral("\"my_string\"");
    var declaration = new Usage(usageIdentifier, stringLiteral, Usage.UsageType.DECLARATION);
    symbol.getUsages().add(declaration);

    var resolvedValue = SymbolHelper.resolveStringValue(usageIdentifier);

    assertThat(resolvedValue).isEqualTo("my_string");
  }

  @Test
  void shouldReturnCurrentTreeIfNotAnIdentifier() {
    var stringLiteral = TreeCreationUtils.stringLiteral("\"my_string\"");
    var resolvedValue = SymbolHelper.resolveValue(stringLiteral);
    assertThat(resolvedValue).isSameAs(stringLiteral);
  }

  @Test
  void shouldReturnCurrentIdentifierIfNoSymbolIsPresent() {
    var identifier = TreeCreationUtils.identifier("a");
    var resolvedValue = SymbolHelper.resolveValue(identifier);
    assertThat(resolvedValue).isSameAs(identifier);
  }

  @Test
  void shouldReturnCurrentIdentifierIfNoVlueIsAssigned() {
    var symbol = new Symbol("int", Scope.FUNCTION);
    var identifier = TreeCreationUtils.identifier("a");
    symbol.getUsages().add(new Usage(identifier, null, Usage.UsageType.DECLARATION));
    identifier.setSymbol(symbol);
    var resolvedValue = SymbolHelper.resolveValue(identifier);
    assertThat(resolvedValue).isSameAs(identifier);
  }

  @Test
  void shouldReturnAssignedValue() {
    var symbol = new Symbol("int", Scope.FUNCTION);
    var value = TreeCreationUtils.integerLiteral("5");
    var identifier = TreeCreationUtils.identifier("a");
    symbol.getUsages().add(new Usage(identifier, value, Usage.UsageType.DECLARATION));
    identifier.setSymbol(symbol);
    var resolvedValue = SymbolHelper.resolveValue(identifier);
    assertThat(resolvedValue).isSameAs(value);
  }
}
