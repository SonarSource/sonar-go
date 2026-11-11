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
package org.sonar.go.utils;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.LoopTree;

import static org.assertj.core.api.Assertions.assertThat;

class VariableHelperTest {

  @Test
  void shouldProvideSingleVariableWithValue() {
    var identifier = TreeCreationUtils.identifier("a");
    var value = new IntegerLiteralTreeImpl(null, "1");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier), null, List.of(value), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(1);
    var variable = variables.get(0);
    assertThat(variable.identifier()).isSameAs(identifier);
    assertThat(variable.value()).isSameAs(value);
  }

  @Test
  void shouldProvideSingleVariableNoValue() {
    var identifier = TreeCreationUtils.identifier("a");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier), null, List.of(), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(1);
    var variable = variables.get(0);
    assertThat(variable.identifier()).isSameAs(identifier);
    assertThat(variable.value()).isNull();
  }

  @Test
  void shouldProvideMultipleVariablesWithValues() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var value1 = new IntegerLiteralTreeImpl(null, "1");
    var value2 = new IntegerLiteralTreeImpl(null, "2");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(value1, value2), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isSameAs(value1);
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isSameAs(value2);
  }

  @Test
  void shouldProvideMultipleVariablesNoValues() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isNull();
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isNull();
  }

  @Test
  void shouldProvideMultipleVariablesSingleValue() {
    var identifier1 = TreeCreationUtils.identifier("a");
    var identifier2 = TreeCreationUtils.identifier("b");
    var value = new IntegerLiteralTreeImpl(null, "1");
    var variableDeclaration = new VariableDeclarationTreeImpl(null, List.of(identifier1, identifier2), null, List.of(value), false);

    var variables = VariableHelper.getVariables(variableDeclaration);
    assertThat(variables).hasSize(2);
    var variable1 = variables.get(0);
    assertThat(variable1.identifier()).isSameAs(identifier1);
    assertThat(variable1.value()).isSameAs(value);
    var variable2 = variables.get(1);
    assertThat(variable2.identifier()).isSameAs(identifier2);
    assertThat(variable2.value()).isSameAs(value);
  }

  @Test
  void shouldProvideNothingWithoutFieldList() {
    assertThat(VariableHelper.getFields(null)).isEmpty();
    assertThat(VariableHelper.getFieldNames(null)).isEmpty();
  }

  @Test
  void shouldProvideNamesOfReceiverAndNamedResults() {
    var function = parseFunction("""
      package main
      type box struct{}
      func (receiver *box) method(first int, second string) (result int, err error) {
        return 0, nil
      }
      """);

    assertThat(VariableHelper.getFields(function.receiver())).hasSize(1);
    assertThat(VariableHelper.getFieldNames(function.receiver()).map(IdentifierTree::name)).containsExactly("receiver");
    assertThat(VariableHelper.getFields(function.returnType())).hasSize(2);
    assertThat(VariableHelper.getFieldNames(function.returnType()).map(IdentifierTree::name)).containsExactly("result", "err");
  }

  @Test
  void shouldProvideNoNameForUnnamedResults() {
    var function = parseFunction("""
      package main
      func method() (int, error) {
        return 0, nil
      }
      """);

    assertThat(VariableHelper.getFields(function.returnType())).hasSize(2);
    assertThat(VariableHelper.getFieldNames(function.returnType())).isEmpty();
  }

  @Test
  void shouldProvideEveryNameOfFieldDeclaringSeveralOnes() {
    var function = parseFunction("""
      package main
      func method() (first, second int) {
        return 0, 0
      }
      """);

    assertThat(VariableHelper.getFields(function.returnType())).hasSize(1);
    assertThat(VariableHelper.getFieldNames(function.returnType()).map(IdentifierTree::name)).containsExactly("first", "second");
  }

  @Test
  void shouldDistinguishRangeTargetsFromTheIterable() {
    var loop = TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, """
      package main
      func f(items []int) {
        for index, value := range items { println(index, value) }
      }
      """);
    var variables = VariableHelper.getRangeVariables(loop);
    assertThat(variables).extracting(variable -> variable.identifier().name()).containsExactly("index", "value");
    assertThat(variables).allMatch(VariableHelper.RangeVariable::declaration);
  }

  @Test
  void shouldDistinguishRangeAssignmentsFromDeclarations() {
    var loop = TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, """
      package main
      func f(items []int) {
        var index int
        for index = range items { println(index) }
      }
      """);
    var variables = VariableHelper.getRangeVariables(loop);
    assertThat(variables).extracting(variable -> variable.identifier().name()).containsExactly("index");
    assertThat(variables).noneMatch(VariableHelper.RangeVariable::declaration);
  }

  @Test
  void shouldIgnoreNestedAssignmentsWhenIdentifyingRangeTargets() {
    for (String targets : List.of(
      "target, slots[func() int { offset := 0; return offset }()]",
      "slots[func() int { offset := 0; return offset }()], target",
      "target, slots[func() int { var offset int; offset = 0; return offset }()]",
      "slots[func() int { var offset int; offset = 0; return offset }()], target")) {
      var loop = TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, """
        package main
        func f(items, slots []int) {
          var target int
          for %s = range items { println(target) }
        }
        """.formatted(targets));
      var variables = VariableHelper.getRangeVariables(loop);
      assertThat(variables).extracting(variable -> variable.identifier().name()).containsExactly("target");
      assertThat(variables).noneMatch(VariableHelper.RangeVariable::declaration);
    }
  }

  @Test
  void shouldIgnoreLoopsWithoutRangeVariables() {
    for (String statement : List.of("for {}", "for true {}", "for range items {}")) {
      var loop = TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, """
        package main
        func f(items []int) {
          %s
        }
        """.formatted(statement));
      assertThat(VariableHelper.getRangeVariables(loop)).isEmpty();
    }
  }

  private static FunctionDeclarationTree parseFunction(String code) {
    return TestGoConverterSingleFile.parseAndRetrieve(FunctionDeclarationTree.class, code);
  }
}
