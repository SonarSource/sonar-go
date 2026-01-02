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

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.IdentifierTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class GoNativeTypeTest {
  @Test
  void testIsInt() {
    assertTrue(GoNativeType.isInt(GoNativeType.INT));
    assertTrue(GoNativeType.isInt(GoNativeType.INT8));
    assertTrue(GoNativeType.isInt(GoNativeType.INT16));
    assertTrue(GoNativeType.isInt(GoNativeType.INT32));
    assertTrue(GoNativeType.isInt(GoNativeType.INT64));
    assertTrue(GoNativeType.isInt(GoNativeType.UINT));
    assertTrue(GoNativeType.isInt(GoNativeType.UINT8));
    assertTrue(GoNativeType.isInt(GoNativeType.UINT16));
    assertTrue(GoNativeType.isInt(GoNativeType.UINT32));
    assertTrue(GoNativeType.isInt(GoNativeType.UINT64));
    assertTrue(GoNativeType.isInt(GoNativeType.UINTPTR));
    assertFalse(GoNativeType.isInt(GoNativeType.FLOAT32));
    assertFalse(GoNativeType.isInt(GoNativeType.FLOAT64));
    assertFalse(GoNativeType.isInt(GoNativeType.COMPLEX64));
    assertFalse(GoNativeType.isInt(GoNativeType.COMPLEX128));
  }

  @Test
  void isFloat() {
    assertFalse(GoNativeType.isFloat(GoNativeType.INT));
    assertFalse(GoNativeType.isFloat(GoNativeType.INT8));
    assertFalse(GoNativeType.isFloat(GoNativeType.INT16));
    assertFalse(GoNativeType.isFloat(GoNativeType.INT32));
    assertFalse(GoNativeType.isFloat(GoNativeType.INT64));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINT));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINT8));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINT16));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINT32));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINT64));
    assertFalse(GoNativeType.isFloat(GoNativeType.UINTPTR));
    assertTrue(GoNativeType.isFloat(GoNativeType.FLOAT32));
    assertTrue(GoNativeType.isFloat(GoNativeType.FLOAT64));
    assertFalse(GoNativeType.isFloat(GoNativeType.COMPLEX64));
    assertFalse(GoNativeType.isFloat(GoNativeType.COMPLEX128));
  }

  @Test
  void isComplex() {
    assertFalse(GoNativeType.isComplex(GoNativeType.INT));
    assertFalse(GoNativeType.isComplex(GoNativeType.INT8));
    assertFalse(GoNativeType.isComplex(GoNativeType.INT16));
    assertFalse(GoNativeType.isComplex(GoNativeType.INT32));
    assertFalse(GoNativeType.isComplex(GoNativeType.INT64));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINT));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINT8));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINT16));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINT32));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINT64));
    assertFalse(GoNativeType.isComplex(GoNativeType.UINTPTR));
    assertFalse(GoNativeType.isComplex(GoNativeType.FLOAT32));
    assertFalse(GoNativeType.isComplex(GoNativeType.FLOAT64));
    assertTrue(GoNativeType.isComplex(GoNativeType.COMPLEX64));
    assertTrue(GoNativeType.isComplex(GoNativeType.COMPLEX128));
  }

  static Stream<Arguments> shouldReturnProperType() {
    return Stream.of(
      arguments("var a = 5", GoNativeType.DEFAULT_INT),
      arguments("var a = 5.0", GoNativeType.DEFAULT_FLOAT),
      arguments("var a = 5i", GoNativeType.DEFAULT_COMPLEX),
      arguments("var a = \"5\"", GoNativeType.STRING),
      arguments("var a = true", GoNativeType.BOOL),
      arguments("var a = false", GoNativeType.BOOL),

      arguments("var a int = 5", GoNativeType.INT),
      arguments("var a float64 = 5.0", GoNativeType.FLOAT64),
      arguments("var a complex64 = 5i", GoNativeType.COMPLEX64),
      arguments("var a string = \"5\"", GoNativeType.STRING),
      arguments("var a bool = true", GoNativeType.BOOL),
      arguments("var a bool = false", GoNativeType.BOOL),

      arguments("var a int", GoNativeType.INT),
      arguments("var a float64", GoNativeType.FLOAT64),
      arguments("var a complex64", GoNativeType.COMPLEX64),
      arguments("var a string", GoNativeType.STRING),
      arguments("var a bool", GoNativeType.BOOL),
      arguments("var a bool", GoNativeType.BOOL),

      arguments("var a = TRUE", GoNativeType.UNKNOWN),
      arguments("var a = 3 + 5", GoNativeType.INT));
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnProperType(String variableDeclaration, String expectedType) {
    var symbol = parseAndGetSymbol(variableDeclaration);
    assertThat(symbol.getType()).isEqualTo(expectedType);
  }

  private Symbol parseAndGetSymbol(String variableDeclaration) {
    String code = """
      package main
      func main() {
        %s
      }
      """.formatted(variableDeclaration);
    var ast = TestGoConverterSingleFile.parse(code);
    new SymbolVisitor<>().scan(mock(), ast);
    var symbolRef = new AtomicReference<Symbol>();
    var symbolsRetriever = new TreeVisitor<>();
    symbolsRetriever.register(IdentifierTree.class, (ctx, identifier) -> {
      var symbol = identifier.symbol();
      if (symbol != null) {
        symbolRef.set(symbol);
      }
    });
    symbolsRetriever.scan(mock(), ast);
    return symbolRef.get();
  }
}
