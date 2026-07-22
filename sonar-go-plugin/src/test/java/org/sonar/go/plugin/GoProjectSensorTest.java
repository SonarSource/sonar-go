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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;
import org.sonar.go.coverage.FileResolutionStatistics;
import org.sonar.plugins.go.api.checks.GoVersion;
import org.sonar.scanner.plugin.api.impl.internal.SonarRuntimeImpl;
import org.sonar.scanner.plugin.api.impl.sensor.DefaultSensorDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class GoProjectSensorTest {

  private static final SonarRuntime SQ_RUNTIME_SUPPORTING_TELEMETRY = SonarRuntimeImpl.forSonarQube(
    Version.create(10, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
  private static final SonarRuntime SQ_RUNTIME_NOT_SUPPORTING_TELEMETRY = SonarRuntimeImpl.forSonarQube(
    Version.create(10, 8), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);

  @Test
  void shouldDescribe() {
    var descriptor = new DefaultSensorDescriptor();
    new GoProjectSensor().describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("GoProjectSensor");
  }

  @ParameterizedTest
  @MethodSource
  void shouldSendGoVersionTelemetry(List<Set<GoVersion>> versionSets, String expectedValue) {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    versionSets.forEach(goProjectSensor::addGoVersions);
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.used_version", expectedValue);
  }

  static Stream<Arguments> shouldSendGoVersionTelemetry() {
    return Stream.of(
      Arguments.of(List.of(), "noGoModFile"),
      Arguments.of(List.of(Set.of(GoVersion.parse("1.21"))), "1.21"),
      Arguments.of(List.of(Set.of(GoVersion.parse("1.21")), Set.of(GoVersion.parse("1.23"))), "1.21;1.23"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldSendPluginVersionTelemetry(boolean propertiesLoadFails, String expectedValue) {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    try (MockedConstruction<Properties> ignored = propertiesLoadFails
      ? mockConstruction(Properties.class,
        (mock, mockContext) -> doThrow(new IOException("boom")).when(mock).load(any(InputStream.class)))
      : null) {
      new GoProjectSensor().execute(context);

      verify(context).addTelemetryProperty("go.plugin_version", expectedValue);
    }
  }

  static Stream<Arguments> shouldSendPluginVersionTelemetry() {
    return Stream.of(
      Arguments.of(false, "1.2.3-test"),
      Arguments.of(true, "unknown"));
  }

  @Test
  void shouldSendCoverageTelemetry() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var statistics = new FileResolutionStatistics();
    statistics.incrementRelativePath();
    statistics.incrementUnresolved();

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.addCoverageStatistics(statistics);
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.coverage_relative_path", "1");
    verify(context).addTelemetryProperty("go.coverage_unresolved", "1");
    verify(context).addTelemetryProperty("go.coverage_absolute_path", "0");
  }

  @Test
  void shouldAccumulateCoverageStatisticsFromMultipleModules() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var statisticsModule1 = new FileResolutionStatistics();
    statisticsModule1.incrementRelativePath();
    statisticsModule1.incrementRelativePath();

    var statisticsModule2 = new FileResolutionStatistics();
    statisticsModule2.incrementRelativePath();
    statisticsModule2.incrementUnresolved();

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.addCoverageStatistics(statisticsModule1);
    goProjectSensor.addCoverageStatistics(statisticsModule2);
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.coverage_relative_path", "3");
    verify(context).addTelemetryProperty("go.coverage_unresolved", "1");
  }

  @Test
  void shouldNotSendCoverageTelemetryWhenNoCoverageDataAdded() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.execute(context);

    // Coverage telemetry should not be sent when no coverage data was accumulated
    verify(context, never()).addTelemetryProperty(eq("go.coverage_absolute_path"), anyString());
    verify(context, never()).addTelemetryProperty(eq("go.coverage_relative_path"), anyString());
    verify(context, never()).addTelemetryProperty(eq("go.coverage_unresolved"), anyString());
  }

  @Test
  void shouldSendParseFailuresCountTelemetry() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.increaseParseFailuresCount();
    goProjectSensor.increaseParseFailuresCount();
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.parse_failures_count", "2");
  }

  @Test
  void shouldSendZeroParseFailuresCountWhenNoFailures() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.parse_failures_count", "0");
  }

  @Test
  void shouldSendFilesProcessedCountTelemetry() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.increaseFilesProcessedCount(2);
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.processed_files_count", "2");
  }

  @Test
  void shouldSendZeroFilesProcessedCountWhenNoFilesProcessed() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.processed_files_count", "0");
  }

  @ParameterizedTest
  @MethodSource
  void shouldSendReadFromCacheFilesCountTelemetry(List<Integer> increases, String expectedValue) {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    increases.forEach(goProjectSensor::increaseReadFromCacheFilesCount);
    goProjectSensor.execute(context);

    verify(context).addTelemetryProperty("go.read_from_cache_files_count", expectedValue);
  }

  static Stream<Arguments> shouldSendReadFromCacheFilesCountTelemetry() {
    return Stream.of(
      Arguments.of(List.of(), "0"),
      Arguments.of(List.of(3), "3"),
      Arguments.of(List.of(2, 5), "7"));
  }

  @Test
  void shouldNotSendTelemetryWhenApiVersionNotSupported() {
    var context = spy(SensorContextTester.create(Path.of(".")));
    context.setRuntime(SQ_RUNTIME_NOT_SUPPORTING_TELEMETRY);

    var goProjectSensor = new GoProjectSensor();
    goProjectSensor.addGoVersions(Set.of(GoVersion.parse("1.21")));
    goProjectSensor.execute(context);

    verify(context, never()).addTelemetryProperty(anyString(), anyString());
  }
}
