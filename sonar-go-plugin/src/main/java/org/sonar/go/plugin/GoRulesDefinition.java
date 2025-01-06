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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.RuleProperty;
import org.sonar.go.checks.GoCheckList;
import org.sonar.go.checks.utils.PropertyDefaultValue;
import org.sonar.go.checks.utils.PropertyDefaultValues;
import org.sonar.go.externalreport.AbstractReportSensor;
import org.sonar.go.externalreport.GoLintReportSensor;
import org.sonar.go.externalreport.GoVetReportSensor;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

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

    setDefaultValuesForParameters(repository, checks);

    repository.done();

    AbstractReportSensor.createExternalRuleRepository(context, GoVetReportSensor.LINTER_ID, GoVetReportSensor.LINTER_NAME);
    AbstractReportSensor.createExternalRuleRepository(context, GoLintReportSensor.LINTER_ID, GoLintReportSensor.LINTER_NAME);
  }

  private static void setDefaultValuesForParameters(RulesDefinition.NewRepository repository, List<Class<?>> checks) {
    for (Class<?> check : checks) {
      org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(check, org.sonar.check.Rule.class);
      String ruleKey = ruleAnnotation.key();
      for (Field field : check.getDeclaredFields()) {
        RuleProperty ruleProperty = field.getAnnotation(RuleProperty.class);
        PropertyDefaultValues defaultValues = field.getAnnotation(PropertyDefaultValues.class);
        if (ruleProperty == null || defaultValues == null) {
          continue;
        }
        String paramKey = ruleProperty.key();

        List<PropertyDefaultValue> valueForLanguage = Arrays.stream(defaultValues.value())
          .filter(defaultValue -> defaultValue.language().toString().equals("GO"))
          .toList();
        if (valueForLanguage.size() != 1) {
          throw new IllegalStateException("Invalid @PropertyDefaultValue on " + check.getSimpleName() +
            " for language GO");
        }
        valueForLanguage
          .forEach(defaultValue -> repository.rule(ruleKey).param(paramKey).setDefaultValue(defaultValue.defaultValue()));
      }
    }
  }
}
