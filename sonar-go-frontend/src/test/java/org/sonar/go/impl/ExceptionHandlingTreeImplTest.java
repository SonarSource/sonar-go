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
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlingTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    ParameterTree parameter = new ParameterTreeImpl(meta, TreeCreationUtils.identifier("e"), null);
    Tree lhs = TreeCreationUtils.identifier("x");
    Tree one = new LiteralTreeImpl(meta, "1");
    Tree assignmentExpressionTree = new AssignmentExpressionTreeImpl(meta, AssignmentExpressionTree.Operator.EQUAL, lhs, one);
    CatchTreeImpl catchWithIdentifier = new CatchTreeImpl(meta, parameter, assignmentExpressionTree, null);
    CatchTreeImpl catchWithoutIdentifier = new CatchTreeImpl(meta, null, assignmentExpressionTree, null);
    TokenImpl tryToken = new TokenImpl(new TextRangeImpl(1, 0, 1, 3), "try", Token.Type.KEYWORD);

    Tree emptyTry = new BlockTreeImpl(meta, Collections.emptyList());

    List<CatchTree> catchTreeList = Arrays.asList(catchWithIdentifier, catchWithoutIdentifier);

    Tree emptyFinally = new BlockTreeImpl(meta, Collections.emptyList());

    ExceptionHandlingTreeImpl exceptionHandlingTree = new ExceptionHandlingTreeImpl(null, emptyTry, tryToken, catchTreeList, emptyFinally);

    assertThat(exceptionHandlingTree.children()).containsExactly(emptyTry, catchWithIdentifier, catchWithoutIdentifier, emptyFinally);
    assertThat(exceptionHandlingTree.tryBlock()).isEqualTo(emptyTry);
    assertThat(exceptionHandlingTree.catchBlocks()).containsExactly(catchWithIdentifier, catchWithoutIdentifier);
    assertThat(exceptionHandlingTree.finallyBlock()).isEqualTo(emptyFinally);
  }

}
