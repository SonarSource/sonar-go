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
package org.sonar.go.plugin;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.go.externalreport.AbstractReportSensor;
import org.sonar.go.externalreport.GoLintReportSensor;
import org.sonar.go.externalreport.GoVetReportSensor;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.plugin.RulesDefinitionUtils;

public class GoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "go";

  private final SonarRuntime runtime;

  public GoRulesDefinition(SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, GoLanguage.KEY)
      .setName("SonarAnalyzer");
    RuleMetadataLoader metadataLoader = new RuleMetadataLoader(GoPlugin.RESOURCE_FOLDER, GoProfileDefinition.PATH_TO_JSON, runtime);

    List<Class<?>> checks = GoCheckList.checks();
    metadataLoader.addRulesByAnnotatedClass(repository, checks);

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, checks, Language.GO);

    repository.done();

    AbstractReportSensor.createExternalRuleRepository(context, GoVetReportSensor.LINTER_ID, GoVetReportSensor.LINTER_NAME);
    AbstractReportSensor.createExternalRuleRepository(context, GoLintReportSensor.LINTER_ID, GoLintReportSensor.LINTER_NAME);
  }
}
