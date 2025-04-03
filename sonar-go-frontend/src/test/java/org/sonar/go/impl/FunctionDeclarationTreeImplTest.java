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
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.cfg.ControlFlowGraph;
import org.sonar.go.impl.cfg.BlockImpl;
import org.sonar.go.impl.cfg.ControlFlowGraphImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.go.utils.TreeCreationUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.impl.TextRanges.range;
import static org.sonar.go.utils.TreeCreationUtils.simpleNative;

class FunctionDeclarationTreeImplTest {
  private static final NativeKind SIMPLE_KIND = new NativeKind() {
  };
  private static final NativeKind METHOD_RECEIVER = new StringNativeKind("Names([]*Ident)");

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree returnType = TreeCreationUtils.identifier("int");
    Tree receiver = simpleNative(METHOD_RECEIVER, List.of(TreeCreationUtils.identifier("r")));
    Tree receiverWrapper = simpleNative(SIMPLE_KIND, List.of(receiver));
    IdentifierTree name = TreeCreationUtils.identifier("foo");
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
    assertThat(tree.cfg()).isSameAs(cfg);

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

}
