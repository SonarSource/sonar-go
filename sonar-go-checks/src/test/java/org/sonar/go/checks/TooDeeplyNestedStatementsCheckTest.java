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

class TooDeeplyNestedStatementsCheckTest {

  @Test
  void test() {
    GoVerifier.verify("TooDeeplyNestedStatementsCheck/too_deeply_nested_statements.go", new TooDeeplyNestedStatementsCheck());
  }

  @Test
  void test_max_2() {
    TooDeeplyNestedStatementsCheck check = new TooDeeplyNestedStatementsCheck();
    check.max = 2;
    GoVerifier.verify("TooDeeplyNestedStatementsCheck/too_deeply_nested_statements_max_2.go", check);
  }

}
