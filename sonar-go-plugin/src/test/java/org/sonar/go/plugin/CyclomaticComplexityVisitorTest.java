/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonar.go.plugin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.testing.TestGoConverter;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class CyclomaticComplexityVisitorTest {

  private static List<HasTextRange> getComplexityTrees(String content) {
    Tree root = TestGoConverter.parse(content);
    return new CyclomaticComplexityVisitor().complexityTrees(root);
  }

  @Test
  void test_matchCases() {
    String content = """
      package main

      func foo(a int) string {
        switch a {
        case 0:
          return "none"
        case 1:
          return "one"
        case 2:
          return "many"
        default:
          return "it's complicated"
        }
      }""";
    List<HasTextRange> trees = getComplexityTrees(content);
    assertThat(trees)
      .hasSize(4);
    trees.remove(0);
    assertThat(trees)
      .allMatch(tree -> tree instanceof MatchCaseTree);
  }

  @Test
  void test_functions_with_conditional() {
    String content = """
      package main
      func foo(a int) {
        if a == 2 {
          print(a + 1)
        } else {
          print(a)
        }
      }
      """;
    List<HasTextRange> trees = getComplexityTrees(content);
    assertThat(trees).hasSize(2);
    assertThat(trees.get(0)).isInstanceOf(FunctionDeclarationTree.class);
    assertThat(trees.get(1)).isInstanceOf(Token.class);
  }

  @Test
  void test_loops() {
    String content = """
      package main
      func foo2() {
        for _, element := range someSlice {
          for (element > y) {
            element = element - 1
          }
        }
      }""";
    List<HasTextRange> trees = getComplexityTrees(content);
    trees.remove(0);
    assertThat(trees)
      .hasSize(2)
      .allMatch(tree -> tree instanceof LoopTree);
  }
}
