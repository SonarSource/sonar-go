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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public void saveCoverage(SensorContext context, Coverage coverage, Set<GoModFileData> goModFileDataSet, Path reportPath) {
    coverage.fileMap.forEach((filePath, coverageStats) -> {
      try {
        if (!coverageStats.isEmpty()) {
          saveFileCoverage(context, filePath, coverageStats, goModFileDataSet, reportPath);
        }
      } catch (Exception e) {
        LOG.warn("Failed saving coverage info for file: {}", filePath);
      }
    });
  }

  private static void saveFileCoverage(SensorContext sensorContext, String filePath, List<CoverageStat> coverageStats, Set<GoModFileData> goModFileDataSet, Path reportPath)
    throws IOException {
    FileSystem fileSystem = sensorContext.fileSystem();
    InputFile inputFile = findInputFile(filePath, fileSystem, goModFileDataSet, reportPath);
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
   *
   * @param filename         The filename from coverage report, can be absolute or relative and can be prefixed with module name.
   * @param fileSystem       The FileSystem of the project.
   * @param goModFileDataSet The Set of go.mod file data of the Go modules.
   * @param reportPath       The path to the coverage report file.
   * @return The resolved InputFile, or null if the file cannot be resolved.
   */
  @CheckForNull
  private static InputFile findInputFile(String filename, FileSystem fileSystem, Set<GoModFileData> goModFileDataSet, Path reportPath) {
    FilePredicates predicates = fileSystem.predicates();
    // try to resolve absolute path first
    var inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(filename));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using absolute path.", filename, inputFile);
      return inputFile;
    }
    var goModFileData = findNearestGoModFileData(goModFileDataSet, filename);
    inputFile = resolveWithModuleName(filename, fileSystem, reportPath, predicates, goModFileData);
    if (inputFile != null) {
      return inputFile;
    }
    inputFile = resolveRelative(filename, fileSystem, predicates);
    if (inputFile != null) {
      return inputFile;
    }
    inputFile = resolveRelativeWithoutModuleName(filename, fileSystem, reportPath, predicates, goModFileData);
    if (inputFile != null) {
      return inputFile;
    }
    return resolveUsingSubpaths(filename, fileSystem, predicates);
  }

  @CheckForNull
  private static InputFile resolveWithModuleName(String filename, FileSystem fileSystem, Path reportPath, FilePredicates predicates, GoModFileData goModFileData) {
    if (goModFileData.moduleName().isBlank()) {
      return null;
    }
    var prefix = goModFileData.moduleName() + "/";
    if (!filename.startsWith(prefix)) {
      return null;
    }
    var filenameNoModuleName = filename.substring(prefix.length());
    Path goModParentPath;
    try {
      var goModFilePath = Path.of(goModFileData.goModFilePath());
      goModParentPath = goModFilePath.getParent();
    } catch (InvalidPathException e) {
      goModParentPath = null;
    }
    if (goModParentPath != null) {
      var filenameNoModuleInGoModDir = goModParentPath.resolve(filenameNoModuleName);
      var inputFile = fileSystem.inputFile(predicates.hasRelativePath(filenameNoModuleInGoModDir.toString()));
      if (inputFile != null) {
        LOG.debug("Resolved file '{}' to '{}' using relative path, without module name in go.mod directory '{}'", filename, inputFile, goModParentPath);
        return inputFile;
      }
    }
    var filenameNoModuleNameInReportPath = reportPath.getParent().resolve(filenameNoModuleName);
    var inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(filenameNoModuleNameInReportPath.toString()));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using absolute path, without module name in report path '{}'", filename, inputFile, reportPath.getParent());
      return inputFile;
    }
    return null;
  }

  @CheckForNull
  private static InputFile resolveRelative(String filename, FileSystem fileSystem, FilePredicates predicates) {
    var inputFile = fileSystem.inputFile(predicates.hasRelativePath(filename));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using relative path", filename, inputFile);
      return inputFile;
    }
    return null;
  }

  @CheckForNull
  private static InputFile resolveRelativeWithoutModuleName(String filename, FileSystem fileSystem, Path reportPath, FilePredicates predicates, GoModFileData goModFileData) {
    if (goModFileData.moduleName().isBlank()) {
      return null;
    }
    var prefix = goModFileData.moduleName() + "/";
    if (!filename.startsWith(prefix)) {
      return null;
    }
    var filenameNoModuleName = filename.substring(prefix.length());
    var filenameNoModuleNameInReportPath = reportPath.getParent().resolve(filenameNoModuleName);
    var inputFile = fileSystem.inputFile(predicates.hasRelativePath(filenameNoModuleNameInReportPath.toString()));
    if (inputFile != null) {
      LOG.debug("Resolved file '{}' to '{}' using relative path, without module name in report path {}", filename, inputFile, reportPath.getParent());
      return inputFile;
    }
    return null;
  }

  @CheckForNull
  private static InputFile resolveUsingSubpaths(String filename, FileSystem fileSystem, FilePredicates predicates) {
    // old fallback behavior: trying to resolve as relative path of subpaths, e.g.:
    // for filename "a/b/c/d.go", try "a/b/c/d.go", "b/c/d.go", "c/d.go, "d.go"
    Path path = Paths.get(filename);
    var inputFile = fileSystem.inputFile(predicates.hasRelativePath(path.toString()));
    while (inputFile == null && path.getNameCount() > 1) {
      path = path.subpath(1, path.getNameCount());
      inputFile = fileSystem.inputFile(predicates.hasRelativePath(path.toString()));
      if (inputFile != null) {
        LOG.debug("Resolved file '{}' to '{}' using relative path by searching subpaths", filename, inputFile);
      }
    }
    return inputFile;
  }

  private static GoModFileData findNearestGoModFileData(Set<GoModFileData> goModFileDataSet, String filename) {
    GoModFileData nearest = GoModFileData.UNKNOWN_DATA;
    int longestPrefixLength = 0;

    for (GoModFileData goModFileData : goModFileDataSet) {
      String moduleName = goModFileData.moduleName();
      if (!moduleName.isBlank() && filename.startsWith(moduleName + "/")) {
        int prefixLength = moduleName.length();
        if (prefixLength > longestPrefixLength) {
          longestPrefixLength = prefixLength;
          nearest = goModFileData;
        }
      }
    }

    return nearest;
  }
}
