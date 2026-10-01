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
package org.sonar.go.impl;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.FieldListTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class FieldListTreeImplTest {

  @Test
  void shouldHoldFieldsAsChildren() {
    var name = TreeCreationUtils.identifier("first");
    var type = TreeCreationUtils.identifier("int");
    var field = TreeCreationUtils.field(List.of(name), type);
    FieldListTree fieldList = TreeCreationUtils.fieldList(field);

    assertThat(fieldList.fields()).containsExactly(field);
    assertThat(fieldList.children()).containsExactly(field);
    assertThat(field.names()).containsExactly(name);
    assertThat(field.type()).isSameAs(type);
    assertThat(field.children()).containsExactly(name, type);
  }

  @Test
  void shouldHoldNoTypeWhenTheFieldHasNone() {
    var name = TreeCreationUtils.identifier("T");
    var field = TreeCreationUtils.field(List.of(name), null);

    assertThat(field.type()).isNull();
    assertThat(field.children()).containsExactly(name);
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

    assertThat(function.receiver().fields()).hasSize(1);
    assertThat(function.receiver().names()).map(IdentifierTree::name).containsExactly("receiver");
    assertThat(function.returnType().fields()).hasSize(2);
    assertThat(function.returnType().names()).map(IdentifierTree::name).containsExactly("result", "err");
  }

  @Test
  void shouldProvideNoNameForUnnamedResults() {
    var function = parseFunction("""
      package main
      func method() (int, error) {
        return 0, nil
      }
      """);

    assertThat(function.returnType().fields()).hasSize(2);
    assertThat(function.returnType().names()).isEmpty();
    assertThat(function.returnType().fields())
      .map(field -> ((IdentifierTree) field.type()).name())
      .containsExactly("int", "error");
  }

  @Test
  void shouldProvideEveryNameOfFieldDeclaringSeveralOnes() {
    var function = parseFunction("""
      package main
      func method() (first, second int) {
        return 0, 0
      }
      """);

    assertThat(function.returnType().fields()).hasSize(1);
    assertThat(function.returnType().names()).map(IdentifierTree::name).containsExactly("first", "second");
  }

  @Test
  void shouldProvideTypeParametersOfFunctionAndTypeDeclaration() {
    var code = """
      package main
      type pair[key comparable, value any] struct{}
      func compare[first int, second int](a first, b second) {}
      """;
    var typeDeclaration = TestGoConverterSingleFile.parseAndRetrieve(ClassDeclarationTree.class, code);
    var function = TestGoConverterSingleFile.parseAndRetrieve(FunctionDeclarationTree.class, code);

    assertThat(typeDeclaration.typeParameters().names()).map(IdentifierTree::name).containsExactly("key", "value");
    assertThat(function.typeParameters().names()).map(IdentifierTree::name).containsExactly("first", "second");
  }

  @Test
  void shouldProvideNoTypeParameterWhenThereIsNone() {
    var code = """
      package main
      type single struct{}
      func plain() {}
      """;

    assertThat(TestGoConverterSingleFile.parseAndRetrieve(ClassDeclarationTree.class, code).typeParameters()).isNull();
    var function = TestGoConverterSingleFile.parseAndRetrieve(FunctionDeclarationTree.class, code);
    assertThat(function.typeParameters()).isNull();
    assertThat(function.receiver()).isNull();
    assertThat(function.returnType()).isNull();
  }

  @Test
  void shouldProvideNoNameForAnEmptyFieldList() {
    FieldListTree fieldList = TreeCreationUtils.fieldList();

    assertThat(fieldList.fields()).isEmpty();
    assertThat(fieldList.names()).isEmpty();
    assertThat(TreeCreationUtils.field(emptyList(), null).names()).isEmpty();
  }

  private static FunctionDeclarationTree parseFunction(String code) {
    return TestGoConverterSingleFile.parseAndRetrieve(FunctionDeclarationTree.class, code);
  }
}
