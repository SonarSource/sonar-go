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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurationStatisticsTest {

  private SensorContextTester sensorContext = SensorContextTester.create(Paths.get("."));

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void statistics_disabled() {
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    fillStatistics(statistics);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }

  @Test
  void statistics_activated() {
    sensorContext.settings().setProperty("sonar.go.duration.statistics", "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    fillStatistics(statistics);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertThat(logTester.logs(Level.INFO).get(0)).startsWith("Duration Statistics, ");
  }

  @Test
  void statistics_format() {
    sensorContext.settings().setProperty("sonar.go.duration.statistics", "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    statistics.store("A", 12_000_000L);
    statistics.store("B", 15_000_000_000L);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertThat(logTester.logs(Level.INFO).get(0)).isEqualTo("Duration Statistics, B 15'000 ms, A 12 ms");
  }

  @Test
  void statistics_notifies_tracer_when_enabled() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<String> notifiedIds = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> notifiedIds.add(id);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    fillStatistics(statistics);
    // Not "C": the BiConsumer overload (a check's per-node callback in production) never notifies the
    // tracer, since it can fire millions of times per run at sub-microsecond durations.
    assertThat(notifiedIds).containsExactlyInAnyOrder("A", "B");
  }

  @Test
  void statistics_does_not_notify_tracer_when_disabled() {
    List<String> notifiedIds = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> notifiedIds.add(id);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    fillStatistics(statistics);
    assertThat(notifiedIds).isEmpty();
  }

  @Test
  void statistics_passes_detail_through_to_tracer() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<String> details = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> details.add(detail);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    statistics.time("A", () -> "some/file.go", () -> {
    });
    statistics.time("B", (Supplier<Void>) () -> null);
    assertThat(details).containsExactly("some/file.go", null);
  }

  @Test
  void statistics_aggregates_nested_biconsumer_calls_into_the_enclosing_breakdown() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<Map<String, Long>> breakdowns = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> breakdowns.add(breakdown);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    BiConsumer<String, String> ruleA = statistics.time("RuleA", (t, u) -> {
    });
    BiConsumer<String, String> ruleB = statistics.time("RuleB", (t, u) -> {
    });
    statistics.time("ChecksVisitor", () -> "some/file.go", () -> {
      ruleA.accept("x", "y");
      ruleA.accept("x", "y");
      ruleB.accept("x", "y");
    });
    assertThat(breakdowns).hasSize(1);
    assertThat(breakdowns.get(0)).containsOnlyKeys("RuleA", "RuleB");
  }

  @Test
  void statistics_pops_breakdown_and_notifies_tracer_when_timed_block_throws() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<String> notifiedIds = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> notifiedIds.add(id);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    RuntimeException thrown = new RuntimeException("boom");

    assertThatThrownBy(() -> statistics.time("A", (Supplier<Void>) () -> {
      throw thrown;
    })).isSameAs(thrown);
    assertThat(notifiedIds).containsExactly("A");

    // A later call must not merge into a breakdown map leaked by the earlier exception.
    statistics.time("B", (Supplier<Void>) () -> null);
    assertThat(notifiedIds).containsExactly("A", "B");
  }

  @Test
  void statistics_breakdown_is_empty_when_nothing_ran_nested() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<Map<String, Long>> breakdowns = new ArrayList<>();
    DurationTracer tracer = (id, detail, startNanos, durationNanos, breakdown) -> breakdowns.add(breakdown);
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(), tracer);
    statistics.time("Parse", () -> {
    });
    assertThat(breakdowns).containsExactly(Map.of());
  }

  @Test
  void statistics_does_not_resolve_the_detail_when_no_tracer_is_installed() {
    // The property being on is the normal "support asked me to measure a slow scan" case, and the
    // tracer is NOOP there. Building a detail string nothing will read is pure waste per directory.
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    AtomicInteger resolved = new AtomicInteger();

    statistics.time("Parse", () -> {
      resolved.incrementAndGet();
      return "some/file.go";
    }, () -> {
    });

    assertThat(resolved).hasValue(0);
  }

  @Test
  void statistics_resolves_the_detail_once_when_a_tracer_is_installed() {
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    List<String> details = new ArrayList<>();
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(),
      (id, detail, startNanos, durationNanos, breakdown) -> details.add(detail));
    AtomicInteger resolved = new AtomicInteger();

    statistics.time("Parse", () -> {
      resolved.incrementAndGet();
      return "some/file.go";
    }, () -> {
    });

    assertThat(resolved).hasValue(1);
    assertThat(details).containsExactly("some/file.go");
  }

  @Test
  void statistics_still_accumulates_without_a_tracer() {
    // The NOOP path skips the breakdown bookkeeping entirely, so guard that it still does the one job
    // duration statistics had before tracing existed.
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());

    fillStatistics(statistics);
    statistics.log();

    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertThat(logTester.logs(Level.INFO).get(0)).contains("A ", "B ", "C ");
  }

  @Test
  void statistics_times_the_region_before_resolving_the_detail() {
    // `Parse`'s detail sorts and joins every filename in the batch. Billing that to the span would make its
    // duration in the trace disagree with — and always exceed — what the same region contributed to `stats`.
    sensorContext.settings().setProperty(DurationStatistics.DURATION_STATISTICS_PROPERTY_KEY, "true");
    AtomicLong spanEnd = new AtomicLong();
    DurationStatistics statistics = new DurationStatistics(sensorContext.config(),
      (id, detail, startNanos, durationNanos, breakdown) -> spanEnd.set(startNanos + durationNanos));
    AtomicLong detailResolvedAt = new AtomicLong();

    statistics.time("Parse", () -> {
      pause();
      detailResolvedAt.set(System.nanoTime());
      return "some/file.go";
    }, () -> {
    });

    assertThat(spanEnd.get()).isLessThan(detailResolvedAt.get());
  }

  /**
   * Burns long enough on the clock the span is measured against that resolving the detail would be
   * unmistakable in the span's duration if it were counted. Spins rather than sleeps so the test does not
   * depend on how promptly the scheduler hands the thread back.
   */
  private static void pause() {
    long until = System.nanoTime() + 5_000_000L;
    while (System.nanoTime() < until) {
      Thread.onSpinWait();
    }
  }

  private void fillStatistics(DurationStatistics statistics) {
    StringBuilder txt = new StringBuilder();
    statistics.time("A", () -> txt.append("1")).append(2);
    statistics.time("B", () -> {
      txt.append("3");
    });
    statistics
      .time("C", (t, u) -> txt.append(t).append(u))
      .accept("4", "5");
    assertThat(txt).hasToString("12345");
  }
}
