/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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
package org.sonar.go.plugin;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemoryMonitorTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final Pattern MEMORY_RECORD_PATTERN = Pattern.compile("[a-zA-Z\\s]+:\\s[\\d']+MB, [\\d']+MB");
  private static final Pattern PEAK_MEMORY_PATTERN = Pattern.compile("Sensor peak memory:\\s[\\d']+MB");

  private MemoryMonitor memoryMonitor;

  @BeforeEach
  void setUp() {
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty("sonar.go.duration.statistics", "true");
    memoryMonitor = new MemoryMonitor(mapSettings.asConfig());
  }

  @Test
  void shouldLogAvailableMemory() {
    memoryMonitor.logAvailableMemory(ManagementFactory.getOperatingSystemMXBean());
    assertThat(logTester.logs()).hasSize(1)
      .anyMatch(s -> s.startsWith("Total system memory: "));
  }

  @Test
  void shouldFailToLogAvailableMemory() {
    OperatingSystemMXBean operatingSystemMXBean = mock();
    memoryMonitor.logAvailableMemory(operatingSystemMXBean);
    assertThat(logTester.logs()).hasSize(1)
      .anyMatch(s -> s.startsWith("Could not get total system memory: "));
  }

  @Test
  void shouldNotLogWhenNotEnabled() {
    memoryMonitor = new MemoryMonitor(new MapSettings().asConfig());
    memoryMonitor.logAvailableMemory(ManagementFactory.getOperatingSystemMXBean());
    memoryMonitor.logMemory();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldLogMemoryCorrectly() {
    memoryMonitor.addRecord("test");
    memoryMonitor.addRecord("After the Go analysis");
    memoryMonitor.addRecord("End of the sensor");
    memoryMonitor.logMemory();

    List<String> logs = logTester.logs();
    assertThat(logs).hasSize(2);

    String[] memoryRecordLog = logs.get(0).split(System.lineSeparator());
    assertThat(memoryRecordLog).hasSize(7);
    assertThat(memoryRecordLog[0]).isEqualTo("Go memory statistics (used, peak):");
    assertThat(memoryRecordLog[1]).startsWith("Initial memory: ").matches(MEMORY_RECORD_PATTERN);
    assertThat(memoryRecordLog[2]).startsWith("test: ").matches(MEMORY_RECORD_PATTERN);
    assertThat(memoryRecordLog[3]).startsWith("After the Go analysis: ").matches(MEMORY_RECORD_PATTERN);
    assertThat(memoryRecordLog[4]).startsWith("End of the sensor: ").matches(MEMORY_RECORD_PATTERN);
    assertThat(memoryRecordLog[5]).matches(PEAK_MEMORY_PATTERN);
    assertThat(memoryRecordLog[6]).isEqualTo("Note that these values may not be accurate due to garbage collection; they should only be used to detect significant outliers.");

    assertThat(logs.get(1)).startsWith("Total system memory: ");
  }

  @Test
  void shouldFormatCorrectlyOnHighValues() {
    memoryMonitor.memoryRecords.add(new MemoryMonitor.MemoryRecord("highUsage", 123456789L, 9876543210L));
    memoryMonitor.logMemory();

    List<String> logs = logTester.logs();
    assertThat(logs).hasSize(2);

    String[] memoryRecordLog = logs.get(0).split(System.lineSeparator());
    assertThat(memoryRecordLog).hasSize(5);
    assertThat(memoryRecordLog[0]).isEqualTo("Go memory statistics (used, peak):");
    assertThat(memoryRecordLog[1]).startsWith("Initial memory: ").matches(MEMORY_RECORD_PATTERN);
    assertThat(memoryRecordLog[2]).isEqualTo("highUsage: 123'456'789MB, 9'876'543'210MB");
    assertThat(memoryRecordLog[3]).matches(PEAK_MEMORY_PATTERN);
    assertThat(memoryRecordLog[4]).isEqualTo("Note that these values may not be accurate due to garbage collection; they should only be used to detect significant outliers.");

    assertThat(logs.get(1)).startsWith("Total system memory: ");
  }
}
