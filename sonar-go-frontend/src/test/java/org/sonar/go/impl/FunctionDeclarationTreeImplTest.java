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
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.impl.TextRanges.range;
import static org.sonar.go.utils.TreeCreationUtils.identifier;
import static org.sonar.go.utils.TreeCreationUtils.simpleNative;

class FunctionDeclarationTreeImplTest {
  private static final NativeKind SIMPLE_KIND = new NativeKind() {
  };

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree returnType = new IdentifierTreeImpl(meta, "int");
    Tree receiver = simpleNative(SIMPLE_KIND, singletonList(new IdentifierTreeImpl(meta, "r")));
    IdentifierTree name = new IdentifierTreeImpl(meta, "foo");
    IdentifierTree paramName = new IdentifierTreeImpl(meta, "p1");
    ParameterTree param = new ParameterTreeImpl(meta, paramName, null);
    List<Tree> params = singletonList(param);
    BlockTree body = new BlockTreeImpl(meta, emptyList());
    IdentifierTree child = identifier("GENERIC");
    NativeTree nativeChildren = simpleNative(SIMPLE_KIND, singletonList(child));

    FunctionDeclarationTreeImpl tree = new FunctionDeclarationTreeImpl(meta, returnType, receiver, name, params, body, singletonList(nativeChildren));
    assertThat(tree.children()).containsExactly(returnType, receiver, name, param, body, nativeChildren);
    assertThat(tree.returnType()).isEqualTo(returnType);
    assertThat(tree.name()).isEqualTo(name);
    assertThat(tree.formalParameters()).isEqualTo(params);
    assertThat(tree.body()).isEqualTo(body);
    assertThat(tree.nativeChildren()).isEqualTo(singletonList(nativeChildren));

    FunctionDeclarationTreeImpl lightweightConstructor = new FunctionDeclarationTreeImpl(meta, null, null, null, emptyList(), null, emptyList());

    assertThat(lightweightConstructor.children()).isEmpty();
    assertThat(lightweightConstructor.nativeChildren()).isEmpty();
  }

  @Test
  void rangeToHighlight_with_name() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), emptyList());
    TreeMetaData nameMetaData = metaDataProvider.metaData(range(1, 2, 3, 4));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 5, 7));
    IdentifierTree name = new IdentifierTreeImpl(nameMetaData, "foo");
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), null, null, name, emptyList(), body, emptyList()).rangeToHighlight())
      .isEqualTo(nameMetaData.textRange());
  }

  @Test
  void rangeToHighlight_with_body_only() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 17, 18), "{", Token.Type.OTHER),
      new TokenImpl(range(5, 1, 19, 20), "}", Token.Type.OTHER)));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 17, 20));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), null, null, null, emptyList(), body, emptyList()).rangeToHighlight())
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
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, null, null, null, emptyList(), body, emptyList()).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 15));
  }

  @Test
  void rangeToHighlight_with_no_name_no_body_but_some_signature() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 5, 10), "fun", Token.Type.KEYWORD),
      new TokenImpl(range(5, 11, 5, 12), "(", Token.Type.OTHER),
      new TokenImpl(range(5, 13, 5, 14), ")", Token.Type.OTHER)));
    TreeMetaData functionMetaData = metaDataProvider.metaData(range(5, 1, 5, 14));
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, null, null, null, emptyList(), null, emptyList()).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 14));
  }

}
