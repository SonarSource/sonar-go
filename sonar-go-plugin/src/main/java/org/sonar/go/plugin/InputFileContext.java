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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.go.api.checks.SecondaryLocation;
import org.sonar.go.visitors.TreeContext;

public class InputFileContext extends TreeContext {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileContext.class);

  private static final String PARSING_ERROR_RULE_KEY = "S2260";
  private Map<String, Set<org.sonar.go.api.TextRange>> filteredRules = new HashMap<>();

  public final SensorContext sensorContext;

  public final InputFile inputFile;

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
  }

  @CheckForNull
  public TextRange textRange(@Nullable org.sonar.go.api.TextRange textRange) {
    if (textRange == null) {
      return null;
    }
    try {
      return inputFile.newRange(
        textRange.start().line(),
        textRange.start().lineOffset(),
        textRange.end().line(),
        textRange.end().lineOffset());
    } catch (IllegalArgumentException e) {
      // When Go file contains comments like: /*line :6:1*/ then the TextRanges are taken from such comments and not from real location.
      // It can be disabled in call: parser.ParseFile(fileSet, filename, fileContent, 0)
      // (last argument equals zero instead of parser.ParseComments), but then the comments are missing in the AST.
      // To avoid exceptions in visitors, there is extra validation if the TextRange is valid.
      // If not, it is logged below and `null` is returned.
      var numberOfLines = inputFile.lines();
      var message = "Invalid %s, for file: %s, number of lines: %s".formatted(textRange, inputFile, numberOfLines);
      LOG.debug(message, e);
    }
    return null;
  }

  public void reportIssue(RuleKey ruleKey,
    @Nullable org.sonar.go.api.TextRange textRange,
    String message,
    List<SecondaryLocation> secondaryLocations,
    @Nullable Double gap) {

    if (textRange != null && filteredRules.getOrDefault(ruleKey.toString(), Collections.emptySet())
      .stream().anyMatch(textRange::isInside)) {
      // Issue is filtered by one of the filter.
      return;
    }

    NewIssue issue = sensorContext.newIssue();
    NewIssueLocation issueLocation = issue.newLocation()
      .on(inputFile)
      .message(message);
    var location = textRange(textRange);
    if (location != null) {
      issueLocation.at(location);
    }

    issue
      .forRule(ruleKey)
      .at(issueLocation)
      .gap(gap);

    secondaryLocations.forEach(secondary -> {
      var newIssueLocation = issue.newLocation()
        .on(inputFile)
        .message(secondary.message == null ? "" : secondary.message);
      var secondaryLocation = textRange(secondary.textRange);
      if (secondaryLocation != null) {
        newIssueLocation.at(secondaryLocation);
      }
      issue.addLocation(newIssueLocation);
    });

    issue.save();
  }

  public void reportAnalysisParseError(String repositoryKey, InputFile inputFile, @Nullable org.sonar.go.api.TextPointer location) {
    reportAnalysisError("Unable to parse file: " + inputFile, location);
    RuleKey parsingErrorRuleKey = RuleKey.of(repositoryKey, PARSING_ERROR_RULE_KEY);
    if (sensorContext.activeRules().find(parsingErrorRuleKey) == null) {
      return;
    }
    NewIssue parseError = sensorContext.newIssue();
    NewIssueLocation parseErrorLocation = parseError.newLocation()
      .on(inputFile)
      .message("A parsing error occurred in this file.");

    Optional.ofNullable(location)
      .map(org.sonar.go.api.TextPointer::line)
      .map(inputFile::selectLine)
      .ifPresent(parseErrorLocation::at);

    parseError
      .forRule(parsingErrorRuleKey)
      .at(parseErrorLocation)
      .save();
  }

  public void reportAnalysisError(String message, @Nullable org.sonar.go.api.TextPointer location) {
    NewAnalysisError error = sensorContext.newAnalysisError();
    error
      .message(message)
      .onFile(inputFile);

    if (location != null) {
      TextPointer pointerLocation = inputFile.newPointer(location.line(), location.lineOffset());
      error.at(pointerLocation);
    }

    error.save();
  }

  public void setFilteredRules(Map<String, Set<org.sonar.go.api.TextRange>> filteredRules) {
    this.filteredRules = filteredRules;
  }

}
