/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONArray;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONObject;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.JSONParser;

public class SFDXCoverageSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(SFDXCoverageSensor.class);

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Test coverage Sensor for Apex")
      .onlyOnLanguage(ApexPlugin.APEX_LANGUAGE_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    Optional<String> reportPath = context.config().get(ApexPlugin.REPORT_PATH_KEY);
    if (!reportPath.isPresent()) {
      return;
    }

    String trimmedPath = reportPath.get().trim();
    String reportContent = getReportContent(context, trimmedPath);
    if (reportContent == null) {
      return;
    }

    try {
      JSONArray parseResult = (JSONArray) new JSONParser().parse(reportContent);
      saveCoverage(context, parseResult);
    } catch (Exception e) {
      LOG.error("Cannot read coverage report file, expecting standard SFDX test coverage result in JSON format: '{}'", trimmedPath, e);
    }
  }

  private static void saveCoverage(SensorContext context, JSONArray coverages) {
    FileSystem fileSystem = context.fileSystem();
    FilePredicates predicates = fileSystem.predicates();

    for (Object coverage : coverages) {
      JSONObject jsonObject = (JSONObject) coverage;
      String filename = (String) jsonObject.get("name");
      try {
        InputFile inputFile = fileSystem.inputFile(predicates.or(
          predicates.hasFilename(filename + ".cls"),
          predicates.hasFilename(filename + ".trigger")));
        if (inputFile != null) {
          saveNewCoverage(context, (Map<String, Long>) jsonObject.get("lines"), inputFile);
        } else {
          LOG.warn("File '{}' is present in coverage report but cannot be found in filesystem", filename);
        }
      } catch (IllegalArgumentException e) {
        LOG.error("More than one file found with name: '{}'", filename, e);
      } catch (IllegalStateException e) {
        LOG.error("Invalid coverage information on file: '{}'", filename, e);
      }
    }
  }

  private static void saveNewCoverage(SensorContext context, Map<String, Long> hitsPerLines, InputFile inputFile) {
    NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
    for (Entry<String, Long> hitsPerLine : hitsPerLines.entrySet()) {
      int line = Integer.parseInt(hitsPerLine.getKey());
      newCoverage.lineHits(line, hitsPerLine.getValue().intValue());
    }
    newCoverage.save();
  }

  @CheckForNull
  private static String getReportContent(SensorContext context, String reportPath) {
    String reportContent = null;
    try {
      reportContent = fileContent(context.fileSystem(), reportPath);
      if (reportContent == null) {
        LOG.error("SFDX coverage report not found: '{}'", reportPath);
      }
    } catch (IOException e) {
      LOG.error("Error reading coverage report: '{}'", reportPath, e);
    }
    return reportContent;
  }

  @CheckForNull
  private static String fileContent(FileSystem fs, String reportPath) throws IOException {
    InputFile report = fs.inputFile(fs.predicates().hasPath(reportPath));
    if (report != null && report.isFile()) {
      return report.contents();
    }
    File reportFile = fs.resolvePath(reportPath);
    if (reportFile.isFile()) {
      return new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
    }
    return null;
  }

}
