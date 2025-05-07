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
import org.sonar.plugins.go.api.Token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.impl.TextRanges.range;

class EllipsisTreeImplTest {

  @Test
  void testEllipsisAsArgumentType() {
    var element = new IdentifierTreeImpl(null, "name", "my_type", "my_package", 1);
    var ellipsisToken = new TokenImpl(range(5, 1, 5, 4), "...", Token.Type.OTHER);

    var ellipsisTree = new EllipsisTreeImpl(null, ellipsisToken, element);

    assertThat(ellipsisTree.children()).hasSize(1);
    assertThat(ellipsisTree.ellipsis()).isSameAs(ellipsisToken);
    assertThat(ellipsisTree.element()).isSameAs(element);
  }

  @Test
  void testEllipsisAsArrayType() {
    var ellipsisToken = new TokenImpl(range(5, 1, 5, 4), "...", Token.Type.OTHER);

    var ellipsisTree = new EllipsisTreeImpl(null, ellipsisToken, null);

    assertThat(ellipsisTree.children()).isEmpty();
    assertThat(ellipsisTree.ellipsis()).isSameAs(ellipsisToken);
    assertThat(ellipsisTree.element()).isNull();
  }

}
