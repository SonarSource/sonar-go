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
import org.sonar.plugins.go.api.PlaceHolderTree;
import org.sonar.plugins.go.api.Token;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceHolderTreeImplTest {

  @Test
  void test_place_holder() {
    TokenImpl keyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 1), "_", Token.Type.OTHER);
    PlaceHolderTree placeHolderTree = new PlaceHolderTreeImpl(null, keyword);
    assertThat(placeHolderTree.children()).isEmpty();
    assertThat(placeHolderTree.placeHolderToken().text()).isEqualTo("_");
    assertThat(placeHolderTree.placeHolderToken().type()).isEqualTo(Token.Type.OTHER);
  }
}
