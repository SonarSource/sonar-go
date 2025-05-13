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

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public abstract class AbstractExternalRulesDefinition implements RulesDefinition {

  public static final String RULES_JSON_PATH_TEMPLATE = "org/sonar/l10n/go/rules/%s/rules.json";

  private final ExternalRuleLoader ruleLoader;

  protected AbstractExternalRulesDefinition(SonarRuntime sonarRuntime, String linterId, String linterName) {
    this.ruleLoader = new ExternalRuleLoader(
      linterId,
      linterName,
      RULES_JSON_PATH_TEMPLATE.formatted(linterId),
      GoLanguage.KEY,
      sonarRuntime);
  }

  @Override
  public void define(Context context) {
    ruleLoader.createExternalRuleRepository(context);
  }
}
