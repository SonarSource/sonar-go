/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

class FileHeaderCheckTest {
  private final FileHeaderCheck check = new FileHeaderCheck();

  @Test
  void shouldCheckSingleLineHeader() {
    check.headerFormat = "// copyright 2018";
    GoVerifier.verify("FileHeaderCheck/noncompliant.go", check);
    GoVerifier.verifyNoIssue("FileHeaderCheck/compliant.go", check);
  }

  @Test
  void shouldCheckRegexSingleLineHeader() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    GoVerifier.verify("FileHeaderCheck/noncompliant.go", check);
    GoVerifier.verifyNoIssue("FileHeaderCheck/compliant.go", check);
  }

  @Test
  void shouldCheckMultilineHeader() {
    check.headerFormat = """
      /*
       * SonarSource SLang
       * Copyright (C) 1999-2001 SonarSource Sàrl
       * mailto:info AT sonarsource DOT com
       */""";
    GoVerifier.verifyNoIssue("FileHeaderCheck/multiline.go", check);
  }

  @Test
  void shouldNotAllowHeaderNotOnFirstLine() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    GoVerifier.verify("FileHeaderCheck/no_first_line.go", check);
  }
}
