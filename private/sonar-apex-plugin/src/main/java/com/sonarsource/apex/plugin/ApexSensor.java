/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import com.sonarsource.apex.converter.ApexCodeVerifier;
import com.sonarsource.apex.converter.ApexConverter;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.SlangSensor;

public class ApexSensor extends SlangSensor {

  private final Checks<SlangCheck> checks;

  public ApexSensor(SonarRuntime sonarRuntime, CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, ApexLanguage language) {
    super(sonarRuntime, noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(ApexPlugin.APEX_REPOSITORY_KEY);

    checks.addAnnotatedChecks((Iterable<?>) ApexCheckList.checks());
    checks.addAnnotatedChecks(new CommentedCodeCheck(new ApexCodeVerifier()));
  }

  @Override
  protected ASTConverter astConverter(SensorContext sensorContext) {
    return new ApexConverter();
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

  @Override
  protected String repositoryKey() {
    return ApexPlugin.APEX_REPOSITORY_KEY;
  }
}
