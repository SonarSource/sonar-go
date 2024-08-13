/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import java.util.List;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.sonarsource.slang.checks.CommentedCodeCheck;

public class ApexRulesDefinition  implements RulesDefinition {
  private static final String RESOURCE_FOLDER = "org/sonar/l10n/apex/rules/apex";
  private static final String FORMAT_PARAMETER_NAME = "format";

  private enum PropertyOverride {
    S100_FORMAT("S100", FORMAT_PARAMETER_NAME, "^[a-z][a-zA-Z0-9_]*$"),
    S101_FORMAT("S101", FORMAT_PARAMETER_NAME, "^[A-Z][a-zA-Z0-9_]*$"),
    S117_FORMAT("S117", FORMAT_PARAMETER_NAME, "^[a-z][a-zA-Z0-9_]*$");

    private String ruleKey;
    private String paramName;
    private String paramValue;

    PropertyOverride(String ruleKey, String paramName, String paramValue) {
      this.ruleKey = ruleKey;
      this.paramName = paramName;
      this.paramValue = paramValue;
    }
  }

  private final SonarRuntime runtime;

  public ApexRulesDefinition(SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(RulesDefinition.Context context) {
    RulesDefinition.NewRepository repository = context
        .createRepository(ApexPlugin.APEX_REPOSITORY_KEY, ApexPlugin.APEX_LANGUAGE_KEY)
        .setName(ApexPlugin.REPOSITORY_NAME);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, ApexProfileDefinition.PATH_TO_JSON, runtime);

    List<Class<?>> checks = ApexCheckList.checks();
    checks.add(CommentedCodeCheck.class);

    ruleMetadataLoader.addRulesByAnnotatedClass(repository, checks);
    applyOverrides(repository);

    repository.done();
  }

  private static void applyOverrides(NewRepository repository) {
    for (PropertyOverride override : PropertyOverride.values()) {
      NewRule rule = repository.rule(override.ruleKey);
      if (rule != null) {
        NewParam param = rule.param(override.paramName);
        if (param != null) {
          param.setDefaultValue(override.paramValue);
        }
      }
    }
  }
}
