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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.MatchCaseTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.api.Token.*;
import static org.sonar.go.impl.TextRanges.range;

class MatchCaseTreeImplTest {

  private static final List<Token> TOKENS = Arrays.asList(
    new TokenImpl(range(1, 1, 1, 5), "when", Type.OTHER),
    new TokenImpl(range(1, 7, 1, 8), "1", Type.OTHER),
    new TokenImpl(range(1, 9, 1, 10), "a", Type.OTHER));

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree expression = new LiteralTreeImpl(meta, "42");
    Tree body = new IdentifierTreeImpl(meta, "x");
    MatchCaseTree tree = new MatchCaseTreeImpl(meta, expression, body);
    assertThat(tree.children()).containsExactly(expression, body);
    assertThat(tree.expression()).isEqualTo(expression);
    assertThat(tree.body()).isEqualTo(body);

    assertThat(new MatchCaseTreeImpl(meta, null, body).children()).containsExactly(body);
    assertThat(new MatchCaseTreeImpl(meta, expression, null).children()).containsExactly(expression);
  }

  @Test
  void rangeToHighlight() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), TOKENS);
    TreeMetaData matchCaseMetaData = metaDataProvider.metaData(range(1, 1, 1, 10));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(1, 9, 1, 10));
    BlockTree body = new BlockTreeImpl(bodyMetaData, Collections.singletonList(new IdentifierTreeImpl(bodyMetaData, "a")));

    assertThat(new MatchCaseTreeImpl(matchCaseMetaData, null, body).rangeToHighlight())
      .isEqualTo(range(1, 1, 1, 8));
  }

  @Test
  void rangeToHighlight_with_empty_body() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), TOKENS);
    TreeMetaData matchCaseMetaData = metaDataProvider.metaData(range(1, 1, 1, 8));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(1, 1, 1, 8));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());

    assertThat(new MatchCaseTreeImpl(matchCaseMetaData, null, body).rangeToHighlight())
      .isEqualTo(range(1, 1, 1, 8));

    assertThat(new MatchCaseTreeImpl(matchCaseMetaData, null, null).rangeToHighlight())
      .isEqualTo(range(1, 1, 1, 8));
  }

}
