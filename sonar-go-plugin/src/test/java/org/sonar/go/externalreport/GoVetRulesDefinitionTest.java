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
import static org.sonar.api.rules.CleanCodeAttribute.LOGICAL;

class GoVetRulesDefinitionTest {

  private static final SonarRuntime SONARQUBE_10_6_CCT_SUPPORT_MINIMAL_VERSION = SonarRuntimeImpl.forSonarQube(Version.create(10, 6), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
  private static final SonarRuntime SONARLINT_RUNTIME_9_9 = SonarRuntimeImpl.forSonarLint(Version.create(9, 2));

  @Test
  void shouldDefineRulesWithCorrectAttributeAndImpact() {
    var context = new RulesDefinition.Context();
    var rulesDefinition = new GoVetRulesDefinition(SONARQUBE_10_6_CCT_SUPPORT_MINIMAL_VERSION);
    rulesDefinition.define(context);
    var repository = context.repository("external_" + GoVetReportSensor.LINTER_ID);

    var codeSmell = repository.rule("unsafeptr");
    assertThat(codeSmell.name()).isEqualTo("Invalid conversions of uintptr to unsafe.Pointer");
    assertThat(codeSmell.cleanCodeAttribute()).isEqualTo(LOGICAL);
    assertThat(codeSmell.defaultImpacts()).containsOnly(Map.entry(SoftwareQuality.RELIABILITY, Severity.MEDIUM));
  }

  @Test
  void shouldNotDefineRulesWithSonarLintRuntime() {
    var context = new RulesDefinition.Context();
    var rulesDefinition = new GoVetRulesDefinition(SONARLINT_RUNTIME_9_9);
    rulesDefinition.define(context);
    assertThat(context.repositories()).isEmpty();
  }

}
