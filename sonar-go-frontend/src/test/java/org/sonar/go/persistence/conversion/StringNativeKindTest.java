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
package org.sonar.go.persistence.conversion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringNativeKindTest {

  @Test
  void constructor() {
    assertThat(new StringNativeKind("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of(null)).isNull();
  }

  @Test
  void kind() {
    assertThat(new StringNativeKind("ast.Element").kind()).isEqualTo("ast.Element");
    assertThat(StringNativeKind.of("ast.Element").kind()).isEqualTo("ast.Element");
    assertThat(StringNativeKind.toString(new StringNativeKind("ast.Element"))).isEqualTo("ast.Element");
  }

  @Test
  void toStringShouldReturnNullOnNullKind() {
    assertThat(StringNativeKind.toString(null)).isNull();
  }

  @Test
  void test_equals() {
    assertThat(new StringNativeKind("ast.Element")).isEqualTo(new StringNativeKind("ast.Element"));
    assertThat(new StringNativeKind("ast.Element")).hasSameHashCodeAs(new StringNativeKind("ast.Element"));
  }

}
