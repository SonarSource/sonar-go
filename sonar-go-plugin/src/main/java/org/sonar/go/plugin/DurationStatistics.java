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
package org.sonar.go.plugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

public class DurationStatistics {

  private static final Logger LOG = LoggerFactory.getLogger(DurationStatistics.class);

  private static final String PROPERTY_KEY = "sonar.go.duration.statistics";

  private final Map<String, AtomicLong> stats = new ConcurrentHashMap<>();

  private final boolean recordStat;

  public DurationStatistics(Configuration config) {
    recordStat = config.getBoolean(PROPERTY_KEY).orElse(false);
  }

  <C, T> BiConsumer<C, T> time(String id, BiConsumer<C, T> consumer) {
    if (recordStat) {
      return (t, u) -> time(id, () -> consumer.accept(t, u));
    } else {
      return consumer;
    }
  }

  public void time(String id, Runnable runnable) {
    if (recordStat) {
      time(id, () -> {
        runnable.run();
        return null;
      });
    } else {
      runnable.run();
    }
  }

  <T> T time(String id, Supplier<T> supplier) {
    if (recordStat) {
      long startTime = System.nanoTime();
      T result = supplier.get();
      store(id, System.nanoTime() - startTime);
      return result;
    } else {
      return supplier.get();
    }
  }

  void store(String id, long elapsedTime) {
    stats.computeIfAbsent(id, key -> new AtomicLong(0)).addAndGet(elapsedTime);
  }

  void log() {
    if (recordStat) {
      StringBuilder out = new StringBuilder();
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
      symbols.setGroupingSeparator('\'');
      NumberFormat format = new DecimalFormat("#,###", symbols);
      out.append("Duration Statistics");
      stats.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
        .forEach(e -> out.append(", ")
          .append(e.getKey())
          .append(" ")
          .append(format.format(e.getValue().get() / 1_000_000L))
          .append(" ms"));
      LOG.info("{}", out);
    }
  }

}
