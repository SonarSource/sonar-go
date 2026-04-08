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
package org.sonar.go.testing;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.checks.GoCheck;

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
