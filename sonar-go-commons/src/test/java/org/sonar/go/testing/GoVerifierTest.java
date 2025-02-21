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
package org.sonar.go.testing;

import org.junit.jupiter.api.Test;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.checks.GoCheck;

class GoVerifierTest {

  private static final GoCheck RAISE_ISSUE_ON_SYMBOL = init -> init.register(IdentifierTree.class, (ctx, identifierTree) -> {
    if (identifierTree.symbol() != null) {
      ctx.reportIssue(identifierTree, "Symbol found!");
    }
  });

  @Test
  void identifierShouldHaveSymbols() {
    GoVerifier.verify("GoVerifierTest/symbol_check.go", RAISE_ISSUE_ON_SYMBOL);
  }
}
