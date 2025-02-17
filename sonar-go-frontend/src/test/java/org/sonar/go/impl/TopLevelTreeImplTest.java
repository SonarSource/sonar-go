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
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;

class TopLevelTreeImplTest {

  @Test
  void doesImportTypeSupportSingleType() {
    var code = """
      package main
      import (
        "fmt"
      )
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("fmt")).isTrue();
    assertThat(tree.doesImportType("os")).isFalse();
  }

  @Test
  void doesImportTypeSupportSingleTypeWithoutParenthesis() {
    var code = """
      package main
      import "fmt"
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("fmt")).isTrue();
    assertThat(tree.doesImportType("os")).isFalse();
  }

  @Test
  void doesImportTypeSupportMultipleType() {
    var code = """
      package main
      import (
        "fmt"
        "os"
      )
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("fmt")).isTrue();
    assertThat(tree.doesImportType("os")).isTrue();
  }

  @Test
  void doesImportTypeWithoutAnyImports() {
    var code = """
      package main
      import (
      )
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("fmt")).isFalse();
  }

  @Test
  void doesImportTypeDoesNotSupportAlias() {
    var code = """
      package main
      import f "fmt"
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("fmt")).isFalse();
  }

  @Test
  void doesImportTypeDoesNotSupportDotImport() {
    var code = """
      package main
      import . "os"
      """;
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    assertThat(tree.doesImportType("os")).isFalse();
  }
}
