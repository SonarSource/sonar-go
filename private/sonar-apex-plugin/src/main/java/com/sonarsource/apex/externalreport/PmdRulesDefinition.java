/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.externalreport;

import com.sonarsource.apex.plugin.ApexPlugin;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public class PmdRulesDefinition implements RulesDefinition {

  private static final String RULES_JSON = "org/sonar/l10n/apex/rules/pmd/rules.json";

  private static final String RULE_REPOSITORY_LANGUAGE = ApexPlugin.APEX_LANGUAGE_KEY;

  static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(PmdSensor.LINTER_KEY, PmdSensor.LINTER_NAME, RULES_JSON, RULE_REPOSITORY_LANGUAGE);

  @Override
  public void define(Context context) {
    RULE_LOADER.createExternalRuleRepository(context);
  }

  @Override
  public String toString() {
    return PmdSensor.LINTER_KEY + "-rules-definition";
  }
}
