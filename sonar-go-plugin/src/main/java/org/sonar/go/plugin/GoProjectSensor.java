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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
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
    boolean logTelemetry = context.config().getBoolean(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY).orElse(false);
    sendGoVersionTelemetry(context, logTelemetry);
    sendPluginVersionTelemetry(context, logTelemetry);
    sendFileCountTelemetry(context, logTelemetry);
    if (hasCoverageData) {
      sendCoverageTelemetry(context, logTelemetry);
    }
  }

  private void sendGoVersionTelemetry(SensorContext context, boolean logTelemetry) {
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
    sendTelemetryProperty(context, logTelemetry, "go.used_version", usedVersion);
  }

  private static void sendTelemetryProperty(SensorContext context, boolean logTelemetry, String property, String value) {
    context.addTelemetryProperty(property, value);
    if (logTelemetry) {
      LOG.debug("Telemetry property: {}={}", property, value);
    }
  }

  private static void sendPluginVersionTelemetry(SensorContext context, boolean logTelemetry) {
    sendTelemetryProperty(context, logTelemetry, "go.plugin_version", resolvePluginVersion());
  }

  public static String resolvePluginVersion() {
    try (InputStream is = GoProjectSensor.class.getClassLoader()
      .getResourceAsStream("org/sonar/plugins/go/pluginVersion.properties")) {
      if (is != null) {
        var props = new Properties();
        props.load(is);
        return props.getProperty("plugin.version", "unknown");
      }
    } catch (IOException e) {
      // fall through to fallback
    }
    return "unknown";
  }

  private void sendFileCountTelemetry(SensorContext context, boolean logTelemetry) {
    sendTelemetryProperty(context, logTelemetry, "go.processed_files_count", Integer.toString(filesProcessedCount));
    sendTelemetryProperty(context, logTelemetry, "go.parse_failures_count", Integer.toString(parseFailuresCount));
    sendTelemetryProperty(context, logTelemetry, "go.read_from_cache_files_count", Integer.toString(readFromCacheFilesCount));
  }

  private void sendCoverageTelemetry(SensorContext context, boolean logTelemetry) {
    sendTelemetryProperty(context, logTelemetry, "go.coverage_absolute_path", Integer.toString(accumulatedCoverageStatistics.absolutePath()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_relative_no_module_in_go_mod_dir", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInGoModDir()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_absolute_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.absoluteNoModuleInReportPath()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_relative_path", Integer.toString(accumulatedCoverageStatistics.relativePath()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_relative_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInReportPath()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_relative_sub_paths", Integer.toString(accumulatedCoverageStatistics.relativeSubPaths()));
    sendTelemetryProperty(context, logTelemetry, "go.coverage_unresolved", Integer.toString(accumulatedCoverageStatistics.unresolved()));
  }
}
