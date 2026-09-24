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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

public class DurationStatistics {

  public static final String DURATION_STATISTICS_PROPERTY_KEY = "sonar.go.duration.statistics";

  private static final Logger LOG = LoggerFactory.getLogger(DurationStatistics.class);

  /** Sentinel for "the timed block threw, so it never reached the point where its duration is taken". */
  private static final long NOT_MEASURED = -1L;

  private final Map<String, AtomicLong> stats = new ConcurrentHashMap<>();

  private final boolean recordStat;
  private final DurationTracer tracer;
  /**
   * Whether anything is actually listening. A normal analysis with {@code sonar.go.duration.statistics=true}
   * — which is what support asks for on a slow scan — leaves the tracer at {@link DurationTracer#NOOP}, and
   * the breakdown bookkeeping below would then allocate a map per timed region and box a Long per check
   * callback only to throw it all away, skewing the very measurement being collected.
   */
  private final boolean tracing;
  /**
   * The go pipeline is single-threaded, so at most one traced call is ever open at a time; a check's
   * untraced per-node calls (see {@link #time(String, BiConsumer)}) merge into whichever traced call is
   * currently on top, e.g. the {@code ChecksVisitor} pass for the file they're running against — giving
   * that span a per-rule breakdown without emitting a separate event per node.
   */
  private final Deque<Map<String, Long>> activeBreakdowns = new ArrayDeque<>();

  public DurationStatistics(Configuration config) {
    this(config, DurationTracer.NOOP);
  }

  public DurationStatistics(Configuration config, DurationTracer tracer) {
    recordStat = config.getBoolean(DURATION_STATISTICS_PROPERTY_KEY).orElse(false);
    this.tracer = tracer;
    this.tracing = tracer != DurationTracer.NOOP;
  }

  /**
   * Not traced: a check's wrapped consumer fires once per matching AST node, which can be millions of
   * calls at sub-microsecond durations for a hot rule — too fine-grained for Chrome trace event format's
   * microsecond resolution, and far too many events to be a usable trace. Still accumulated into
   * {@code stats} as before, so the per-rule breakdown in {@link #log()} is unaffected.
   */
  <C, T> BiConsumer<C, T> time(String id, BiConsumer<C, T> consumer) {
    if (recordStat) {
      return (t, u) -> time(id, null, () -> {
        consumer.accept(t, u);
        return null;
      }, false);
    } else {
      return consumer;
    }
  }

  public void time(String id, Runnable runnable) {
    time(id, null, runnable);
  }

  /**
   * @param detail supplies what the region ran on, e.g. the file being visited — passed to a configured
   * {@link DurationTracer} as trace-span context. Resolved only when a tracer is actually installed, so a
   * normal analysis never pays for building a string nothing reads, and only once the region has been
   * timed, so the cost of building it lands on nobody's span; no effect on the {@code stats} key.
   */
  public void time(String id, @Nullable Supplier<String> detail, Runnable runnable) {
    if (recordStat) {
      time(id, detail, () -> {
        runnable.run();
        return null;
      }, true);
    } else {
      runnable.run();
    }
  }

  <T> T time(String id, Supplier<T> supplier) {
    return time(id, null, supplier, true);
  }

  <T> T time(String id, @Nullable Supplier<String> detail, Supplier<T> supplier) {
    return time(id, detail, supplier, true);
  }

  private <T> T time(String id, @Nullable Supplier<String> detail, Supplier<T> supplier, boolean notifyTracer) {
    if (!recordStat) {
      return supplier.get();
    }
    if (!tracing) {
      // Exactly the pre-tracing path, down to leaving `stats` untouched when the block throws.
      long startTime = System.nanoTime();
      T result = supplier.get();
      store(id, System.nanoTime() - startTime);
      return result;
    }
    if (notifyTracer) {
      activeBreakdowns.push(new LinkedHashMap<>());
    }
    long startTime = System.nanoTime();
    long elapsed = NOT_MEASURED;
    try {
      T result = supplier.get();
      elapsed = System.nanoTime() - startTime;
      store(id, elapsed);
      if (!notifyTracer && !activeBreakdowns.isEmpty()) {
        activeBreakdowns.peek().merge(id, elapsed, Long::sum);
      }
      return result;
    } finally {
      if (notifyTracer) {
        // The region is timed before the detail supplier runs: resolving it is the tracer's own cost, not
        // the analysis', and for `Parse` it sorts and joins every filename in the batch. Reusing `elapsed`
        // rather than re-reading the clock also keeps the span's duration equal to what `store` accumulated
        // for the same region; only a block that threw never got that far and has to be timed here.
        long duration = elapsed == NOT_MEASURED ? (System.nanoTime() - startTime) : elapsed;
        var resolved = detail == null ? null : detail.get();
        tracer.onSpanCompleted(id, resolved, startTime, duration, activeBreakdowns.pop());
      }
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
