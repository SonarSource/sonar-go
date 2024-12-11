/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.slang.externalreport.CheckstyleFormatImporter;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public class GolangCILintReportSensor extends AbstractPropertyHandlerSensor {

  public static final String LINTER_KEY = "golangci-lint";
  public static final String LINTER_NAME = "GolangCI-Lint";

  public static final String PROPERTY_KEY = "sonar.go.golangci-lint.reportPaths";

  public GolangCILintReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, PROPERTY_KEY, GoLanguage.KEY);
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return new GolangCILintCheckstyleFormatImporter(context, LINTER_KEY)::importFile;
  }

  private static class GolangCILintCheckstyleFormatImporter extends CheckstyleFormatImporter {

    private static final String GOSEC = "gosec";
    private final SensorContext context;

    public GolangCILintCheckstyleFormatImporter(SensorContext context, String linterKey) {
      super(context, linterKey);
      this.context = context;
    }

    /**
     * Current strategy to define rule type for Golangci-lint:
     * <ul>
     * <li> (null, "gosec") -> VULNERABILITY
     * <li> ("error", null) -> BUG
     * <li> (not "error", null) -> CODE_SMELL
     * </ul>
     *
     * <a href="https://github.com/securego/gosec">Gosec</a> is the only linter importing VULNERABILITY issues for now.
     */
    @Override
    protected RuleType ruleType(@Nullable String severity, String source) {
      if (GOSEC.equals(source)) {
        return RuleType.VULNERABILITY;
      }
      return super.ruleType(severity, source);
    }

    @Override
    protected RuleKey createRuleKey(String source, RuleType ruleType, Severity ruleSeverity) {
      if (GOSEC.equals(source)) {
        // gosec issues are exclusively "major vulnerability", keeping "gosec" as rule key.
        return RuleKey.of(linterKey, source);
      }
      String ruleKey = String.format("%s.%s.%s", source, ruleType.toString().toLowerCase(Locale.ROOT),
        ruleSeverity.toString().toLowerCase(Locale.ROOT));
      return RuleKey.of(linterKey, ruleKey);
    }

    @Override
    protected List<Impact> impacts(String severity, String source) {
      var isSonarCloud = context.runtime().getProduct() == SonarProduct.SONARQUBE && context.runtime().getEdition() == SonarEdition.SONARCLOUD;
      if (!isSonarCloud) {
        // SonarQube Cloud does not yet support the `impact` field for external issues
        if (GOSEC.equals(source)) {
          return List.of(new Impact(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.MEDIUM));
        }
        return List.of(new Impact(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.MEDIUM));
      }
      return List.of();
    }
  }
}
