/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

class ApexRulesDefinitionTest {

  @Test
  void rules() {
    RulesDefinition.Repository repository = getRepositoryForVersion(Version.create(9, 3));

    assertThat(repository.name()).isEqualTo("Sonar");
    assertThat(repository.language()).isEqualTo("apex");
    RulesDefinition.Rule rule = repository.rule("NotExistingRule");
    assertThat(rule).isNull();
  }

  @Test
  void owasp_security_standard_includes_2021() {
    RulesDefinition.Repository repository = getRepositoryForVersion(Version.create(9, 3));

    RulesDefinition.Rule rule = repository.rule("S1313");
    assertThat(rule).isNotNull();
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("owaspTop10:a3", "owaspTop10-2021:a1");
  }

  @Test
  void owasp_security_standard() {
    RulesDefinition.Repository repository = getRepositoryForVersion(Version.create(8, 9));

    RulesDefinition.Rule rule = repository.rule("S1313");
    assertThat(rule).isNotNull();
    assertThat(rule.securityStandards()).containsExactly("owaspTop10:a3");
  }

  private RulesDefinition.Repository getRepositoryForVersion(Version version) {
    RulesDefinition rulesDefinition = new ApexRulesDefinition(
      SonarRuntimeImpl.forSonarQube(version, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    return context.repository("apex");
  }

}
