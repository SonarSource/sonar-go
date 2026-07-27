/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveComplexityTest {

  public static final GoConverter parser = TestGoConverterSingleFile.GO_CONVERTER;

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
  void error_check_guard_pattern() {
    String packageCode = """
      package main
      func mayFail() error {
        return nil
      }
      """;

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        err := mayFail()
        if err != nil {
          return
        }
      }""").value()).isZero();

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        err := mayFail()
        if err == nil {
          return
        }
      }""").value()).isZero();

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        if err := mayFail(); err != nil {
          return
        }
      }""").value()).isZero();

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        err := mayFail()
        if err != nil {
          if err != nil {
            return
          }
        }
      }""").value()).isZero();
  }

  @Test
  void error_check_guard_pattern_does_not_apply_with_else_or_unrelated_nil_checks() {
    String packageCode = """
      package main
      func mayFail() error {
        return nil
      }
      func getPointer() *int {
        return nil
      }
      """;

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        err := mayFail()
        if err != nil {
          return
        } else {
          return
        }
      }""").value()).isEqualTo(2);

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        p := getPointer()
        if p != nil {
          return
        }
      }""").value()).isEqualTo(1);

    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        err := mayFail()
        if err != nil {
          p := getPointer()
          if p != nil {
            return
          }
          return
        }
      }""").value()).isEqualTo(1);
  }

  @Test
  void error_check_guard_pattern_with_multi_return_value() {
    String packageCode = """
      package main
      func mayFail() (*int, error) {
        return nil, nil
      }
      """;

    // two error-check guards one after another, each on its own call result
    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        p1, err := mayFail()
        if err != nil {
          return
        }
        p2, err := mayFail()
        if err != nil {
          return
        }
        _ = p1
        _ = p2
      }""").value()).isZero();

    // error check combined with another condition is not a guard clause and counts normally
    assertThat(complexityFromFullSample(packageCode + """
      func foo() {
        p, err := mayFail()
        if err != nil || p == nil {
          return
        }
      }""").value()).isEqualTo(2);
  }

  @Test
  void error_check_guard_pattern_with_standard_library_function() {
    assertThat(complexityFromFullSample("""
      package main
      import "strconv"
      func foo() {
        n, err := strconv.Atoi("42")
        if err != nil {
          return
        }
        _ = n
      }""").value()).isZero();
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
    Tree tree = parser.parse(Map.of("foo.go", code), "moduleName").get("foo.go").tree();
    return new CognitiveComplexity(tree);
  }
}
