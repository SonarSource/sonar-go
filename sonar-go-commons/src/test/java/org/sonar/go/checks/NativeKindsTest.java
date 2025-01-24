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
package org.sonar.go.checks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NativeKindsTest {

  @Test
  void shouldBeStringNativeKindOfTypeA() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "TypeA");
    assertThat(result).isTrue();
  }

  @Test
  void shouldNotBeStringNativeKindOfTypeWhenAnotherType() {
    var kind = new StringNativeKind("TypeA");
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "AnotherType");
    assertThat(result).isFalse();
  }

  @Test
  void shouldBeStringNativeKindOfTypeWhenAnotherNativeKind() {
    var kind = mock(NativeKind.class);
    var tree = new NativeTreeImpl(mock(TreeMetaData.class), kind, List.of());
    var result = NativeKinds.isStringNativeKindOfType(tree, "TypeA");
    assertThat(result).isFalse();
  }
}
