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

import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public abstract class AbstractExternalRulesDefinition implements RulesDefinition {
  public static final String RULES_JSON_PATH_TEMPLATE = "org/sonar/l10n/go/rules/%s/rules.json";

  @Nullable
  private final ExternalRuleLoader ruleLoader;

  protected AbstractExternalRulesDefinition(SonarRuntime sonarRuntime, String linterId, String linterName) {
    if (sonarRuntime.getProduct() != SonarProduct.SONARLINT) {
      this.ruleLoader = new ExternalRuleLoader(
        linterId,
        linterName,
        RULES_JSON_PATH_TEMPLATE.formatted(linterId),
        GoLanguage.KEY,
        sonarRuntime);
    } else {
      this.ruleLoader = null;
    }
  }

  @Override
  public void define(Context context) {
    if (ruleLoader != null) {
      ruleLoader.createExternalRuleRepository(context);
    }
  }
}
