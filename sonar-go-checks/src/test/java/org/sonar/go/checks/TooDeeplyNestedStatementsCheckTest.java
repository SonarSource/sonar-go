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
