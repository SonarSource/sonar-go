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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rules.CleanCodeAttribute.TRUSTWORTHY;

class GolangCILintRulesDefinitionTest {

  private static final SonarRuntime SONARQUBE_10_6_CCT_SUPPORT_MINIMAL_VERSION = SonarRuntimeImpl.forSonarQube(Version.create(10, 6), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);

  @Test
  void shouldDefineRulesWithCorrectAttributeAndImpact() {
    var context = new RulesDefinition.Context();
    var rulesDefinition = new GolangCILintRulesDefinition(SONARQUBE_10_6_CCT_SUPPORT_MINIMAL_VERSION);
    rulesDefinition.define(context);
    var repository = context.repository("external_" + GolangCILintReportSensor.LINTER_ID);

    var gosecRule = repository.rule("gosec");
    assertThat(gosecRule.name()).isEqualTo("Issue raised by gosec"); // Placeholder, overridden at issue-level
    assertThat(gosecRule.cleanCodeAttribute()).isEqualTo(TRUSTWORTHY);
    assertThat(gosecRule.defaultImpacts()).containsOnly(Map.entry(SoftwareQuality.SECURITY, Severity.HIGH));
  }
}
