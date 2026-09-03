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

class PredeclaredIdentifierShadowedCheckTest {

  private final PredeclaredIdentifierShadowedCheck check = new PredeclaredIdentifierShadowedCheck();

  @Test
  void shouldRaiseOnShadowingDeclarations() {
    GoVerifier.verify("PredeclaredIdentifierShadowedCheck/PredeclaredIdentifierShadowedCheck.go", check);
  }

  @Test
  void shouldRaiseOnPredeclaredTypesShadowedAtPackageScope() {
    GoVerifier.verify("PredeclaredIdentifierShadowedCheck/PredeclaredIdentifierShadowedCheckTypes.go", check);
  }

  @Test
  void shouldRaiseOnlyOnTheDeclaringUsageOfAName() {
    GoVerifier.verify("PredeclaredIdentifierShadowedCheck/PredeclaredIdentifierShadowedCheckRedeclaration.go", check);
  }

  @Test
  void shouldNotRaiseOnDeclarationsThatShadowNothing() {
    GoVerifier.verifyNoIssue("PredeclaredIdentifierShadowedCheck/PredeclaredIdentifierShadowedCheckCompliant.go", check);
  }
}
