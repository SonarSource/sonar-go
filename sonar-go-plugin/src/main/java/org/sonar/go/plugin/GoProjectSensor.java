/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.go.coverage.FileResolutionStatistics;
import org.sonar.plugins.go.api.checks.GoVersion;

import static org.sonar.go.coverage.GoCoverSensor.TELEMETRY_SUPPORTED_API_VERSION;

/**
 * Only this sensor should store Telemetry. It implements {@link ProjectSensor} and it is executed only once for thw whole project.
 * Changing to {@link org.sonar.api.batch.sensor.Sensor} will cause that not all telemetry data will be saved.
 */
public class GoProjectSensor implements ProjectSensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoProjectSensor.class);

  private final Set<GoVersion> accumulatedGoVersions = new HashSet<>();
  private final FileResolutionStatistics accumulatedCoverageStatistics = new FileResolutionStatistics();
  private boolean hasCoverageData = false;
  private int readFromCacheFilesCount;
  private int parseFailuresCount;
  private int filesProcessedCount;

  public void addGoVersions(Set<GoVersion> versions) {
    accumulatedGoVersions.addAll(versions);
  }

  public void addCoverageStatistics(FileResolutionStatistics statistics) {
    accumulatedCoverageStatistics.accumulate(statistics);
    hasCoverageData = true;
  }

  public void increaseParseFailuresCount() {
    parseFailuresCount++;
  }

  public void increaseFilesProcessedCount(int increaseBy) {
    filesProcessedCount += increaseBy;
  }

  public void increaseReadFromCacheFilesCount(int increaseBy) {
    readFromCacheFilesCount += increaseBy;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(GoLanguage.KEY).name("GoProjectSensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (!context.runtime().getApiVersion().isGreaterThanOrEqual(TELEMETRY_SUPPORTED_API_VERSION)) {
      return;
    }
    sendGoVersionTelemetry(context);
    sendFileCountTelemetry(context);
    if (hasCoverageData) {
      sendCoverageTelemetry(context);
    }
  }

  private void sendGoVersionTelemetry(SensorContext context) {
    String usedVersion;
    if (accumulatedGoVersions.isEmpty()) {
      usedVersion = "noGoModFile";
    } else {
      usedVersion = accumulatedGoVersions.stream()
        .sorted()
        .map(GoVersion::toString)
        .distinct()
        .collect(Collectors.joining(";"));
    }
    sendTelemetryProperty(context, "go.used_version", usedVersion);
  }

  private static void sendTelemetryProperty(SensorContext context, String property, String value) {
    context.addTelemetryProperty(property, value);
    LOG.debug("Telemetry property: {}={}", property, value);
  }

  private void sendFileCountTelemetry(SensorContext context) {
    sendTelemetryProperty(context, "go.processed_files_count", Integer.toString(filesProcessedCount));
    sendTelemetryProperty(context, "go.parse_failures_count", Integer.toString(parseFailuresCount));
    sendTelemetryProperty(context, "go.read_from_cache_files_count", Integer.toString(readFromCacheFilesCount));
  }

  private void sendCoverageTelemetry(SensorContext context) {
    sendTelemetryProperty(context, "go.coverage_absolute_path", Integer.toString(accumulatedCoverageStatistics.absolutePath()));
    sendTelemetryProperty(context, "go.coverage_relative_no_module_in_go_mod_dir", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInGoModDir()));
    sendTelemetryProperty(context, "go.coverage_absolute_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.absoluteNoModuleInReportPath()));
    sendTelemetryProperty(context, "go.coverage_relative_path", Integer.toString(accumulatedCoverageStatistics.relativePath()));
    sendTelemetryProperty(context, "go.coverage_relative_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInReportPath()));
    sendTelemetryProperty(context, "go.coverage_relative_sub_paths", Integer.toString(accumulatedCoverageStatistics.relativeSubPaths()));
    sendTelemetryProperty(context, "go.coverage_unresolved", Integer.toString(accumulatedCoverageStatistics.unresolved()));
  }
}
