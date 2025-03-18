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
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.go.api.checks.GoVersion;

/**
 * The purpose of this GoVersionAnalyzer is to retrieve the go version of the project from the go.mod file.
 * In case we're unable to retrieve the version, we always fallback to an unknown version and don't return null
 * For more information about allowed structure of a go.mod file and the go versions, check the following resources:
 * @see <a href="https://go.dev/doc/toolchain#version">https://go.dev/doc/toolchain#version</a>
 * @see <a href="https://go.dev/doc/modules/gomod-ref#go">https://go.dev/doc/modules/gomod-ref#go</a>
 */
public class GoVersionAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(GoVersionAnalyzer.class);
  private static final Pattern LINE_TERMINATOR = Pattern.compile("[\\n\\r\\u2028\\u2029]");
  private static final String GO_VERSION_PATTERN_STRING = "^go\\s++(?<majorAndMinor>[1-9]\\d*+\\.\\d++)(?<patch>\\.\\d++)?(?:(?:rc|beta)\\d+)?\\s*+$";
  private static final Pattern GO_VERSION_PATTERN = Pattern.compile(GO_VERSION_PATTERN_STRING);

  private final SensorContext sensorContext;

  public GoVersionAnalyzer(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
  }

  public GoVersion analyzeGoVersion() {
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      // Restricted behavior in SQ for IDE
      // TODO: https://sonarsource.atlassian.net/browse/SONARGO-420
      return GoVersion.UNKNOWN_VERSION;
    }

    // hasPath is not supported in SQ for IDE, see above for follow up ticket
    FilePredicates predicates = sensorContext.fileSystem().predicates();
    var goModFilePredicate = predicates.or(predicates.hasPath("go.mod"), predicates.hasPath("src/go.mod"));
    var goModFiles = StreamSupport.stream(
      sensorContext.fileSystem().inputFiles(goModFilePredicate).spliterator(), false).toList();

    if (goModFiles.size() != 1) {
      LOG.debug("Expected exactly one go.mod file, but found {} files.", goModFiles.size());
      return logDetectionFailureAndReturn();
    }

    var goModFile = goModFiles.get(0);
    try {
      var content = goModFile.contents();
      return analyzeGoModFile(content, goModFile.toString());
    } catch (IOException e) {
      LOG.debug("Failed to read go.mod file: {}", goModFile, e);
    }
    return logDetectionFailureAndReturn();
  }

  /**
   * Analyzed the content of a go.mod file and returns the go version.
   * If the go version is not found, the method returns {@link GoVersion#UNKNOWN_VERSION}
   * Information about release candidate (rc) or beta versions are stripped, as we don't need this detail.
   * Also {@link GoVersion} does not support beta/rc versions.
   */
  private static GoVersion analyzeGoModFile(String content, String loggableFilePath) {
    var lines = LINE_TERMINATOR.split(content);
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
    return logDetectionFailureAndReturn();
  }

  private static GoVersion logDetectionFailureAndReturn() {
    LOG.debug("Could not detect the used go version of the project");
    return GoVersion.UNKNOWN_VERSION;
  }

}
