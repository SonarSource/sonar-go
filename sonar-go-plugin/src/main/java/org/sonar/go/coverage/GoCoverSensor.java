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
package org.sonar.go.coverage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoCoverSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoCoverSensor.class);
  private static final String GO_LANGUAGE_KEY = "go";

  public static final String REPORT_PATH_KEY = "sonar.go.coverage.reportPaths";

  // See ParseProfiles function:
  // https://github.com/golang/go/blob/master/src/cmd/cover/profile.go
  static final Pattern MODE_LINE_REGEXP = Pattern.compile("^mode: (\\w+)$");
  static final Pattern COVERAGE_LINE_REGEXP = Pattern.compile("^(.+):(\\d+)\\.(\\d+),(\\d+)\\.(\\d+) (\\d+) (\\d+)$");

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
      Coverage coverage = new Coverage(goContext);
      getReportPaths(context).forEach(reportPath -> parse(reportPath, coverage));
      coverage.fileMap.forEach((filePath, coverageStats) -> {
        try {
          saveFileCoverage(context, filePath, coverageStats);
        } catch (Exception e) {
          LOG.warn("Failed saving coverage info for file: {}", filePath);
        }
      });
    } catch (Exception e) {
      LOG.warn("Coverage import failed: {}", e.getMessage(), e);
    }
  }

  private static void saveFileCoverage(SensorContext sensorContext, String filePath, List<CoverageStat> coverageStats) throws IOException {
    FileSystem fileSystem = sensorContext.fileSystem();
    InputFile inputFile = findInputFile(filePath, fileSystem);
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
   *  It is possible that absolutePath references a file that does not exist in the file system.
   *  It happens when go tests where executed on a different computer.
   *  Even when absolute path does not match a file of the project, this method try to find a valid
   *  mach using a shorter relative path.
   *  @see <a href="https://github.com/SonarSource/sonar-go/issues/218">sonar-go/issues/218</a>
   */
  private static InputFile findInputFile(String absolutePath, FileSystem fileSystem) {
    FilePredicates predicates = fileSystem.predicates();
    InputFile inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(absolutePath));
    if (inputFile != null) {
      return inputFile;
    }
    LOG.debug("Resolving file {} using relative path", absolutePath);
    Path path = Paths.get(absolutePath);
    inputFile = fileSystem.inputFile(predicates.hasRelativePath(path.toString()));
    while (inputFile == null && path.getNameCount() > 1) {
      path = path.subpath(1, path.getNameCount());
      inputFile = fileSystem.inputFile(predicates.hasRelativePath(path.toString()));
    }
    return inputFile;
  }

  static Stream<Path> getReportPaths(SensorContext sensorContext) {
    Configuration config = sensorContext.config();
    Path baseDir = sensorContext.fileSystem().baseDir().toPath();
    String[] reportPaths = config.getStringArray(REPORT_PATH_KEY);
    return Arrays.stream(reportPaths).flatMap(reportPath -> isWildcard(reportPath)
      ? getPatternPaths(baseDir, reportPath)
      : getRegularPath(baseDir, reportPath));
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

  private static Stream<Path> getPatternPaths(Path baseDir, String reportPath) {
    try (Stream<Path> paths = Files.walk(baseDir, 999)) {
      return findMatchingPaths(baseDir, reportPath, paths);

    } catch (IOException e) {
      LOG.warn("Failed finding coverage files using pattern {}", reportPath);
      return Stream.empty();
    }
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

  static void parse(Path reportPath, Coverage coverage) {
    LOG.info("Load coverage report from '{}'", reportPath);
    try (InputStream input = new FileInputStream(reportPath.toFile())) {
      Scanner scanner = new Scanner(input, UTF_8.name());
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
      }
    } catch (IOException e) {
      LOG.warn("Failed parsing coverage info for file {}: {}", reportPath, e.getMessage());
    }
  }

  private static void addIfValidLine(String line, int lineNumber, Coverage coverage) {
    try {
      coverage.add(new CoverageStat(lineNumber, line));
    } catch (IllegalArgumentException e) {
      LOG.debug("Ignoring line in coverage report: {}.", e.getMessage(), e);
    }
  }

  static class Coverage {
    final GoPathContext goContext;
    Map<String, List<CoverageStat>> fileMap = new HashMap<>();

    Coverage(GoPathContext goContext) {
      this.goContext = goContext;
    }

    void add(CoverageStat coverage) {
      fileMap
        .computeIfAbsent(goContext.resolve(coverage.filePath), key -> new ArrayList<>())
        .add(coverage);
    }
  }

  static class FileCoverage {
    Map<Integer, LineCoverage> lineMap = new HashMap<>();
    List<String> lines;

    public FileCoverage(List<CoverageStat> coverageStats, @Nullable List<String> lines) {
      this.lines = lines;
      coverageStats.forEach(this::add);
    }

    private void add(CoverageStat coverage) {
      int startLine = findStartIgnoringBrace(coverage);
      int endLine = findEndIgnoringBrace(coverage, startLine);
      for (int line = startLine; line <= endLine; line++) {
        if (!isEmpty(line - 1)) {
          lineMap.computeIfAbsent(line, key -> new LineCoverage())
            .add(coverage);
        }
      }
    }

    private boolean isEmpty(int line) {
      return lines != null &&
        lines.get(line).trim().isEmpty();
    }

    int findStartIgnoringBrace(CoverageStat coverage) {
      int line = coverage.startLine;
      int column = coverage.startCol;
      while (shouldIgnore(line, column)) {
        column++;
        if (column > lines.get(line - 1).length()) {
          line++;
          column = 1;
        }
      }
      return line;
    }

    int findEndIgnoringBrace(CoverageStat coverage, int startLine) {
      int line = coverage.endLine;
      int column = coverage.endCol - 1;
      if (lines != null && line > lines.size()) {
        line = lines.size();
        column = lines.get(line - 1).length();
      }
      while (line > startLine && shouldIgnore(line, column)) {
        column--;
        if (column == 0) {
          line--;
          column = lines.get(line - 1).length();
        }
      }
      return line;
    }

    boolean shouldIgnore(int line, int column) {
      if (lines != null && line > 0 && line <= lines.size() && column > 0) {
        String currentLine = lines.get(line - 1);
        if (column > currentLine.length()) {
          // Ignore end of line
          return true;
        }
        int ch = currentLine.charAt(column - 1);
        return ch < ' ' || ch == '{' || ch == '}';
      }
      return false;
    }
  }

  static class LineCoverage {
    int hits = 0;

    void add(CoverageStat coverage) {
      long sum = ((long) hits) + coverage.count;
      if (sum > Integer.MAX_VALUE) {
        hits = Integer.MAX_VALUE;
      } else {
        hits = (int) sum;
      }
    }
  }

  static class CoverageStat {

    final String filePath;
    final int startLine;
    final int startCol;
    final int endLine;
    final int endCol;
    final int count;

    CoverageStat(int lineNumber, String line) {
      Matcher matcher = COVERAGE_LINE_REGEXP.matcher(line);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid go coverage at line " + lineNumber);
      }
      filePath = matcher.group(1);
      startLine = Integer.parseInt(matcher.group(2));
      startCol = Integer.parseInt(matcher.group(3));
      endLine = Integer.parseInt(matcher.group(4));
      endCol = Integer.parseInt(matcher.group(5));
      // No need to parse numStmt as it is never used.
      count = parseIntWithOverflow(matcher.group(7));
    }

    private static int parseIntWithOverflow(String s) {
      int result = 0;
      try {
        result = Integer.parseInt(s);
      } catch (NumberFormatException e) {
        // Thanks to the regex, we know that the input can only contain digits, the only possible Exception is therefore coming from overflow.
        return Integer.MAX_VALUE;
      }
      return result;
    }

  }

}
