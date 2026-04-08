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

class BadFunctionNameCheckTest {

  @Test
  void test() {
    GoVerifier.verify("BadFunctionNameCheck/bad_function_name.go", new BadFunctionNameCheck());
  }

  @Test
  void testUpperCase() {
    BadFunctionNameCheck check = new BadFunctionNameCheck();
    check.format = "^[A-Z]*$";
    GoVerifier.verify("BadFunctionNameCheck/bad_function_name_uppercase.go", check);
  }

  @Test
  void testFileAllowsUnderscoresForTestFile() {
    GoVerifier.verifyAsTestFile("BadFunctionNameCheck/bad_function_name_test_file.go", new BadFunctionNameCheck());
  }

  @Test
  void testFileWithMainFormatReportsIssuesForTestFile() {
    BadFunctionNameCheck check = new BadFunctionNameCheck();
    check.formatForTests = GoChecksConstants.GO_NAMING_DEFAULT;
    GoVerifier.verifyAsTestFile("BadFunctionNameCheck/bad_function_name.go", check);
  }
}
