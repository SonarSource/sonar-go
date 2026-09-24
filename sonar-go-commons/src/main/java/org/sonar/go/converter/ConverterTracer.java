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
package org.sonar.go.converter;

/**
 * Receives one completed timing region of the Go-process round trip: the spawn, the stdin write, the
 * stdout drain, the exit wait, the JSON decode. Lets a benchmark see where the wall time between
 * "the sensor asked for a parse" and "the sensor got trees back" actually goes, without the benchmark
 * having to reimplement the round trip to measure it.
 *
 * <p>Defaults to {@link #NOOP} and nothing in a shipped analysis installs anything else;
 * {@link ConverterTracing} skips all the bookkeeping while none is installed, so an ordinary scan pays
 * one volatile read and one reference comparison per region.
 *
 * <p>The span names are the ones {@code CorpusBenchmark} already records for the same regions, so a
 * full-sensor trace and a corpus trace can be read, and aggregated by {@code tracetool}, as one
 * vocabulary rather than two.
 */
@FunctionalInterface
public interface ConverterTracer {

  ConverterTracer NOOP = (name, startNanos, durationNanos, args) -> {
    // Nobody is listening — see the class javadoc.
  };

  /**
   * @param startNanos the {@code System.nanoTime()} reading taken when the region opened, so a recorder
   *   can place the span on a timeline shared with regions timed elsewhere in the same JVM
   * @param args alternating key/value pairs describing what the region did, e.g. {@code "bytes", 4096};
   *   every one of them is cheap to produce, because the call sites build them before knowing whether
   *   anything will read them
   */
  void onSpanCompleted(String name, long startNanos, long durationNanos, Object... args);
}
