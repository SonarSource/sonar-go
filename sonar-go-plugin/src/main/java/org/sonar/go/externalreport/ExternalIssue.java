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
package org.sonar.go.externalreport;

import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;

import static org.sonar.go.externalreport.AbstractReportSensor.GENERIC_ISSUE_KEY;

class ExternalIssue {

  final String linter;
  final RuleType type;
  final String ruleKey;
  final String filename;
  final int lineNumber;
  final String message;

  ExternalIssue(String linter, RuleType type, @Nullable String ruleKey, String filename, int lineNumber, String message) {
    this.linter = linter;
    this.type = type;
    this.ruleKey = mapRuleKey(linter, ruleKey, message);
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.message = message;
  }

  private static String mapRuleKey(String linter, @Nullable String ruleKey, String message) {
    if (ruleKey != null) {
      return ruleKey;
    }
    String key = ExternalKeyUtils.lookup(message, linter);
    return key != null ? key : GENERIC_ISSUE_KEY;
  }

}
