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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
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
 * - the replaced modules, if any (support both single and multi replace statements)
 * For more information about allowed structure of a go.mod file and the go versions, check the following resources:
 * @see <a href="https://go.dev/doc/toolchain#version">https://go.dev/doc/toolchain#version</a>
 * @see <a href="https://go.dev/doc/modules/gomod-ref#go">https://go.dev/doc/modules/gomod-ref#go</a>
 * @see <a href="https://go.dev/ref/mod#go-mod-file-replace">https://go.dev/ref/mod#go-mod-file-replace</a>
 */
public class GoModFileAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(GoModFileAnalyzer.class);
  private static final Pattern LINE_TERMINATOR = Pattern.compile("[\\n\\r\\u2028\\u2029]");
  // The pattern for a string or identifier in Go, which can be a quoted string (" or `) or an unquoted identifier. See
  // https://go.dev/ref/mod#go-mod-file-lexical
  private static final String STRING_OR_IDENTIFIER_PATTERN_STRING = "\"(?:\\\\.|[^\"\\\\])*+\"|[^\\s]++";
  private static final String REPLACE_SPEC = "\\s*+(?<moduleLeft>" + STRING_OR_IDENTIFIER_PATTERN_STRING + ")(?:\\s++(?<versionLeft>[^\\s]++))?\\s*+=>\\s*+(?<moduleRight>"
    + STRING_OR_IDENTIFIER_PATTERN_STRING + ")(?:\\s++(?<versionRight>[^\\s]++))?\\s*+$";
  private static final Pattern MODULE_PATTERN = Pattern.compile("^module\\s++(?<moduleName>" + STRING_OR_IDENTIFIER_PATTERN_STRING + ")\\s*+$");
  private static final Pattern VERSION_PATTERN = Pattern.compile("^go\\s++(?<majorAndMinor>[1-9]\\d*+\\.\\d++)(?<patch>\\.\\d++)?(?:(?:rc|beta)\\d+)?\\s*+$");
  private static final Pattern SIMPLE_REPLACE_PATTERN = Pattern.compile("^replace\\s++" + REPLACE_SPEC + "$");
  private static final Pattern MULTIPLE_REPLACE_PATTERN = Pattern.compile("^replace\\s++\\(\\s*+$");
  private static final Pattern REPLACE_SPEC_PATTERN = Pattern.compile(REPLACE_SPEC);

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
   * Method is public for tests, but should be called by {@link #analyzeGoModFile()}.
   */
  public static GoModFileData analyzeGoModFileContent(String content, String loggableFilePath) {
    var lines = LINE_TERMINATOR.split(content);
    var moduleName = "";
    var goVersion = GoVersion.UNKNOWN_VERSION;
    List<Map.Entry<GoModFileData.ModuleSpec, GoModFileData.ModuleSpec>> replacedModules = new ArrayList<>();

    var lineIterator = Arrays.stream(lines).iterator();
    while (lineIterator.hasNext()) {
      var line = lineIterator.next();

      if (goVersion == GoVersion.UNKNOWN_VERSION) {
        var optVersion = parseVersion(line);
        if (optVersion.isPresent()) {
          goVersion = optVersion.get();
        }
      }

      var optModuleName = parseModuleName(line);
      if (optModuleName.isPresent() && moduleName.isEmpty()) {
        moduleName = optModuleName.get();
      }

      replacedModules.addAll(parseReplacedModules(line, lineIterator));
    }

    logDetails(goVersion, moduleName, loggableFilePath);
    return new GoModFileData(moduleName, goVersion, replacedModules);
  }

  private static void logDetails(GoVersion goVersion, String moduleName, String loggableFilePath) {
    if (goVersion == GoVersion.UNKNOWN_VERSION) {
      LOG.debug("Failed to detect a go version in the go.mod file: {}", loggableFilePath);
    } else {
      LOG.debug("Detected go version in project: {}", goVersion);
    }
    if (moduleName.isEmpty()) {
      LOG.debug("Failed to detect a module name in the go.mod file: {}", loggableFilePath);
    } else {
      LOG.debug("Detected go module name in project: {}", moduleName);
    }
  }

  private static Optional<String> parseModuleName(String line) {
    var matcher = MODULE_PATTERN.matcher(line);
    if (matcher.find()) {
      String moduleName = matcher.group("moduleName");
      return Optional.of(removeQuotes(moduleName));
    } else {
      return Optional.empty();
    }
  }

  private static Optional<GoVersion> parseVersion(String line) {
    var matcher = VERSION_PATTERN.matcher(line);
    if (matcher.find()) {
      String versionToParse = matcher.group("majorAndMinor");
      String patch = matcher.group("patch");

      if (patch != null) {
        versionToParse = versionToParse.concat(patch);
      }

      var version = GoVersion.parse(versionToParse);
      return Optional.of(version);
    } else {
      return Optional.empty();
    }
  }

  private static List<Map.Entry<GoModFileData.ModuleSpec, GoModFileData.ModuleSpec>> parseReplacedModules(String line, Iterator<String> lineIterator) {
    var matcherSimpleReplace = SIMPLE_REPLACE_PATTERN.matcher(line);
    if (matcherSimpleReplace.find()) {
      return List.of(processReplaceSpec(matcherSimpleReplace));
    }

    var matcherMultiReplace = MULTIPLE_REPLACE_PATTERN.matcher(line);
    if (matcherMultiReplace.find()) {
      return processMultiReplaceModule(lineIterator);
    }

    return Collections.emptyList();
  }

  private static List<Map.Entry<GoModFileData.ModuleSpec, GoModFileData.ModuleSpec>> processMultiReplaceModule(Iterator<String> lineIterator) {
    var result = new ArrayList<Map.Entry<GoModFileData.ModuleSpec, GoModFileData.ModuleSpec>>();
    while (lineIterator.hasNext()) {
      String line = lineIterator.next();
      if (")".equals(line.trim())) {
        break;
      }
      var matcher = REPLACE_SPEC_PATTERN.matcher(line);
      if (matcher.find()) {
        result.add(processReplaceSpec(matcher));
      }
    }
    return result;
  }

  private static Map.Entry<GoModFileData.ModuleSpec, GoModFileData.ModuleSpec> processReplaceSpec(Matcher matcher) {
    String moduleLeft = removeQuotes(matcher.group("moduleLeft"));
    String versionLeft = matcher.group("versionLeft");
    String moduleRight = removeQuotes(matcher.group("moduleRight"));
    String versionRight = matcher.group("versionRight");
    var moduleSpecLeft = new GoModFileData.ModuleSpec(moduleLeft, versionLeft);
    var moduleSpecRight = new GoModFileData.ModuleSpec(moduleRight, versionRight);
    return Map.entry(moduleSpecLeft, moduleSpecRight);
  }

  private static GoModFileData logDetectionFailureAndReturn() {
    LOG.debug("Could not detect the metadata from mod file of the project");
    return GoModFileData.UNKNOWN_DATA;
  }

  private static String removeQuotes(String string) {
    if (string.startsWith("\"") && string.endsWith("\"")) {
      return string.substring(1, string.length() - 1);
    }
    return string;
  }
}
