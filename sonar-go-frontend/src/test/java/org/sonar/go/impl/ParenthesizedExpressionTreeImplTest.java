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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.ParenthesizedExpressionTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.impl.TextRanges.range;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;
import static org.sonar.go.utils.TreeCreationUtils.binary;
import static org.sonar.go.utils.TreeCreationUtils.identifier;
import static org.sonar.go.utils.TreeCreationUtils.integerLiteral;
import static org.sonar.go.utils.TreeCreationUtils.literal;

class ParenthesizedExpressionTreeImplTest {

  @Test
  void test() {
    Tree identifier = identifier("value");
    Tree literalInt2 = integerLiteral("2");
    Tree literalTrue = literal("true");
    Tree binary1 = binary(BinaryExpressionTree.Operator.GREATER_THAN, identifier, literalInt2);
    Token leftParenthesis1 = new TokenImpl(range(5, 1, 5, 2), "(", Token.Type.OTHER);
    Token rightParenthesis1 = new TokenImpl(range(5, 6, 5, 7), ")", Token.Type.OTHER);
    Token leftParenthesis2 = new TokenImpl(range(5, 0, 5, 1), "(", Token.Type.OTHER);
    Token rightParenthesis2 = new TokenImpl(range(5, 10, 5, 11), ")", Token.Type.OTHER);

    ParenthesizedExpressionTree parenthesisExpression1 = new ParenthesizedExpressionTreeImpl(null, binary1, leftParenthesis1, rightParenthesis1);
    Tree binary2 = binary(BinaryExpressionTree.Operator.EQUAL_TO, literalTrue, parenthesisExpression1);
    ParenthesizedExpressionTree parenthesisExpression2 = new ParenthesizedExpressionTreeImpl(null, binary2, leftParenthesis2, rightParenthesis2);

    assertThat(parenthesisExpression1.children()).hasSize(1);
    assertThat(parenthesisExpression2.children()).hasSize(1);
    assertThat(areEquivalent(parenthesisExpression1, parenthesisExpression1)).isTrue();
    assertThat(areEquivalent(parenthesisExpression1, new ParenthesizedExpressionTreeImpl(null, binary1, null, null))).isTrue();
    assertThat(areEquivalent(parenthesisExpression1, parenthesisExpression2)).isFalse();
    assertThat(parenthesisExpression1.expression()).isEqualTo(binary1);
    assertThat(parenthesisExpression2.expression()).isEqualTo(binary2);
  }

}
