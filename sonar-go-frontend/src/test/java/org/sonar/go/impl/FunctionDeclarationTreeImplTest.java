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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.go.impl.cfg.BlockImpl;
import org.sonar.go.impl.cfg.ControlFlowGraphImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.testing.TestGoConverter;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.cfg.ControlFlowGraph;

import static java.util.Collections.emptyList;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.sonar.go.impl.TextRanges.range;
import static org.sonar.go.symbols.GoNativeType.UNKNOWN;
import static org.sonar.go.utils.TreeCreationUtils.simpleNative;

class FunctionDeclarationTreeImplTest {
  private static final NativeKind SIMPLE_KIND = new NativeKind() {
  };
  private static final NativeKind METHOD_RECEIVER = new StringNativeKind("Names([]*Ident)");

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree returnType = TreeCreationUtils.identifier("int");
    Tree receiver = simpleNative(METHOD_RECEIVER, List.of(TreeCreationUtils.identifier("r", "*main.MyReceiverType", "receiverPackage")));
    Tree receiverWrapper = simpleNative(SIMPLE_KIND, List.of(receiver));
    IdentifierTree name = TreeCreationUtils.identifier("foo", UNKNOWN, "fooPackage");
    IdentifierTree paramName = TreeCreationUtils.identifier("p1");
    ParameterTree param = new ParameterTreeImpl(meta, paramName, null);
    List<Tree> params = List.of(param);
    Tree typeParameters = simpleNative(SIMPLE_KIND, List.of(TreeCreationUtils.identifier("T")));
    BlockTree body = new BlockTreeImpl(meta, emptyList());
    ControlFlowGraph cfg = new ControlFlowGraphImpl(List.of(new BlockImpl(List.of())));

    FunctionDeclarationTreeImpl tree = new FunctionDeclarationTreeImpl(meta, returnType, receiverWrapper, name, params, typeParameters, body, cfg);
    assertThat(tree.children()).containsExactly(returnType, receiverWrapper, name, param, typeParameters, body);
    assertThat(tree.returnType()).isEqualTo(returnType);
    assertThat(tree.name()).isEqualTo(name);
    assertThat(tree.formalParameters()).isEqualTo(params);
    assertThat(tree.body()).isEqualTo(body);
    assertThat(tree.receiver()).isSameAs(receiverWrapper);
    assertThat(tree.receiverName()).isEqualTo("r");
    // second call of receiverName for coverage (lazy calculation)
    assertThat(tree.receiverName()).isEqualTo("r");
    assertThat(tree.receiverType()).isEqualTo("*main.MyReceiverType");
    // second call of receiverType for coverage (lazy calculation)
    assertThat(tree.receiverType()).isEqualTo("*main.MyReceiverType");
    assertThat(tree.cfg()).isSameAs(cfg);
    assertThat(tree.signature()).isEqualTo("*main.MyReceiverType.foo");

    FunctionDeclarationTreeImpl lightweightConstructor = new FunctionDeclarationTreeImpl(meta, null, null, null, emptyList(), null, null, null);

