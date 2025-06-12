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

import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.go.api.checks.GoModFileData;
import org.sonar.plugins.go.api.checks.GoVersion;

/**
 * The purpose of this {@link GoModFileAnalyzer} is to retrieve data of the project from the go.mod file.
 * It retrieves some information from it:
 * - the go version used in the project, if missing we fall back to an unknown version and don't return null
 * - the module name
 * For more information about allowed structure of a go.mod file and the go versions, check the following resources:
 * @see <a href="https://go.dev/doc/toolchain#version">https://go.dev/doc/toolchain#version</a>
 * @see <a href="https://go.dev/doc/modules/gomod-ref#go">https://go.dev/doc/modules/gomod-ref#go</a>
 */
public class GoModFileAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(GoModFileAnalyzer.class);
  private static final Pattern LINE_TERMINATOR = Pattern.compile("[\\n\\r\\u2028\\u2029]");
  private static final String STRING_PATTERN_STRING = "(?:\"(?:\\\\.|[^\"\\\\])*+\"|`[^`]*+`)";
  private static final String IDENTIFIER_PATTERN_STRING = "[^\\s]++";
  private static final String GO_MODULE_PATTERN_STRING = "^module\\s++(?<moduleName>(?:%s|%s))\\s*+$".formatted(STRING_PATTERN_STRING, IDENTIFIER_PATTERN_STRING);
  private static final Pattern GO_MODULE_PATTERN = Pattern.compile(GO_MODULE_PATTERN_STRING);
  private static final String GO_VERSION_PATTERN_STRING = "^go\\s++(?<majorAndMinor>[1-9]\\d*+\\.\\d++)(?<patch>\\.\\d++)?(?:(?:rc|beta)\\d+)?\\s*+$";
  private static final Pattern GO_VERSION_PATTERN = Pattern.compile(GO_VERSION_PATTERN_STRING);

  private final SensorContext sensorContext;

  public GoModFileAnalyzer(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
  }

  public GoModFileData analyzeGoModFile() {
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      // Restricted behavior in SQ for IDE
      // TODO: https://sonarsource.atlassian.net/browse/SONARGO-420
      // Early return to avoid unnecessary logging
      return GoModFileData.UNKNOWN_DATA;
    }
    var goModFiles = GoModFileFinder.findGoModFiles(sensorContext);
    if (goModFiles.size() != 1) {
      LOG.debug("Expected exactly one go.mod file, but found {} files.", goModFiles.size());
      return logDetectionFailureAndReturn();
    }

    var goModFile = goModFiles.get(0);
    try {
      var content = goModFile.contents();
      return analyzeGoModFileContent(content, goModFile.toString());
    } catch (IOException e) {
      LOG.debug("Failed to read go.mod file: {}", goModFile, e);
    }
    return logDetectionFailureAndReturn();
  }

  /**
   * Analyzed the content of a go.mod file and returns a {@link GoModFileData} object.
   * If the module name is not found, it will be set to an empty string.
   * If the go version is not found, the version will be set to {@link GoVersion#UNKNOWN_VERSION}
   * Information about release candidate (rc) or beta versions are stripped, as we don't need this detail.
   * Also {@link GoVersion} does not support beta/rc versions.
   */
  private static GoModFileData analyzeGoModFileContent(String content, String loggableFilePath) {
    var lines = LINE_TERMINATOR.split(content);
    var moduleName = extractModuleName(lines, loggableFilePath);
    var goVersion = extractGoVersion(lines, loggableFilePath);
    return new GoModFileData(moduleName, goVersion);
  }

  private static String extractModuleName(String[] lines, String loggableFilePath) {
    for (String line : lines) {
      var matcher = GO_MODULE_PATTERN.matcher(line);
      if (matcher.find()) {
        String moduleName = removeQuotes(matcher.group("moduleName"));

        LOG.debug("Detected go module name in project: {}", moduleName);
        return moduleName;
      }
    }
    LOG.debug("Failed to detect a module name in the go.mod file: {}", loggableFilePath);
    return "";
  }

  private static String removeQuotes(String string) {
    if ((string.startsWith("\"") && string.endsWith("\"")) || (string.startsWith("`") && string.endsWith("`"))) {
      return string.substring(1, string.length() - 1);
    }
    return string;
  }

  private static GoVersion extractGoVersion(String[] lines, String loggableFilePath) {
    for (String line : lines) {
      var matcher = GO_VERSION_PATTERN.matcher(line);
      if (matcher.find()) {
        // cannot be null, as the matcher has found a match
        String versionToParse = matcher.group("majorAndMinor");

        String patch = matcher.group("patch");

        if (patch != null) {
          versionToParse = versionToParse.concat(patch);
        }

        var version = GoVersion.parse(versionToParse);
        LOG.debug("Detected go version in project: {}", version);
        return version;
      }
    }
    LOG.debug("Failed to detect a go version in the go.mod file: {}", loggableFilePath);
    return GoVersion.UNKNOWN_VERSION;
  }

  private static GoModFileData logDetectionFailureAndReturn() {
    LOG.debug("Could not detect the metadata from mod file of the project");
    return GoModFileData.UNKNOWN_DATA;
  }
}
