/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.go.plugin.GoLanguage;

public class GolangCILintReportSensor extends AbstractPropertyHandlerSensor {

  public static final String LINTER_ID = "golangci-lint";
  public static final String LINTER_NAME = "GolangCI-Lint";

  public static final String PROPERTY_KEY = "sonar.go.golangci-lint.reportPaths";

  public GolangCILintReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_ID, LINTER_NAME, PROPERTY_KEY, GoLanguage.KEY);
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return new GolangCILintCheckstyleFormatImporter(context, LINTER_ID)::importFile;
  }

  private static class GolangCILintCheckstyleFormatImporter extends CheckstyleFormatImporter {

    // Example format: "assign: self-assignment of name to name"
    private static final Pattern GOVET_RULE_ID_PATTERN = Pattern.compile("^([a-z]+):");

    public GolangCILintCheckstyleFormatImporter(SensorContext context, String linterKey) {
      super(context, linterKey);
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
      if ("gosec".equals(source)) {
        return RuleType.VULNERABILITY;
      }
      return super.ruleType(severity, source);
    }

    @Override
    protected RuleKey createRuleKey(String source, RuleType ruleType, Severity ruleSeverity, String message) {
      if ("gosec".equals(source)) {
        // gosec issues are exclusively "major vulnerability", keeping "gosec" as rule key.
        return RuleKey.of(linterKey, source);
      }
      if ("govet".equals(source)) {
        // govet rules are already defined in GoVetRulesDefinition, reusing the rule keys to have consistent CCT
        var matcher = GOVET_RULE_ID_PATTERN.matcher(message);
        if (matcher.find()) {
          var ruleKey = matcher.group(1);
          return RuleKey.of("govet", ruleKey);
        }
      }
      String ruleKey = String.format("%s.%s.%s", source, ruleType.toString().toLowerCase(Locale.ROOT),
        ruleSeverity.toString().toLowerCase(Locale.ROOT));
      return RuleKey.of(linterKey, ruleKey);
    }
  }
}
