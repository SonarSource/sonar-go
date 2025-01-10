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
package org.sonar.go.checks;

import org.junit.jupiter.api.Test;

class TooComplexExpressionCheckTest {

  @Test
  void test_max_3() {
    GoVerifier.verify("TooComplexExpressionCheck/too_complex_expression_3.go", new TooComplexExpressionCheck());
  }

  @Test
  void test_max_2() {
    var check = new TooComplexExpressionCheck();
    check.max = 2;
    GoVerifier.verify("TooComplexExpressionCheck/too_complex_expression_2.go", check);
  }

}
