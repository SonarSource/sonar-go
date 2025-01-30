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
import org.sonar.go.api.NativeTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.CompositeLiteralTest.parse;

class KeyValueTest {
  // Note: more tests indirectly cover KeyValue in CompositeLiteralTest.

  @Test
  void shouldNotParseOtherNativeTree() {
    var compositeLiteralOptional = KeyValue.of((NativeTree) parse("1 | 2"));
    assertThat(compositeLiteralOptional).isEmpty();
  }
}
