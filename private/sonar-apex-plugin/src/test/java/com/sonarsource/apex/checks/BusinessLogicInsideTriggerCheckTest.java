/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import org.junit.jupiter.api.Test;

class BusinessLogicInsideTriggerCheckTest {
  @Test
  void test() {
    BusinessLogicInsideTriggerCheck check = new BusinessLogicInsideTriggerCheck();
    Verifier.verifyNoIssue("BusinessLogicInsideTrigger-simple.cls", check);
    Verifier.verify("BusinessLogicInsideTrigger-complex1.cls", check);
    Verifier.verify("BusinessLogicInsideTrigger-complex2.cls", check);
    Verifier.verify("BusinessLogicInsideTrigger-complex3.cls", check);
    Verifier.verify("BusinessLogicInsideTrigger-complex4.cls", check);
  }
}
