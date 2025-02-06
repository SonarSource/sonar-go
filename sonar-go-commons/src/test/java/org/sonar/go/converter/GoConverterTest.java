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
package org.sonar.go.converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.ClassDeclarationTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.ParseException;
import org.sonar.go.api.ReturnTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.go.converter.GoConverter.DefaultCommand.getExecutableForCurrentOS;

class GoConverterTest {

  @Test
  void test_parse_return() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 42}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "42");
  }

  @Test
  void test_parse_binary_notation() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 0b_0010_1010}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "_0010_1010");
  }

  @Test
  void test_parse_imaginary_literals() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {return 6.67428e-11i}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);
  }

  @Test
  void test_parse_embed_overlapping_interfaces() {
    Tree tree = TestGoConverter.parse("package main\ntype A interface{\n     DoX() string\n}\ntype B interface{\n     DoX() \n}\ntype AB interface{\n    A\n    B\n}");
    List<Tree> classList = tree.descendants()
      .filter(t -> t instanceof ClassDeclarationTree)
      .collect(Collectors.toList());
    assertThat(classList).hasSize(3);
  }

  @Test
  void test_parse_infinite_for() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {for {}}");
    List<Tree> returnList = tree.descendants().filter(t -> t instanceof LoopTree).collect(Collectors.toList());
    assertThat(returnList).hasSize(1);
  }

  @Test
  void test_parse_generics() {
    Tree tree = TestGoConverter.parse("package main\nfunc f1[T any]() {}\nfunc f2() {\nf:=f1[string]}");
    List<Tree> functions = tree.descendants().filter(t -> t instanceof FunctionDeclarationTree).collect(Collectors.toList());
    assertThat(functions).hasSize(2);

    List<Tree> f1Children = functions.get(0).children();
    assertThat(f1Children).hasSize(3);
    Tree typeParams = f1Children.get(2);
    assertThat(typeParams).isInstanceOf(NativeTree.class);
    assertThat(((NativeTree) typeParams).nativeKind()).hasToString("TypeParams(FieldList)");

    List<Tree> f2Children = functions.get(1).children();
    Tree f = ((BlockTree) f2Children.get(1)).statementOrExpressions().get(0);
    assertThat(f).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void test_parse_member_select() {
    Tree tree = TestGoConverter.parse("package main\nfunc foo() {fmt.Println()}");
    List<MemberSelectTree> returnList = tree.descendants().filter(MemberSelectTree.class::isInstance).map(MemberSelectTree.class::cast).toList();
    assertThat(returnList).hasSize(1);
    MemberSelectTree memberSelectTree = returnList.get(0);
    assertThat(memberSelectTree.identifier().name()).isEqualTo("Println");
    Tree expression = memberSelectTree.expression();
    assertThat(expression).isInstanceOfSatisfying(IdentifierTree.class, identifierTree -> assertThat(identifierTree.name()).isEqualTo("fmt"));
  }

  @Test
  void parse_error() {
    ParseException e = assertThrows(ParseException.class,
      () -> TestGoConverter.parse("$!#@"));
    assertThat(e).hasMessage("Go parser external process returned non-zero exit value: 2");
  }

  @Test
  void invalid_command() {
    GoConverter.Command command = mock(GoConverter.Command.class);
    when(command.getCommand()).thenReturn(Collections.singletonList("invalid-command"));
    GoConverter converter = new GoConverter(command);
    ParseException e = assertThrows(ParseException.class,
      () -> converter.parse("package main\nfunc foo() {}"));
    assertThat(e).hasMessageContaining("Cannot run program \"invalid-command\"");
  }

  @Test
  void parse_accepted_big_file() {
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[700_000 - code.length()]).replace("\0", "\n");
    Tree tree = TestGoConverter.parse(bigCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  void parse_rejected_big_file() {
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[1_500_000]).replace("\0", "\n");
    ParseException e = assertThrows(ParseException.class,
      () -> TestGoConverter.parse(bigCode));
    assertThat(e).hasMessage("The file size is too big and should be excluded, its size is 1500028 (maximum allowed is 1500000 bytes)");
  }

  @Test
  void load_invalid_executable_path() throws IOException {
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> GoConverter.DefaultCommand.getBytesFromResource("invalid-exe-path"));
    assertThat(e).hasMessage("invalid-exe-path binary not found on class path");
  }

  @Test
  void executable_for_current_os() {
    assertThat(getExecutableForCurrentOS("Linux", "x86_64")).isEqualTo("sonar-go-to-slang-linux-amd64");
    assertThat(getExecutableForCurrentOS("Windows 10", "x86_64")).isEqualTo("sonar-go-to-slang-windows-amd64.exe");
    assertThat(getExecutableForCurrentOS("Mac OS X", "x86_64")).isEqualTo("sonar-go-to-slang-darwin-amd64");
    assertThat(getExecutableForCurrentOS("Mac OS X", "aarch64")).isEqualTo("sonar-go-to-slang-darwin-arm64");
  }

  private List<ReturnTree> getReturnsList(Tree tree) {
    return tree.descendants()
      .filter(t -> t instanceof ReturnTree)
      .map(ReturnTree.class::cast)
      .collect(Collectors.toList());
  }

  private void checkIntegerValue(ReturnTree returnTree, String s) {
    IntegerLiteralTree integerLiteralTree = (IntegerLiteralTree) returnTree.body();
    assertThat(integerLiteralTree.getNumericPart()).isEqualTo(s);
  }
}
