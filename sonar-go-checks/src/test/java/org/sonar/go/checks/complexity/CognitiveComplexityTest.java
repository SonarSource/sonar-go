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
package org.sonar.go.checks.complexity;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveComplexityTest {

  public static final GoConverter parser = TestGoConverter.GO_CONVERTER;

  @Test
  void unrelated_statement() {
    assertThat(complexity("42;").value()).isZero();
  }

  @Test
  void if_statements() {
    assertThat(complexity("if (x) { 42 };").value()).isEqualTo(1);
    assertThat(complexity("if (x) { 42 } else { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { 44 };").value()).isEqualTo(3);
  }

  @Test
  void nested_if_statements() {
    assertThat(complexity("if (x) { 42 };").value()).isEqualTo(1);
    assertThat(complexity("if (x) { 42 } else { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { if (y) { 43 } else { 44 } };").value()).isEqualTo(5);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { 44 };").value()).isEqualTo(3);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { if (y) { 44 } else { 45 } };").value()).isEqualTo(6);

  }

  @Test
  void loop_statements() {
    assertThat(complexity("for (x) { 42 };").value()).isEqualTo(1);
  }

  @Test
  void match_statements() {
    assertThat(complexity("switch x { default: 42 };").value()).isEqualTo(1);
    assertThat(complexity("switch x { case 'a': 0; default: 42; };").value()).isEqualTo(1);
  }

  @Test
  void functions() {
    assertThat(complexityFromFullSample("package main\n func foo() { 42 }").value()).isZero();
    assertThat(complexityFromFullSample("""
      package main
      func foo() {
          add := func(a, b int) int {
          return a + b
        }
      }""").value()).isZero();
  }

  @Test
  void binary_operators() {
    assertThat(complexity("a == b;").value()).isZero();
    assertThat(complexity("a && b;").value()).isEqualTo(1);
    assertThat(complexity("a || b;").value()).isEqualTo(1);
    assertThat(complexity("a && b && c;").value()).isEqualTo(1);
    assertThat(complexity("a || b || c;").value()).isEqualTo(1);
    assertThat(complexity("a || b && c;").value()).isEqualTo(2);
    assertThat(complexity("a || b && c || d;").value()).isEqualTo(3);
  }

  @Test
  void jumps() {
    assertThat(complexity("break;").value()).isZero();
    assertThat(complexity("break foo;").value()).isEqualTo(1);
    assertThat(complexity("for (x) { break; }").value()).isEqualTo(1);
    assertThat(complexity("for (x) { break foo; }").value()).isEqualTo(2);

    assertThat(complexity("continue;").value()).isZero();
    assertThat(complexity("continue foo;").value()).isEqualTo(1);
    assertThat(complexity("for (x) { continue; }").value()).isEqualTo(1);
    assertThat(complexity("for (x) { continue foo; }").value()).isEqualTo(2);
  }

  @Test
  void nesting() {
    assertThat(complexity("if x { a && b }").value()).isEqualTo(2);
    assertThat(complexity("if x { if y { 42 } }").value()).isEqualTo(3);
    assertThat(complexity("for x { if y { 42 } }").value()).isEqualTo(3);
    assertThat(complexity("switch x { default: if y { 42 } }").value()).isEqualTo(3);
    assertThat(complexityFromFullSample("package main\n func foo() { if x { 42 } }").value()).isEqualTo(1);
    assertThat(complexityFromFullSample("package main\n func foo() { f := func() { if x { 42 } } }").value()).isEqualTo(2);
    assertThat(complexity("if x { f := func() { if x { 42 } } }").value()).isEqualTo(4);
  }

  @Test
  void nesting_with_classes() {
    assertThat(complexityFromFullSample("""
      package main
      type T struct {
        x int
      }
      func (t T) foo() {
        if x {
          if y {
            42
          }
        }
      }""").value()).isEqualTo(3);
  }

  @Test
  void nesting_with_functions() {
    String packageCode = "package main\n";
    assertThat(complexityFromFullSample(packageCode + "func foo() { if x { a && b; } }").value()).isEqualTo(2);
    assertThat(complexityFromFullSample(packageCode + "func foo() { f := func() { if x { a && b; } } }").value()).isEqualTo(3);
  }

  private CognitiveComplexity complexity(String code) {
    return complexityFromFullSample(String.format("""
      package main

      func main() {
        %s
      }""", code));
  }

  private CognitiveComplexity complexityFromFullSample(String code) {
    Tree tree = parser.parse(Map.of("foo.go", code)).get("foo.go").tree();
    return new CognitiveComplexity(tree);
  }
}
