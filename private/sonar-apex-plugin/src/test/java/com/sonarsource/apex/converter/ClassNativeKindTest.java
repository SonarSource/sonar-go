/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.ast.ClassDecl;
import apex.jorje.data.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ClassNativeKindTest {

  @Test
  void test_equals() {
    ClassNativeKind kind = new ClassNativeKind(CompilationUnit.class);
    assertThat(kind)
      .isEqualTo(kind)
      .isEqualTo(new ClassNativeKind(CompilationUnit.class))
      .isNotEqualTo(new ClassNativeKind(ClassDecl.class))
      .isNotEqualTo(new Object())
      .isNotEqualTo(null)
      .hasSameHashCodeAs(new ClassNativeKind(CompilationUnit.class))
      .hasToString("CompilationUnit");
  }
}
