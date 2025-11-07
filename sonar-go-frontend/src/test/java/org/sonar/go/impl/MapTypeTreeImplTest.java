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
import org.sonar.plugins.go.api.MapTypeTree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.assertj.core.api.Assertions.assertThat;

class MapTypeTreeImplTest {
  private static final TreeMetaData META_DATA = null;

  @Test
  void testMapType() {
    var key = TreeCreationUtils.identifier("int", "int");
    var value = TreeCreationUtils.identifier("string", "string");
    MapTypeTree tree = new MapTypeTreeImpl(META_DATA, key, value);
    assertThat(tree.children()).containsExactly(key, value);
    assertThat(tree.key()).isSameAs(key);
    assertThat(tree.value()).isSameAs(value);
    Type type = tree.type();
    assertThat(type.type()).isEqualTo("map[int]string");
    assertThat(type.packageName()).isEmpty();
  }

  @Test
  void testMapTypeWithArrayTypeAsValue() {
    var key = TreeCreationUtils.identifier("string", "string");
    var value = new ArrayTypeTreeImpl(META_DATA, null, TreeCreationUtils.identifier("int", "int"));
    MapTypeTree tree = new MapTypeTreeImpl(META_DATA, key, value);
    assertThat(tree.type().type()).isEqualTo("map[string][]int");
  }

  @Test
  void testMapTypeWithMapTypeAsValue() {
    var key = TreeCreationUtils.identifier("string", "string");
    var value = new MapTypeTreeImpl(META_DATA, TreeCreationUtils.identifier("byte", "byte"), TreeCreationUtils.identifier("int", "int"));
    MapTypeTree tree = new MapTypeTreeImpl(META_DATA, key, value);
    assertThat(tree.type().type()).isEqualTo("map[string]map[byte]int");
  }
}
