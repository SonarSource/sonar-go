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
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.MapTypeTree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class MapTypeTreeImplTest {

  @Test
  void testMapType() {
    TreeMetaData meta = null;
    var key = TreeCreationUtils.identifier("key");
    var value = TreeCreationUtils.integerLiteral("1");
    MapTypeTree tree = new MapTypeTreeImpl(meta, key, value);
    assertThat(tree.children()).containsExactly(key, value);
    assertThat(tree.key()).isSameAs(key);
    assertThat(tree.value()).isSameAs(value);
  }
}
