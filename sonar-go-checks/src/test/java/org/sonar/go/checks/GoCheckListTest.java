/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.io.File;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoCheckListTest {

  /**
   * Enforces that each check file is declared in the list.
   */
  @Test
  void shouldContainEveryCheck() {
    File classDir = new File("src/main/java/org/sonar/go/checks/");
    File[] checkFiles = classDir.listFiles((dir, name) -> name.endsWith("Check.java") && !name.startsWith("Abstract"));
    assertThat(GoCheckList.checks()).hasSize(checkFiles != null ? checkFiles.length : 0);
  }

  /**
   * Enforces that each check has a test.
   */
  @Test
  void shouldHaveTestsForEachCheck() {
    for (Class<?> cls : GoCheckList.checks()) {
      if (cls.equals(ParsingErrorCheck.class)) {
        // We can't write any test for this check as it's empty
        continue;
      }
      String testName = '/' + cls.getName().replace('.', '/') + "Test.class";
      assertThat(getClass().getResource(testName))
        .overridingErrorMessage("No test for " + cls.getSimpleName())
        .isNotNull();
    }
  }
}
