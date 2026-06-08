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

  private final Set<GoVersion> accumulatedGoVersions = new HashSet<>();
  private final FileResolutionStatistics accumulatedCoverageStatistics = new FileResolutionStatistics();
  private boolean hasCoverageData = false;
  private int parseFailuresCount;

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
    sendParseFailuresCountTelemetry(context);
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
    context.addTelemetryProperty("go.used_version", usedVersion);
  }

  private void sendParseFailuresCountTelemetry(SensorContext context) {
    context.addTelemetryProperty("go.parse_failures_count", Integer.toString(parseFailuresCount));
  }

  private void sendCoverageTelemetry(SensorContext context) {
    context.addTelemetryProperty("go.coverage_absolute_path", Integer.toString(accumulatedCoverageStatistics.absolutePath()));
    context.addTelemetryProperty("go.coverage_relative_no_module_in_go_mod_dir", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInGoModDir()));
    context.addTelemetryProperty("go.coverage_absolute_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.absoluteNoModuleInReportPath()));
    context.addTelemetryProperty("go.coverage_relative_path", Integer.toString(accumulatedCoverageStatistics.relativePath()));
    context.addTelemetryProperty("go.coverage_relative_no_module_in_report_path", Integer.toString(accumulatedCoverageStatistics.relativeNoModuleInReportPath()));
    context.addTelemetryProperty("go.coverage_relative_sub_paths", Integer.toString(accumulatedCoverageStatistics.relativeSubPaths()));
    context.addTelemetryProperty("go.coverage_unresolved", Integer.toString(accumulatedCoverageStatistics.unresolved()));
  }
}
