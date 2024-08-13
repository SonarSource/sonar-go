/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import org.junit.jupiter.api.Test;

class TriggersProcessRecordsInBulkCheckTest {

  @Test
  void test() {
    Verifier.verify("TriggersProcessRecordsInBulk_trigger1.cls", new TriggersProcessRecordsInBulkCheck());
    Verifier.verifyNoIssue("TriggersProcessRecordsInBulk_trigger2.cls", new TriggersProcessRecordsInBulkCheck());
    Verifier.verify("TriggersProcessRecordsInBulk_handlers.cls", new TriggersProcessRecordsInBulkCheck());
  }

}
