/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.externalreport;

import org.junit.jupiter.api.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class PmdRulesDefinitionTest {

  @Test
  void pmd_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    PmdRulesDefinition rulesDefinition = new PmdRulesDefinition();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_pmd_apex");
    assertThat(repository.name()).isEqualTo("PMD");
    assertThat(repository.language()).isEqualTo("apex");
    assertThat(repository.isExternal()).isTrue();
    assertThat(repository.rules()).hasSize(59);

    RulesDefinition.Rule rule = repository.rule("ApexDangerousMethods");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Apex dangerous methods");
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo(""
      + "See description of PMD rule <code>ApexDangerousMethods</code> at the "
      + "<a href=\"https://pmd.github.io/pmd-6.55.0/pmd_rules_apex_security.html#apexdangerousmethods\">PMD website</a>.");
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }
}
