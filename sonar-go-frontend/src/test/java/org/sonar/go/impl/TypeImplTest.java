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
package org.sonar.go.impl;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TypeImplTest {

  @Test
  void testTypeAndPackageName() {
    TypeImpl type = new TypeImpl("MyType", "main");
    assertThat(type.type()).isEqualTo("MyType");
    assertThat(type.packageName()).isEqualTo("main");
  }

  @Test
  void testIsTypeOfSimpleType() {
    TypeImpl type = new TypeImpl("MyType", "main");
    assertThat(type.isTypeOf("MyType")).isTrue();
    assertThat(type.isTypeOf("int")).isFalse();
  }

  @Test
  void testIsTypeOfPointer() {
    TypeImpl type = new TypeImpl("*MyType", "main");
    assertThat(type.isTypeOf("MyType")).isTrue();
    assertThat(type.isTypeOf("*MyType")).isFalse();
    assertThat(type.isTypeOf("int")).isFalse();
    assertThat(type.isTypeOf("AnotherType")).isFalse();
  }

  @Test
  void testIsTypeOfReference() {
    TypeImpl type = new TypeImpl("&MyType", "main");
    assertThat(type.isTypeOf("MyType")).isTrue();
    assertThat(type.isTypeOf("&MyType")).isFalse();
    assertThat(type.isTypeOf("*MyType")).isFalse();
    assertThat(type.isTypeOf("int")).isFalse();
    assertThat(type.isTypeOf("AnotherType")).isFalse();
  }

  static Stream<Arguments> testCreateFromType() {
    return of(
      arguments("int", "int", ""),
      arguments("string", "string", ""),
      arguments("MyType", "MyType", ""),
      arguments("foo.MyType", "foo.MyType", "foo"),
      arguments("*foo.MyType", "*foo.MyType", "foo"),
      arguments("&foo.MyType", "&foo.MyType", "foo"),
      arguments("example.com/foo/bar.MyType", "example.com/foo/bar.MyType", "example.com/foo/bar"),
      arguments("&example.com/foo/bar.MyType", "&example.com/foo/bar.MyType", "example.com/foo/bar"),
      arguments("*example.com/foo/bar.MyType", "*example.com/foo/bar.MyType", "example.com/foo/bar"));
  }

  @ParameterizedTest
  @MethodSource
  void testCreateFromType(String text, String expectedType, String expectedPackageName) {
    var actual = TypeImpl.createFromType(text);
    assertThat(actual.type()).isEqualTo(expectedType);
    assertThat(actual.packageName()).isEqualTo(expectedPackageName);
  }
}
