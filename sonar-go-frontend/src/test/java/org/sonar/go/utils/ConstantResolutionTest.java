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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.persistence.conversion.StringNativeKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.api.BinaryExpressionTree.Operator.PLUS;
import static org.sonar.go.api.BinaryExpressionTree.Operator.TIMES;
import static org.sonar.go.utils.ConstantResolution.PLACEHOLDER;

class ConstantResolutionTest {
  private static final Tree HELLO = new StringLiteralTreeImpl(null, "\"Hello\"");
  private static final Tree WORLD = new StringLiteralTreeImpl(null, "\"World\"");

  @Test
  void simpleStringConstantResolution() {
    assertThat(ConstantResolution.resolveAsStringConstant(HELLO)).isEqualTo("Hello");
  }

  @Test
  void parenthesisConstantResolution() {
    Token leftParenthesis = new TokenImpl(new TextRangeImpl(1, 1, 1, 6), "(", Token.Type.OTHER);
    Token rightParenthesis = new TokenImpl(new TextRangeImpl(1, 1, 1, 6), ")", Token.Type.OTHER);
    Tree parenthesized = new ParenthesizedExpressionTreeImpl(null, HELLO, leftParenthesis, rightParenthesis);
    assertThat(ConstantResolution.resolveAsStringConstant(parenthesized)).isEqualTo("Hello");
  }

  @Test
  void binaryConstantResolution() {
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, WORLD);
    assertThat(ConstantResolution.resolveAsStringConstant(binary)).isEqualTo("HelloWorld");
  }

  @Test
  void binaryOtherOperatorConstantResolution() {
    BinaryExpressionTree binary = TreeCreationUtils.binary(TIMES, HELLO, WORLD);
    assertThat(ConstantResolution.resolveAsStringConstant(binary)).isEqualTo(PLACEHOLDER);
  }

  @Test
  void binaryNestedConstantResolution() {
    Tree comma = new StringLiteralTreeImpl(null, "\", \"");
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, comma);
    BinaryExpressionTree binaryNested = TreeCreationUtils.binary(PLUS, binary, WORLD);
    assertThat(ConstantResolution.resolveAsStringConstant(binaryNested)).isEqualTo("Hello, World");
  }

  @Test
  void unresolvedConstantResolution() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    assertThat(ConstantResolution.resolveAsStringConstant(tree)).isEqualTo(PLACEHOLDER);
  }

  @Test
  void binaryWithUnresolvedConstantConstantResolution() {
    NativeTree tree = TreeCreationUtils.simpleNative(new StringNativeKind("Kind"), Collections.emptyList());
    BinaryExpressionTree binary = TreeCreationUtils.binary(PLUS, HELLO, tree);
    assertThat(ConstantResolution.resolveAsStringConstant(binary)).isEqualTo("Hello" + PLACEHOLDER);
  }
}
