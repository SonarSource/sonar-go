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
package org.sonar.go.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.FunctionUtils.hasFunctionCallFullNameIgnoreCase;
import static org.sonar.go.utils.FunctionUtils.hasFunctionCallNameIgnoreCase;

class FunctionUtilsTest {
  private class TypeNativeKind implements NativeKind {
  }

  private static TreeMetaData meta = null;
  private static IdentifierTree identifierTree = new IdentifierTreeImpl(meta, "function");
  private static List<Tree> args = new ArrayList<>();

  @Test
  void test_has_function_name_identifier() {
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "FuNcTiOn")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "mySuperFunction")).isFalse();
  }

  @Test
  void test_has_function_name_method_select() {
    Tree member = new IdentifierTreeImpl(meta, "A");
    Tree methodSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, methodSelect, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "A")).isFalse();
  }

  @Test
  void test_has_function_name_unknown() {
    Tree nativeNode = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, nativeNode, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isFalse();
  }

  @Test
  void test_has_function_full_name_identifier() {
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "FuNcTioN")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "mySuperFunction")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree)).isFalse();
  }

  @Test
  void test_has_function_full_name_method_select() {
    IdentifierTree memberA = new IdentifierTreeImpl(meta, "A");
    IdentifierTree memberB = new IdentifierTreeImpl(meta, "B");
    Tree methodSelectAB = new MemberSelectTreeImpl(meta, memberA, memberB);
    Tree methodSelect = new MemberSelectTreeImpl(meta, methodSelectAB, identifierTree);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, methodSelect, args);

    assertThat(hasFunctionCallFullNameIgnoreCase(tree)).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "B")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "B", "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B", "function")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B", "function", "C")).isFalse();
  }

  @Test
  void test_has_function_full_name_unknown() {
    Tree nativeNode = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, nativeNode, args);
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isFalse();
  }

  @Test
  void test_get_strings_tokens_returns_tokens() {
    String code = """
      package main

      func fooBar() {
          var a = "one,two,three";
          foo("one,two$four");
      }""";

    Tree tree = TestGoConverter.GO_CONVERTER.parse(code, null);
    FunctionDeclarationTree root = (FunctionDeclarationTree) tree.children().get(1);

    Set<String> tokens = FunctionUtils.getStringsTokens(root, ",|\\$");

    assertThat(tokens).containsExactlyInAnyOrder("one", "two", "three", "four");
  }
}
