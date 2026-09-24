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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonar.go.testing.TestGoConverterSingleFile;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterTracingTest {

  private final List<String> names = new ArrayList<>();
  private final List<Object[]> argsPerSpan = new ArrayList<>();
  private final List<Long> durations = new ArrayList<>();

  private final ConverterTracer recording = (name, startNanos, durationNanos, args) -> {
    names.add(name);
    argsPerSpan.add(args);
    durations.add(durationNanos);
  };

  @AfterEach
  void tearDown() {
    // Static state: leaving a tracer installed would leak into every later test in this JVM.
    ConverterTracing.uninstall();
  }

  @Test
  void should_not_record_anything_while_no_tracer_is_installed() {
    try (var span = ConverterTracing.span("spawn", "files", 2)) {
      span.arg("pid", 1234L);
    }

    assertThat(names).isEmpty();
  }

  @Test
  void should_share_one_stateless_span_while_disabled() {
    // The untraced path is every real analysis, so it must not allocate per region.
    try (var first = ConverterTracing.span("spawn"); var second = ConverterTracing.span("write.stdin")) {
      assertThat(first).isSameAs(second);
      assertThat(first.arg("bytes", 1)).isSameAs(first);
    }
  }

  @Test
  void should_report_name_args_and_duration_once_installed() {
    ConverterTracing.install(recording);

    try (var span = ConverterTracing.span("write.stdin", "files", 2)) {
      span.arg("bytes", 4096L);
    }

    assertThat(names).containsExactly("write.stdin");
    assertThat(argsPerSpan.get(0)).containsExactly("files", 2, "bytes", 4096L);
    assertThat(durations.get(0)).isNotNegative();
  }

  @Test
  void should_report_a_span_that_carries_no_args() {
    ConverterTracing.install(recording);

    ConverterTracing.span("waitFor").close();

    assertThat(names).containsExactly("waitFor");
    assertThat(argsPerSpan.get(0)).isEmpty();
  }

  @Test
  void should_stop_reporting_after_uninstall() {
    ConverterTracing.install(recording);
    ConverterTracing.span("spawn").close();
    ConverterTracing.uninstall();

    ConverterTracing.span("spawn").close();

    assertThat(names).containsExactly("spawn");
  }

  @Test
  void should_report_every_leg_of_a_real_round_trip_in_order() {
    ConverterTracing.install(recording);

    TestGoConverterSingleFile.parse("package main\nfunc foo() {return 42}");

    // The order is the round trip itself: build the payload, start the process, feed it, read it back,
    // reap it, deserialize. A regression that moved work between legs would reorder or drop one.
    assertThat(names).containsExactly("batch.encode", "spawn", "write.stdin", "drain.stdout", "waitFor", "tree.decode");
    assertThat(argOf("spawn", "pid")).isInstanceOf(Long.class);
    assertThat((Long) argOf("write.stdin", "bytes")).isPositive();
    assertThat((Integer) argOf("drain.stdout", "chars")).isPositive();
    assertThat(argOf("waitFor", "exitCode")).isEqualTo(0);
    assertThat(argOf("tree.decode", "trees")).isEqualTo(1);
  }

  private Object argOf(String spanName, String key) {
    var args = argsPerSpan.get(names.indexOf(spanName));
    int keyIndex = Arrays.asList(args).indexOf(key);
    assertThat(keyIndex).as("%s has no %s arg in %s", spanName, key, Arrays.toString(args)).isNotNegative();
    return args[keyIndex + 1];
  }
}
