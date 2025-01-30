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
package org.sonar.go.checks.utils;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.go.utils.TreeUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class TreeUtilsTest {

  @Test
  void shouldCheckIsNotSemicolon() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var result = TreeUtils.IS_NOT_SEMICOLON.test(initializer);
    assertThat(result).isTrue();
  }

  @Test
  void shouldCheckIsSemicolon() {
    var initializer = new NativeTreeImpl(mock(TreeMetaData.class), new StringNativeKind("Semicolon"), List.of());
    var result = TreeUtils.IS_NOT_SEMICOLON.test(initializer);
    assertThat(result).isFalse();
  }

  @Test
  void shouldGetOnlyIdentifierNames() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = new IdentifierTreeImpl(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), id, null, initializer, false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = new IdentifierTreeImpl(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierNames(List.of(initializer, id, variable, body, id2));

    assertThat(names).containsOnly("foo", "bar");
  }

  @Test
  void shouldGetOnlyIdentifierName() {
    var initializer = new LiteralTreeImpl(mock(TreeMetaData.class), "42");
    var id = new IdentifierTreeImpl(mock(TreeMetaData.class), "foo");
    var variable = new VariableDeclarationTreeImpl(mock(TreeMetaData.class), id, null, initializer, false);
    var body = new BlockTreeImpl(mock(TreeMetaData.class), List.of(variable));
    var id2 = new IdentifierTreeImpl(mock(TreeMetaData.class), "bar");

    var names = TreeUtils.getIdentifierName(List.of(initializer, id, variable, body, id2));

    assertThat(names).isEqualTo("foo.bar");
  }

  @ParameterizedTest
  @MethodSource("shouldGetImports")
  void shouldGetImports(String importsFilePart, List<String> expected) {
    var code = """
      package main
      %s
      func main() {
        fmt.Println("Hello, World!")
      }
      """.formatted(importsFilePart);
    var tree = (TopLevelTree) TestGoConverter.parse(code);

    var imports = TreeUtils.getImportsAsStrings(tree);

    assertThat(imports).containsExactlyInAnyOrderElementsOf(expected);
  }

  static Stream<Arguments> shouldGetImports() {
    return Stream.of(
      arguments("""
        import (
          "fmt"
          "os"
        )
        """, List.of("fmt", "os")),
      arguments("""
        import (
          "fmt"
        )
        """, List.of("fmt")),
      arguments("""
        import (
        )
        """, emptyList()),
      arguments("""
        import "fmt"
        """, List.of("fmt")),
      arguments("""
        import f "fmt"
        """, emptyList()), // These imports are not supported yet
      arguments("""
        import . "os"
        """, emptyList())); // These imports are not supported yet
  }
}
