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

import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class SliceTreeImplTest {

  @Test
  void testSliceTree() {
    TreeMetaData meta = null;
    var x = TreeCreationUtils.identifier("x");
    var low = TreeCreationUtils.identifier("low");
    var high = TreeCreationUtils.identifier("high");
    boolean isSlice3 = false;
    SliceTreeImpl sliceTree = new SliceTreeImpl(meta, x, low, high, null, isSlice3);
    assertThat(sliceTree.children()).containsExactly(x, low, high);
    assertThat(sliceTree.expression()).isSameAs(x);
    assertThat(sliceTree.low()).isSameAs(low);
    assertThat(sliceTree.high()).isSameAs(high);
    assertThat(sliceTree.max()).isNull();
    assertThat(sliceTree.slice3()).isFalse();
  }

}
