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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.go.plugin.GoLanguage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.go.utils.LogArg.lazyArg;

public abstract class AbstractReportSensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractReportSensor.class);

  static final long DEFAULT_REMEDIATION_COST = 5L;
  static final Severity DEFAULT_SEVERITY = Severity.MAJOR;
  static final String GENERIC_ISSUE_KEY = "issue";

  protected AbstractReportSensor(AnalysisWarnings analysisWarnings, String propertyKey, String propertyName, String configurationkey) {
    super(analysisWarnings, propertyKey, propertyName, configurationkey, GoLanguage.KEY);
  }

  @Nullable
  abstract ExternalIssue parse(String line);

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return file -> importReport(context, file);
  }

  protected String logPrefix() {
    return this.getClass().getSimpleName() + ": ";
  }

  private void importReport(SensorContext context, File report) {
    try {
      for (String line : Files.readAllLines(report.toPath(), UTF_8)) {
        if (!line.isEmpty()) {
          ExternalIssue issue = parse(line);
          if (issue != null) {
            addLineIssue(context, issue);
          }
        }
      }
    } catch (IOException e) {
      LOG.warn("{}No issues information will be saved as the report file '{}' can't be read.",
        lazyArg(this::logPrefix), report.getPath(), e);
    }
  }

  @CheckForNull
  InputFile getInputFile(SensorContext context, String filePath) {
    var predicates = context.fileSystem().predicates();
    var inputFile = context.fileSystem().inputFile(predicates.or(predicates.hasRelativePath(filePath), predicates.hasAbsolutePath(filePath)));
    if (inputFile == null) {
      LOG.warn("{}No input file found for {}. No {} issues will be imported on this file.", lazyArg(this::logPrefix), filePath, propertyName());
    }
    return inputFile;
  }

  void addLineIssue(SensorContext context, ExternalIssue issue) {
    var inputFile = getInputFile(context, issue.filename());
    if (inputFile != null) {
      var newExternalIssue = context.newExternalIssue();
      var primaryLocation = newExternalIssue.newLocation()
        .message(issue.message())
        .on(inputFile)
        .at(inputFile.selectLine(issue.lineNumber()));

      newExternalIssue
        .at(primaryLocation)
        .ruleId(issue.ruleKey())
        .engineId(issue.linter())
        .type(issue.type())
        .severity(DEFAULT_SEVERITY)
        .remediationEffortMinutes(DEFAULT_REMEDIATION_COST)
        .save();
    }
  }

  public static void createExternalRuleRepository(Context context, String linterId, String linterName) {
    var externalRepo = context.createExternalRepository(linterId, GoLanguage.KEY).setName(linterName);
    var pathToRulesMeta = AbstractExternalRulesDefinition.RULES_JSON_PATH_TEMPLATE.formatted(linterId);

    try (InputStreamReader inputStreamReader = new InputStreamReader(AbstractReportSensor.class.getClassLoader().getResourceAsStream(pathToRulesMeta), StandardCharsets.UTF_8)) {
      var jsonArray = Json.parse(inputStreamReader).asArray();
      for (JsonValue jsonValue : jsonArray) {
        var rule = jsonValue.asObject();
        var newRule = externalRepo.createRule(rule.getString("key", null))
          .setName(rule.getString("name", null))
          .setHtmlDescription(rule.getString("description", null));
        newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().constantPerIssue(DEFAULT_REMEDIATION_COST + "min"));
        if (linterId.equals(GoVetReportSensor.LINTER_ID)) {
          newRule.setType(RuleType.BUG);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't read resource: " + pathToRulesMeta, e);
    }

    externalRepo.done();
  }

}
