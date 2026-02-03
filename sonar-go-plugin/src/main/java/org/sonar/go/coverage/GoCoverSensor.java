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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.go.plugin.GoModFileFinder;
import org.sonar.plugins.go.api.checks.GoModFileData;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.go.plugin.GoModFileAnalyzer.analyzeGoModFileContent;

public class GoCoverSensor implements Sensor {

  private static final Version TELEMETRY_SUPPORTED_API_VERSION = Version.create(10, 9);

  public static final String REPORT_PATH_KEY = "sonar.go.coverage.reportPaths";
  private static final Logger LOG = LoggerFactory.getLogger(GoCoverSensor.class);
  private static final String GO_LANGUAGE_KEY = "go";
  private static final int COVERAGE_SAVE_BATCH_SIZE = 10_000;

  // See ParseProfiles function:
  // https://github.com/golang/go/blob/master/src/cmd/cover/profile.go
  static final Pattern MODE_LINE_REGEXP = Pattern.compile("^mode: (\\w+)$");
  private static final int MAX_FILES_WALK_DEPTH = 999;

  private GoCoverageStorage coverageStorage = new GoCoverageStorageImpl();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(GO_LANGUAGE_KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PATH_KEY))
      .name("Go Cover sensor for Go coverage");
  }

  @Override
  public void execute(SensorContext context) {
    execute(context, GoPathContext.DEFAULT);
  }

  void execute(SensorContext context, GoPathContext goContext) {
    try {
      getReportPaths(context).forEach(reportPath -> parseAndSave(reportPath, context, goContext));
    } catch (Exception e) {
      LOG.warn("Coverage import failed: {}", e.getMessage(), e);
    }
  }

  static Stream<Path> getReportPaths(SensorContext sensorContext) {
    Configuration config = sensorContext.config();
    Path baseDir = sensorContext.fileSystem().baseDir().toPath();
    String[] reportPaths = config.getStringArray(REPORT_PATH_KEY);
    return Arrays.stream(reportPaths).flatMap(reportPath -> isWildcard(reportPath)
      ? getPatternPaths(baseDir, reportPath)
      : getRegularPath(baseDir, reportPath));
  }

  private static Stream<Path> getPatternPaths(Path baseDir, String reportPath) {
    try (Stream<Path> paths = Files.walk(baseDir, MAX_FILES_WALK_DEPTH)) {
      return findMatchingPaths(baseDir, reportPath, paths);

    } catch (IOException e) {
      LOG.warn("Failed finding coverage files using pattern {}", reportPath);
      return Stream.empty();
    }
  }

  private static Stream<Path> getRegularPath(Path baseDir, String reportPath) {
    Path path = Paths.get(reportPath);
    if (!path.isAbsolute()) {
      path = baseDir.resolve(path);
    }
    if (path.toFile().exists()) {
      return Stream.of(path);
    }

    LOG.warn("Coverage report can't be loaded, report file not found, ignoring this file {}.", reportPath);
    return Stream.empty();
  }

  private static boolean isWildcard(String path) {
    return path.contains("*") || path.contains("?");
  }

  private static String toUnixLikePath(String path) {
    return path.replace('\\', '/');
  }

  private static Stream<Path> findMatchingPaths(Path baseDir, String reportPath, Stream<Path> paths) {
    WildcardPattern globPattern = WildcardPattern.create(toUnixLikePath(reportPath));

    List<Path> matchingPaths = paths
      .filter(currentPath -> {
        Path normalizedPath = baseDir.toAbsolutePath().relativize(currentPath.toAbsolutePath());
        String pathToMatch = toUnixLikePath(normalizedPath.toString());
        return globPattern.match(pathToMatch);
      }).toList();

    if (matchingPaths.isEmpty()) {
      LOG.warn("Coverage report can't be loaded, file(s) not found for pattern: '{}', ignoring this file.", reportPath);
    }

    return matchingPaths.stream();
  }

  void parseAndSave(Path reportPath, SensorContext context, GoPathContext goContext) {
    LOG.info("Load coverage report from '{}'", reportPath);
    var statistics = new FileResolutionStatistics();
    try (InputStream input = new FileInputStream(reportPath.toFile())) {
      var goModFileData = findAllGoModFileData(context);

      Coverage coverage = new Coverage(goContext);
      Scanner scanner = new Scanner(input, UTF_8);
      if (!scanner.hasNextLine() || !MODE_LINE_REGEXP.matcher(scanner.nextLine()).matches()) {
        throw new IOException("Invalid go coverage, expect 'mode:' on the first line.");
      }
      int lineNumber = 2;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (!line.isEmpty()) {
          addIfValidLine(line, lineNumber, coverage);
        }
        lineNumber++;
        if (lineNumber % COVERAGE_SAVE_BATCH_SIZE == 0) {
          LOG.debug("Save {} lines from coverage report '{}'", lineNumber, reportPath);
          coverageStorage.saveCoverage(context, coverage, goModFileData, reportPath, statistics);
          coverage = new Coverage(goContext);
        }
      }
      coverageStorage.saveCoverage(context, coverage, goModFileData, reportPath, statistics);
    } catch (IOException e) {
      LOG.warn("Failed parsing coverage info for file {}: {}", reportPath, e.getMessage());
    }
    saveFileResolutionStatistic(context, statistics);
  }

  private static Set<GoModFileData> findAllGoModFileData(SensorContext context) throws IOException {
    var goModFiles = GoModFileFinder.findGoModFiles(context);
    Set<GoModFileData> results = new HashSet<>();
    for (InputFile goModFile : goModFiles) {
      results.add(analyzeGoModFileContent(goModFile.contents(), goModFile.toString()));
    }
    return results;
  }

  private static void addIfValidLine(String line, int lineNumber, Coverage coverage) {
    try {
      coverage.add(CoverageStat.parseLine(lineNumber, line));
    } catch (IllegalArgumentException e) {
      LOG.debug("Ignoring line in coverage report: {}.", e.getMessage(), e);
    }
  }

  private static void saveFileResolutionStatistic(SensorContext context, FileResolutionStatistics statistics) {
    var isTelemetrySupported = context.runtime().getApiVersion().isGreaterThanOrEqual(TELEMETRY_SUPPORTED_API_VERSION);
    if (isTelemetrySupported) {
      context.addTelemetryProperty("go.coverage_absolute_path", Integer.toString(statistics.absolutePath()));
      context.addTelemetryProperty("go.coverage_relative_no_module_in_go_mod_dir", Integer.toString(statistics.relativeNoModuleInGoModDir()));
      context.addTelemetryProperty("go.coverage_absolute_no_module_in_report_path", Integer.toString(statistics.absoluteNoModuleInReportPath()));
      context.addTelemetryProperty("go.coverage_relative_path", Integer.toString(statistics.relativePath()));
      context.addTelemetryProperty("go.coverage_relative_no_module_in_report_path", Integer.toString(statistics.relativeNoModuleInReportPath()));
      context.addTelemetryProperty("go.coverage_relative_sub_paths", Integer.toString(statistics.relativeSubPaths()));
      context.addTelemetryProperty("go.coverage_unresolved", Integer.toString(statistics.unresolved()));
    }
  }

  // visible for tests
  void setCoverageStorage(GoCoverageStorage coverageStorage) {
    this.coverageStorage = coverageStorage;
  }
}
