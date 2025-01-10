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

class TooManyLinesOfCodeFileCheckTest {

  private final TooManyLinesOfCodeFileCheck check = new TooManyLinesOfCodeFileCheck();

  @Test
  void shouldRaiseWithMax6Lines() {
    check.max = 6;
    GoVerifier.verify("TooManyLinesOfCodeFileCheck/TooManyLinesOfCodeFileCheck6.go", check);
  }

  @Test
  void shouldNotRaiseWithMax7Lines() {
    check.max = 7;
    GoVerifier.verifyNoIssue("TooManyLinesOfCodeFileCheck/TooManyLinesOfCodeFileCheck7.go", check);
  }

}
