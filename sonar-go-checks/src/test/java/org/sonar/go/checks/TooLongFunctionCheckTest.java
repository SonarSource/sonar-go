/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.go.testing.GoVerifier;

class TooLongFunctionCheckTest {

  TooLongFunctionCheck check = new TooLongFunctionCheck();

  @Test
  void shouldVerifyFunctionMax3() {
    check.max = 3;
    GoVerifier.verify("TooLongFunctionCheck/too_long_function_3.go", check);
  }

  @Test
  void shouldVerifyFunctionMax4() {
    check.max = 4;
    GoVerifier.verify("TooLongFunctionCheck/too_long_function_4.go", check);
  }

}
