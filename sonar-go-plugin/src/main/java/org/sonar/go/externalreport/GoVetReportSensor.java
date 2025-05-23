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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rules.RuleType;

import static org.sonar.go.utils.LogArg.lazyArg;

public class GoVetReportSensor extends AbstractReportSensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoVetReportSensor.class);

  public static final String PROPERTY_KEY = "sonar.go.govet.reportPaths";

  private static final Pattern GO_VET_LINE_REGEX = Pattern.compile("(?<file>[^:]+):(?<line>\\d+):(?<column>\\d+:)?(?<message>.*)");

  public static final String LINTER_ID = "govet";
  public static final String LINTER_NAME = "go vet";

  public GoVetReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_ID, LINTER_NAME, PROPERTY_KEY);
  }

  @Nullable
  @Override
  ExternalIssue parse(String line) {
    Matcher matcher = GO_VET_LINE_REGEX.matcher(line);
    if (matcher.matches()) {
      String filename = matcher.group("file").trim();
      int lineNumber = Integer.parseInt(matcher.group("line").trim());
      String message = matcher.group("message").trim();
      return new ExternalIssue(LINTER_ID, RuleType.BUG, null, filename, lineNumber, message);
    } else if (!line.startsWith("exit status") && !line.startsWith("# ")) {
      // go vet in go>=1.12 outputs "# <package name>" before the report, and go<1.12 outputs "exit status <N>" after the report
      LOG.debug("{}Unexpected line: {}", lazyArg(this::logPrefix), line);
    }
    return null;
  }

}
