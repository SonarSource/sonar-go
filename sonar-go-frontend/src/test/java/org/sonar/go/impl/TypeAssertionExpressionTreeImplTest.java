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
package org.sonar.go.impl;

import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class TypeAssertionExpressionTreeImplTest {
  @Test
  void testTypeAssertion() {
    TreeMetaData meta = null;
    var x = TreeCreationUtils.identifier("x");
    var type = TreeCreationUtils.identifier("type");
    TypeAssertionExpressionTreeImpl typeAssertion = new TypeAssertionExpressionTreeImpl(meta, x, type);
    assertThat(typeAssertion.children()).containsExactly(x, type);
    assertThat(typeAssertion.expression()).isSameAs(x);
    assertThat(typeAssertion.type()).isSameAs(type);
  }

  @Test
  void testTypeAssertionWithoutType() {
    TreeMetaData meta = null;
    var x = TreeCreationUtils.identifier("x");
    TypeAssertionExpressionTreeImpl typeAssertion = new TypeAssertionExpressionTreeImpl(meta, x, null);
    assertThat(typeAssertion.children()).containsExactly(x);
    assertThat(typeAssertion.expression()).isSameAs(x);
    assertThat(typeAssertion.type()).isNull();
  }
}
