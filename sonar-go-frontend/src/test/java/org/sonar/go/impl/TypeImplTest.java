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

import static org.assertj.core.api.Assertions.assertThat;

class TypeImplTest {

  @Test
  void testTypeAndPackageName() {
    TypeImpl type = new TypeImpl("MyType", "main");
    assertThat(type.type()).isEqualTo("MyType");
    assertThat(type.packageName()).isEqualTo("main");
  }

  @Test
  void testNullType() {
    TypeImpl type = new TypeImpl(null, "main");
    assertThat(type.type()).isNull();
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

  @Test
  void testIsTypeOfNullType() {
    TypeImpl type = new TypeImpl(null, "main");
    assertThat(type.isTypeOf("MyType")).isFalse();
  }
}
