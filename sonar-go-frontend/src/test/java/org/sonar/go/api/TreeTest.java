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
package org.sonar.go.api;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.api.BinaryExpressionTree.Operator.EQUAL_TO;
import static org.sonar.go.utils.TreeCreationUtils.assignment;
import static org.sonar.go.utils.TreeCreationUtils.binary;
import static org.sonar.go.utils.TreeCreationUtils.block;
import static org.sonar.go.utils.TreeCreationUtils.identifier;
import static org.sonar.go.utils.TreeCreationUtils.integerLiteral;
import static org.sonar.go.utils.TreeCreationUtils.simpleFunction;
import static org.sonar.go.utils.TreeCreationUtils.simpleNative;
import static org.sonar.go.utils.TreeCreationUtils.topLevel;

class TreeTest {

  private static final NativeKind SIMPLE_KIND = new NativeKind() {
  };

  @Test
  void test() {
    Tree x = identifier("x");
    Tree y = identifier("y");
    Tree z = identifier("z");
    Tree int1 = integerLiteral("1");
    Tree xEqualTo1 = binary(EQUAL_TO, x, int1);
    Tree yEqualsXEqualTo1 = assignment(y, xEqualTo1);
    IdentifierTree functionName = identifier("x");
    BlockTree functionBody = block(Arrays.asList(xEqualTo1, yEqualsXEqualTo1));
    Tree function = simpleFunction(functionName, functionBody);
    Tree nativeTree = simpleNative(SIMPLE_KIND, Collections.singletonList(z));
    Tree topLevelTree = topLevel(Arrays.asList(function, nativeTree));

    assertThat(topLevelTree.descendants())
      .containsExactly(function, functionName, functionBody, xEqualTo1, x, int1, yEqualsXEqualTo1, y, xEqualTo1, x, int1, nativeTree, z);
    assertThat(function.descendants())
      .containsExactly(functionName, functionBody, xEqualTo1, x, int1, yEqualsXEqualTo1, y, xEqualTo1, x, int1);
    assertThat(yEqualsXEqualTo1.descendants())
      .containsExactly(y, xEqualTo1, x, int1);
  }

}
