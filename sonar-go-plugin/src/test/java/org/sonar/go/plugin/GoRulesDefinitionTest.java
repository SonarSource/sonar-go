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
package org.sonar.go.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.go.checks.GoCheckList;

import static org.assertj.core.api.Assertions.assertThat;

class GoRulesDefinitionTest {

  private static final SonarRuntime RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);

  @Test
  void test() {
    GoRulesDefinition rulesDefinition = new GoRulesDefinition(RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);

    RulesDefinition.Repository goRepository = context.repository("go");

    assertThat(goRepository.name()).isEqualTo("Sonar");
    assertThat(goRepository.language()).isEqualTo("go");
    assertThat(goRepository.rules()).hasSize(GoCheckList.allChecks().size());

    RulesDefinition.Rule rule = goRepository.rule("S4663");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Multi-line comments should not be empty");
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.activatedByDefault()).isTrue();
  }
}
