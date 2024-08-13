/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.externalreport;

import com.sonarsource.apex.plugin.ApexPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public class PmdSensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = LoggerFactory.getLogger(PmdSensor.class);

  public static final String REPORT_PROPERTY_KEY = "sonar.apex.pmd.reportPaths";

  public static final String LINTER_KEY = "pmd_apex";
  public static final String LINTER_NAME = "PMD";

  public PmdSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, REPORT_PROPERTY_KEY, ApexPlugin.APEX_LANGUAGE_KEY);
  }

  public static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    PmdSensor.LINTER_KEY,
    PmdSensor.LINTER_NAME,
    "org/sonar/l10n/apex/rules/pmd/rules.json",
    ApexPlugin.APEX_LANGUAGE_KEY);

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return file -> importReport(file, context);
  }

  private static void importReport(File reportFile, SensorContext context) {
    try {
      PmdXmlReportReader.read(context, reportFile, RULE_LOADER);
    } catch (FileNotFoundException e) {
      LOG.error("Can't find PMD XML report: {}", reportFile);
    } catch (XMLStreamException | IOException | RuntimeException e) {
      LOG.error("Can't read PMD XML report: {}", reportFile, e);
    }
  }

}
