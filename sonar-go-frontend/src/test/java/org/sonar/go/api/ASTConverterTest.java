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
package org.sonar.go.api;

import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;

import static org.junit.jupiter.api.Assertions.assertSame;

class ASTConverterTest {

  private static final ASTConverter DUMMY_CONVERTER = new ASTConverter() {
    private final Tree SIMPLE_TREE = TreeCreationUtils.identifier("name");

    @Override
    public Tree parse(String content) {
      return SIMPLE_TREE;
    }
  };

  @Test
  void parse_with_file_has_no_effect_by_default() {
    assertSame(DUMMY_CONVERTER.parse(""), DUMMY_CONVERTER.parse("", "file name"));
  }

}
