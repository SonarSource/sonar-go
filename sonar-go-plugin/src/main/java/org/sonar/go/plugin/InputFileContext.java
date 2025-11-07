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
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.go.visitors.TreeContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

import static org.sonar.go.plugin.GoSensor.isFailFast;

public class InputFileContext extends TreeContext {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileContext.class);

  private static final String PARSING_ERROR_RULE_KEY = "S2260";
  private static final int MIN_NUMBER_OF_PARTS_FOR_LOCATION_EXTRACT = 3;
  private static final int LINE_INDEX = 1;
  private static final int LINE_OFFSET_INDEX = 2;
  private Map<String, Set<org.sonar.plugins.go.api.TextRange>> filteredRules = new HashMap<>();

  public final SensorContext sensorContext;

  public final InputFile inputFile;

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
  }

  @CheckForNull
  public TextRange textRange(@Nullable org.sonar.plugins.go.api.TextRange textRange) {
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
      // Extra security check for invalid TextRange.
      // It shouldn't happen anymore since we disabled line directives in Go parser.
      var numberOfLines = inputFile.lines();
      var message = "Invalid %s, for file: %s, number of lines: %s".formatted(textRange, inputFile, numberOfLines);
      LOG.debug(message, e);
      if (isFailFast(sensorContext)) {
        throw new IllegalStateException(message, e);
      }
    }
    return null;
  }

  public void reportIssue(RuleKey ruleKey,
    @Nullable org.sonar.plugins.go.api.TextRange textRange,
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

  public void reportAnalysisParseError(String repositoryKey, String errorMessage) {
    var location = extractLocation(errorMessage);
    reportAnalysisError("Unable to parse file: " + inputFile, location);
    RuleKey parsingErrorRuleKey = RuleKey.of(repositoryKey, PARSING_ERROR_RULE_KEY);
    if (sensorContext.activeRules().find(parsingErrorRuleKey) == null) {
      return;
    }
    NewIssue parseError = sensorContext.newIssue();
    NewIssueLocation parseErrorLocation = parseError.newLocation()
      .on(inputFile)
      .message("A parsing error occurred in this file.");

    Optional.of(location)
      .map(org.sonar.plugins.go.api.TextPointer::line)
      .flatMap(l -> safeExtractSelectLine(inputFile, l))
      .ifPresent(parseErrorLocation::at);

    parseError
      .forRule(parsingErrorRuleKey)
      .at(parseErrorLocation)
      .save();
  }

  private static org.sonar.plugins.go.api.TextPointer extractLocation(String errorMessage) {
    // Example error message:
    // foo.go:1:1: illegal character U+0024 '$'
    String[] parts = errorMessage.split(":");
    if (parts.length < MIN_NUMBER_OF_PARTS_FOR_LOCATION_EXTRACT) {
      return new TextPointerImpl(1, 0);
    }
    try {
      var line = Integer.parseInt(parts[LINE_INDEX]);
      var lineOffset = Integer.parseInt(parts[LINE_OFFSET_INDEX]);
      return new TextPointerImpl(line, lineOffset);
    } catch (NumberFormatException e) {
      // Nothing abnormal, this means that the error is not coming from the Go parser, but one issue thrown locally (ex: when converting the
      // json).
      return new TextPointerImpl(1, 0);
    }
  }

  public void reportAnalysisError(String message, @Nullable org.sonar.plugins.go.api.TextPointer location) {
    NewAnalysisError error = sensorContext.newAnalysisError();
    error
      .message(message)
      .onFile(inputFile);

    if (location != null) {
      try {
        TextPointer pointerLocation = inputFile.newPointer(location.line(), location.lineOffset());
        error.at(pointerLocation);
      } catch (IllegalArgumentException e) {
        LOG.debug("Invalid location '{}' for file {} when reporting parsing error.", location, inputFile, e);
      }
    }

    error.save();
  }

  // Visible for testing
  static Optional<TextRange> safeExtractSelectLine(InputFile inputFile, int line) {
    try {
      return Optional.of(inputFile.selectLine(line));
    } catch (IllegalArgumentException e) {
      LOG.debug("Invalid line '{}' for file {} when reporting parsing error.", line, inputFile, e);
    }
    return Optional.empty();
  }

  public void setFilteredRules(Map<String, Set<org.sonar.plugins.go.api.TextRange>> filteredRules) {
    this.filteredRules = filteredRules;
  }

}
