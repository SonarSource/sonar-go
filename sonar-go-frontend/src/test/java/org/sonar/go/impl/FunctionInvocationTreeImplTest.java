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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FunctionInvocationTreeImplTest {

  @Test
  void simple_function_invocation() {
    TreeMetaData meta = null;
    Tree identifierTree = TreeCreationUtils.identifier("x");
    List<Tree> args = new ArrayList<>();

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(tree.children()).containsExactly(identifierTree);
    assertThat(tree.arguments()).isNotNull();
    assertThat(tree.arguments()).isEmpty();
    assertThat(tree.memberSelect()).isEqualTo(identifierTree);
  }

  @Test
  void function_invocation_with_arguments() {
    TreeMetaData meta = null;
    Tree identifierTree = TreeCreationUtils.identifier("x");
    Tree arg1 = TreeCreationUtils.identifier("x");
    Tree arg2 = new LiteralTreeImpl(meta, "x");
    List<Tree> args = new ArrayList<>();
    args.add(arg1);
    args.add(arg2);

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(tree.children()).containsExactly(identifierTree, arg1, arg2);
    assertThat(tree.arguments()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.arguments().get(0)).isEqualTo(arg1);
    assertThat(tree.arguments().get(1)).isEqualTo(arg2);
    assertThat(tree.memberSelect()).isEqualTo(identifierTree);
  }

  @Test
  void function_invocation_with_member_select() {
    TreeMetaData meta = null;
    IdentifierTree identifierTree = TreeCreationUtils.identifier("y");
    Tree member = TreeCreationUtils.identifier("x");
    Tree memberSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    List<Tree> args = new ArrayList<>();

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, memberSelect, args);
    assertThat(tree.children()).containsExactly(memberSelect);
    assertThat(tree.memberSelect()).isEqualTo(memberSelect);
  }

  static Stream<Arguments> shouldVerifyMethodSignature() {
    return of(
      // function arguments, function body, expectedSignature
      // build-in functions
      arguments("", "string(\"abc\")", "string"),
      arguments("", "int16(42)", "int16"),
      arguments("", "float32(123.4)", "float32"),
      arguments("", "float64(123.4)", "float64"),

      // standard library functions
      arguments("array []byte", "bytes.Clone(array)", "bytes.Clone"),
      arguments("array [][]byte", "bytes.Join(array, []byte(\", \"))", "bytes.Join"),
      arguments("array [][]byte, a2 []byte", "bytes.Join(array, a2)", "bytes.Join"),
      arguments("array []byte", "md5.Sum(array)", "crypto/md5.Sum"),

      // functions from external libraries
      arguments("w http.ResponseWriter, cookie http.Cookie", "http.SetCookie(w, &cookie)",
        "net/http.SetCookie"),
      arguments("cookie http.Cookie", "cookie.String()",
        "net/http.Cookie.String"),
      // methods from external libraries
      arguments("s string", "http.ParseSetCookie(s)",
        "net/http.ParseSetCookie"),
      arguments("s string", "http.ParseCookie(s)",
        "net/http.ParseCookie"),
      // sql.DB as a pointer
      arguments("db *sql.DB", "db.Query(fmt.Sprintf(\"SELECT * FROM user WHERE id = %s\", path))",
        "*database/sql.DB.Query"),
      // sql.DB as not a pointer
      arguments("db sql.DB", "db.Query(fmt.Sprintf(\"SELECT * FROM user WHERE id = %s\", path))",
        "database/sql.DB.Query"),
      // *sql.DB.Query() called with other arguments
      arguments("db *sql.DB, id int, name string", "db.Query(\"SELECT * FROM user WHERE id = $1 AND name = $2\", id, name)",
        "*database/sql.DB.Query"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifyMethodSignature(String arguments, String body, String expectedSignature) {
    var code = """
      package main
      import (
        "bytes"
        "crypto/md5"
        "database/sql"
        "fmt"
        "github.com/beego/beego/v2/server/web"
        "net/http"
      )
      func foo(%s) {
        %s
      }
      """.formatted(arguments, body);

    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .findFirst()
      .get();
    assertThat(func.signature("main")).isEqualTo(expectedSignature);
  }

  static Stream<Arguments> shouldVerifyMethodSignatureForCustomFunctions() {
    return of(
      arguments("", "foo()", "func foo() {}"),
      arguments("text string", "foo(text)", "func foo(t string) {}"),
      arguments("i int", "foo(i)", "func foo(number int) {}"),
      arguments("f float32", "foo(f)", "func foo(number float32) {}"),
      arguments("f float64", "foo(f)", "func foo(number float64) {}"),
      arguments("array []byte", "foo(array)", "func foo(a []byte) {}"),
      arguments("array [][]byte", "foo(array)", "func foo(a [][]byte) {}"),
      arguments("text string, i ...int", "foo(text, i)", "func foo(t string, n ...int) {}"),
      // function is undefined
      arguments("text string, i ...int", "foo(text, i)", ""));
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifyMethodSignatureForCustomFunctions(String arguments, String body, String funcDefinition) {
    var code = """
      package main
      func bar(%s) {
        %s
      }
      %s
      """.formatted(arguments, body, funcDefinition);
    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .findFirst()
      .get();
    assertThat(func.signature("main")).isEqualTo("main.foo");
  }

  @Test
  void shouldVerifyAliasImport() {
    var code = """
      package main

      import my_md5 "crypto/md5"

      func main(array []byte) {
        my_md5.Sum(array)
      }
      """;
    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .findFirst()
      .get();
    assertThat(func.signature("main")).isEqualTo("crypto/md5.Sum");
  }

  @Test
  void shouldVerifyWildcardImport() {
    var code = """
      package main

      import . "crypto/md5"

      func main(array []byte) {
        Sum(array)
      }
      """;
    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .findFirst()
      .get();
    assertThat(func.signature("main")).isEqualTo("crypto/md5.Sum");
  }

  static Stream<Arguments> shouldVerifySignatureMethodReceiver() {
    return of(
      arguments("""
        func (ctrl *MainController) foo() {
          ctrl.Ctx.SetCookie("name1", "value1", 200, "/", "example.com", false, false)
        }""", "*github.com/beego/beego/v2/server/web/context.Context.SetCookie"),
      arguments("""
        func (ctrl *MainController) foo() {
          ctrl.Ctx.SetCookie("name1", "value1")
        }""",
        "*github.com/beego/beego/v2/server/web/context.Context.SetCookie"),
      arguments("""
        func (ctrl MainController) foo() {
          ctrl.Ctx.SetCookie("name1", "value1")
        }""",
        "*github.com/beego/beego/v2/server/web/context.Context.SetCookie"),
      arguments("""
        func (ctrl MainController) foo() {
          ctrl.Ctx.SetCookie("name1", "value1",  200, "/")
        }""",
        "*github.com/beego/beego/v2/server/web/context.Context.SetCookie"),
      arguments("""
        func (s *Server) sqlSensitive() {
          s.db.QueryRow("SELECT email FROM contacts WHERE contact_name='" + r.URL.Query().Get("name") + "'")
        }""",
        "*database/sql.DB.QueryRow"),
      arguments("""
        func (ctrl *MainController) foo() {
          ctrl.method1()
        }""",
        "*main.MainController.method1"),
      // TODO SONARGO-497 Fix method signature for invocation in unusual way
      arguments("""
        func (ctrl *MainController) foo() {
          (*MainController).method1(ctrl)
        }""",
        "method1"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifySignatureMethodReceiver(String function, String expectedSignature) {
    var code = """
      package main
      import (
        "database/sql"
        "github.com/beego/beego/v2/server/web"
      )

      %s

      type MainController struct {
        web.Controller
      }
      type Server struct {
      	db *sql.DB
      }
      func (ctrl *MainController) method1() {
      }
      """.formatted(function);
    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .findFirst()
      .get();
    assertThat(func.signature("main")).isEqualTo(expectedSignature);
  }

  @Test
  void shouldVerifyAnonymousSignatureMethodReceiver() {
    var code = """
      package main
      import (
        "database/sql"
        "github.com/beego/beego/v2/server/web"
        "fmt"
      )

      func (ctrl *MainController) foo() {
        func (ctrl *MainController)(a int) {
          fmt.Println(a)
        }(42)
      }

      type MainController struct {
        web.Controller
      }
      """;
    var topLevelTree = (TopLevelTree) TestGoConverter.parse(code);
    var func = topLevelTree.descendants()
      .filter(FunctionInvocationTreeImpl.class::isInstance)
      .map(FunctionInvocationTreeImpl.class::cast)
      .toList();
    assertThat(func.get(0).signature("main")).isEqualTo("main.$anonymous_at_line_9");
  }
}
