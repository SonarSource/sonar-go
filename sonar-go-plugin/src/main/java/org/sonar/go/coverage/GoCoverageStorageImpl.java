/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonar.go.coverage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.plugins.go.api.checks.GoModFileData;

class GoCoverageStorageImpl implements GoCoverageStorage {
  private static final Logger LOG = LoggerFactory.getLogger(GoCoverageStorageImpl.class);

  @Override
  public void saveCoverage(SensorContext context, Coverage coverage, GoModFileData goModFileData, Path reportPath) {
    coverage.fileMap.forEach((filePath, coverageStats) -> {
      try {
        if (!coverageStats.isEmpty()) {
          saveFileCoverage(context, filePath, coverageStats, goModFileData, reportPath);
        }
      } catch (Exception e) {
        LOG.warn("Failed saving coverage info for file: {}", filePath);
      }
    });
  }

  private static void saveFileCoverage(SensorContext sensorContext, String filePath, List<CoverageStat> coverageStats, GoModFileData goModFileData, Path reportPath)
    throws IOException {
    FileSystem fileSystem = sensorContext.fileSystem();
    InputFile inputFile = findInputFile(filePath, fileSystem, goModFileData, reportPath);
    if (inputFile != null) {
      LOG.debug("Saving coverage measures for file '{}'", filePath);
      List<String> lines = Arrays.asList(inputFile.contents().split("\\r?\\n"));
      NewCoverage newCoverage = sensorContext.newCoverage().onFile(inputFile);
      FileCoverage fileCoverage = new FileCoverage(coverageStats, lines);
      for (Map.Entry<Integer, LineCoverage> entry : fileCoverage.lineMap.entrySet()) {
        newCoverage.lineHits(entry.getKey(), entry.getValue().hits);
      }
      newCoverage.save();
    } else {
      LOG.warn("File '{}' is not included in the project, ignoring coverage", filePath);
    }
  }

  /**
   * The method is trying to resolve the file from coverage report to an InputFile in the project in the following way:
   * <ul>
   *  <li> Try to resolve the file using absolute path as-is.</li>
   *  <li> If module name is defined in go.mod file and the filename starts with module name + '/',
   *       try to resolve the file using absolute path without module name in report path.</li>
   *  <li> Try to resolve the file using relative path.</
   * </ul>
   * @param filename The filename from coverage report, can be absolute or relative and can be prefixed with module name.
   * @param fileSystem The FileSystem of the project.
   * @param goModFileData The go.mod file data of the project.
   * @param reportPath The path to the coverage report file.
   * @return The resolved InputFile, or null if the file cannot be resolved.
   */
  @CheckForNull
  private static InputFile findInputFile(String filename, FileSystem fileSystem, GoModFileData goModFileData, Path reportPath) {
    FilePredicates predicates = fileSystem.predicates();
    // try to resolve absolute path first
    var inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(filename));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using absolute path.", filename, inputFile);
      return inputFile;
    }

    if (!goModFileData.moduleName().isBlank()) {
      var prefix = goModFileData.moduleName() + "/";
      if (filename.startsWith(prefix)) {
        var filenameNoModuleName = filename.substring(prefix.length());
        // try to resolve absolute to report directory and without module name
        var filenameNoModuleNameInReportPath = reportPath.getParent().resolve(filenameNoModuleName);
        inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(filenameNoModuleNameInReportPath.toString()));
        if (inputFile != null) {
          LOG.debug("Resolved file '{}' to '{}' using absolute path, without module name in report path '{}'", filename, inputFile, reportPath.getParent());
          return inputFile;
        }
      }
    }

    inputFile = fileSystem.inputFile(predicates.hasRelativePath(filename));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using relative path", filename, inputFile);
      return inputFile;
    }

    if (!goModFileData.moduleName().isBlank()) {
      var prefix = goModFileData.moduleName() + "/";
      if (filename.startsWith(prefix)) {
        var filenameNoModuleName = filename.substring(prefix.length());
        // try to resolve relative to report directory and without module name
        var filenameNoModuleNameInReportPath = reportPath.getParent().resolve(filenameNoModuleName);
        inputFile = fileSystem.inputFile(predicates.hasRelativePath(filenameNoModuleNameInReportPath.toString()));
        if (inputFile != null) {
          LOG.debug("Resolved file '{}' to '{}' using relative path, without module name in report path {}", filename, inputFile, reportPath.getParent());
          return inputFile;
        }
      }
    }
    return null;
  }
}