    assertThat(lightweightConstructor.children()).isEmpty();
  }

  @Test
  void rangeToHighlight_with_name() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), emptyList());
    TreeMetaData nameMetaData = metaDataProvider.metaData(range(1, 2, 3, 4));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 5, 7));
    IdentifierTree name = TreeCreationUtils.identifier(nameMetaData, "foo");
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), null, null, name, emptyList(), null, body, null).rangeToHighlight())
      .isEqualTo(nameMetaData.textRange());
  }

  @Test
  void rangeToHighlight_with_body_only() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 17, 18), "{", Token.Type.OTHER),
      new TokenImpl(range(5, 1, 19, 20), "}", Token.Type.OTHER)));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 17, 20));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), null, null, null, emptyList(), null, body, null).rangeToHighlight())
      .isEqualTo(body.metaData().textRange());
  }

  @Test
  void rangeToHighlight_with_no_name_but_some_signature() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 5, 10), "fun", Token.Type.KEYWORD),
      new TokenImpl(range(5, 11, 5, 15), "foo", Token.Type.OTHER),
      new TokenImpl(range(5, 17, 5, 18), "{", Token.Type.OTHER),
      new TokenImpl(range(5, 19, 5, 20), "}", Token.Type.OTHER)));
    TreeMetaData functionMetaData = metaDataProvider.metaData(range(5, 1, 5, 20));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 17, 5, 20));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, null, null, null, emptyList(), null, body, null).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 15));
  }

  @Test
  void rangeToHighlight_with_no_name_no_body_but_some_signature() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 5, 10), "fun", Token.Type.KEYWORD),
      new TokenImpl(range(5, 11, 5, 12), "(", Token.Type.OTHER),
      new TokenImpl(range(5, 13, 5, 14), ")", Token.Type.OTHER)));
    TreeMetaData functionMetaData = metaDataProvider.metaData(range(5, 1, 5, 14));
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, null, null, null, emptyList(), null, null, null).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 14));
  }

  static Stream<Arguments> shouldVerifyFunctionDeclarationSignature() {
    return of(
      arguments("func foo() {}"),
      arguments("func foo(a int) {}"),
      arguments("func foo(a float32) {}"),
      arguments("func foo(a float64) {}"),
      arguments("func foo(text string) {}"),
      arguments("func foo(a ...int) {}"),
      arguments("func foo(a ...float64) {}"),
      arguments("func foo(text ...string) {}"),
      arguments("func foo(args ...interface{}) {}"),
      arguments("func foo(a []int) {}"),
      arguments("func foo(a [][]int) {}"),
      arguments("func foo(a int16) {}"),
      arguments("func foo(a int32) {}"),

      arguments("func foo(a int, b string) {}"),
      arguments("func foo(a int, b ...string) {}"),
      arguments("func foo(a int, b []string) {}"),
      arguments("func foo(a int, b any) {}"),

      arguments("func foo(c *fiber.Ctx) {}"),
      arguments("func foo(c fiber.Ctx) {}"),
      arguments("func foo(cookie fiber.Cookie, ctx *fiber.Ctx) {}"),

      arguments("func foo[P any]() {}"),
      arguments("func foo[S interface{ ~[]byte|string }]() {}"),
      arguments("func foo[S ~[]E, E any]() {}"),
      // M type is undefined
      arguments("func foo[P M[int]]() {}"),
      arguments("func foo[_ any]() {}"),
      arguments("func foo(m map[string]int64) int64 {\nreturn 0\n}"),
      arguments("func foo(m map[string]float64) float64 {\nreturn 0\n}"),
      arguments("func foo[K comparable, V int64 | float64](m map[K]V) V {\nreturn 0\n}"),
      arguments("func foo[K comparable, V Number](m map[K]V) V {\nreturn 0\n}"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifyFunctionDeclarationSignature(String function) {
    var code = """
      package main
      import (
        "github.com/gofiber/fiber/v2"
        "github.com/beego/beego/v2/server/web"
      )
      %s
      """.formatted(function);
    var func = TestGoConverter.parseAndRetrieve(FunctionDeclarationTreeImpl.class, code);
    assertThat(func.signature()).isEqualTo("main.foo");
  }

  @Test
  void shouldVerifyAnonymousFunctionSignature() {
    var code = """
      package main

      import (
          "database/sql"
          "flag"
          "fmt"
          "github.com/go-sql-driver/mysql"
          "log"
          "os"
      )

      func a () {
          func (a int) {
              fmt.Println(a)
          }(5)
      }""";
    var tree = (TopLevelTree) TestGoConverter.parse(code);
    var funcList = tree.descendants()
      .filter(FunctionDeclarationTreeImpl.class::isInstance)
      .map(FunctionDeclarationTreeImpl.class::cast)
      .toList();
    assertThat(funcList.get(1).signature()).isEqualTo("$anonymous_at_line_13");
  }

  static Stream<Arguments> shouldVerifyMethodReceiverSignature() {
    return of(
      arguments("func (ctrl *MainController) foo() {}", "*main.MainController.foo"),
      arguments("func (ctrl MainController) foo() {}", "main.MainController.foo"),
      arguments("func (ctrl MainController) foo(a int) {}", "main.MainController.foo"),
      arguments("func (ctrl MainController) foo(a int, b ...float) {}", "main.MainController.foo"),
      arguments("func (r *MainController) foo(a int, b ...float) {}", "*main.MainController.foo"),
      arguments("func (r MainController) foo(a int, b ...float) {}", "main.MainController.foo"),
      arguments("func (s *Server) sqlSensitive(w http.ResponseWriter, r *http.Request) {}", "*main.Server.sqlSensitive"),
      arguments("func (s Server) sqlSensitive(w http.ResponseWriter, r *http.Request) {}", "main.Server.sqlSensitive"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldVerifyMethodReceiverSignature(String function, String expectedSignature) {
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
      """.formatted(function);
    var func = TestGoConverter.parseAndRetrieve(FunctionDeclarationTreeImpl.class, code);
    assertThat(func.signature()).isEqualTo(expectedSignature);
  }
}
