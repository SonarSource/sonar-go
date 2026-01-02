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
package org.sonar.go.plugin;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

/**
 * This class is used to monitor the java memory usage of the Go analyzer.
 * It logs the overall statistics at the end to give a rough idea of the magnitude and proportion of memory usage.
 * Measurements are only enabled by setting with the ˙sonar.go.duration.statistics` property.
 */
public final class MemoryMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryMonitor.class);
  private static final String PROPERTY_KEY = "sonar.go.duration.statistics";
  private final boolean recordingEnabled;
  // Visible for testing
  final List<MemoryRecord> memoryRecords = new ArrayList<>();
  private long overallPeak;

  private final NumberFormat format;

  public MemoryMonitor(Configuration config) {
    recordingEnabled = config.getBoolean(PROPERTY_KEY).orElse(false) && LOG.isInfoEnabled();
    resetPeak();
    addRecord("Initial memory");

    var symbols = new DecimalFormatSymbols(Locale.ROOT);
    symbols.setGroupingSeparator('\'');
    this.format = new DecimalFormat("#,###", symbols);
  }

  public void addRecord(String name) {
    if (recordingEnabled) {
      memoryRecords.add(new MemoryRecord(name, getMemoryUsedInMB(), getPeakMemoryUsedInMB()));
      resetPeak();
    }
  }

  public void logMemory() {
    if (!recordingEnabled) {
      return;
    }
    var sb = new StringBuilder();
    sb.append("Go memory statistics (used, peak):");
    sb.append(System.lineSeparator());

    for (MemoryRecord memoryRecord : memoryRecords) {
      sb.append(memoryRecord.toFormattedString(format))
        .append(System.lineSeparator());
    }
    sb.append("Sensor peak memory: ");
    sb.append(format.format(overallPeak));
    sb.append("MB");
    sb.append(System.lineSeparator());
    sb.append("Note that these values may not be accurate due to garbage collection; they should only be used to detect significant outliers.");
    LOG.info(sb.toString());
    logAvailableMemory(ManagementFactory.getOperatingSystemMXBean());
  }

  private static long toMB(long value) {
    // Integral division because we don't need to track fractions of MB
    return value / (1024 * 1024);
  }

  private static long getAvailableSystemMemoryInMB(java.lang.management.OperatingSystemMXBean operatingSystemMXBean) throws NoSuchMethodError, ClassCastException {
    OperatingSystemMXBean os = (OperatingSystemMXBean) operatingSystemMXBean;
    return toMB(os.getTotalMemorySize());
  }

  private static long getAvailableRuntimeMemory() {
    return toMB(Runtime.getRuntime().maxMemory());
  }

  private String getAvailableRuntimeMemoryInMB() {
    long runtimeMemory = getAvailableRuntimeMemory();
    if (runtimeMemory == Long.MAX_VALUE) {
      // We don't expect that we'll ever run into this case, however the api specifies that it can return Long.MAX_VALUE
      return "unlimited";
    } else {
      return format.format(runtimeMemory) + "MB";
    }
  }

  void logAvailableMemory(java.lang.management.OperatingSystemMXBean operatingSystemMXBean) {
    if (!recordingEnabled) {
      return;
    }
    try {
      LOG.info("Total system memory: {}, available runtime memory: {}", format.format(getAvailableSystemMemoryInMB(operatingSystemMXBean)) + "MB", getAvailableRuntimeMemoryInMB());
    } catch (ClassCastException | NoSuchMethodError e) {
      LOG.info("Could not get total system memory: {}", e.getMessage());
    }
  }

  private static long getMemoryUsedInMB() {
    return toMB(ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .mapToLong(p -> p.getUsage().getUsed())
      .sum());
  }

  private long getPeakMemoryUsedInMB() {
    long currentPeak = toMB(ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .mapToLong(p -> p.getPeakUsage().getUsed())
      .sum());
    overallPeak = Math.max(overallPeak, currentPeak);
    return currentPeak;
  }

  private static void resetPeak() {
    ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .forEach(MemoryPoolMXBean::resetPeakUsage);
  }

  // Visible for testing
  record MemoryRecord(String name, long used, long peak) {
    public String toFormattedString(Format format) {
      return name + ": " + format.format(used) + "MB, " + format.format(peak) + "MB";
    }
  }
}
