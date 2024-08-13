/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import org.junit.jupiter.api.Test;

class TestFunctionsContainSystemRunAsCheckTest {
  @Test
  void test() {
    Verifier.verify("TestFunctionsContainSystemRunAs.cls", new TestFunctionsContainSystemRunAsCheck());
  }
}
