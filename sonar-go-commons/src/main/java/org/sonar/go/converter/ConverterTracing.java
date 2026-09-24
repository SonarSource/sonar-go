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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Installs the {@link ConverterTracer} that the Go round trip reports to, and hands out the spans it
 * reports through. Off unless a benchmark turns it on: while no tracer is installed, {@link #span}
 * returns a shared stateless instance and allocates nothing, so the untraced path — every real
 * analysis — costs one atomic read and a reference comparison per region.
 *
 * <p>A static holder rather than a constructor parameter because {@code GoConverter} is built deep
 * inside {@code InstanceScopeGoExecutable}, several layers below anything a benchmark can reach, and
 * threading a tracer down through the plugin's wiring would change production signatures for the
 * benefit of a harness.
 */
public final class ConverterTracing {

  private static final AtomicReference<ConverterTracer> INSTALLED_TRACER = new AtomicReference<>(ConverterTracer.NOOP);

  private ConverterTracing() {
  }

  public static void install(ConverterTracer tracer) {
    INSTALLED_TRACER.set(Objects.requireNonNull(tracer));
  }

  public static void uninstall() {
    INSTALLED_TRACER.set(ConverterTracer.NOOP);
  }

  /** Opens a timing region; close it with try-with-resources. {@code args} are alternating key/value pairs. */
  public static Span span(String name, Object... args) {
    var tracer = INSTALLED_TRACER.get();
    return tracer == ConverterTracer.NOOP ? DisabledSpan.INSTANCE : new RecordingSpan(tracer, name, args);
  }

  /** A timing region in progress. */
  public interface Span extends AutoCloseable {

    /** Records context that only became known while the region ran, such as the pid a spawn got. */
    Span arg(String key, Object value);

    /** Narrowed from {@link AutoCloseable}: closing a span reports it and cannot fail. */
    @Override
    void close();
  }

  /**
   * Shared by every untraced region, which is safe precisely because it holds no state: it has nothing
   * to keep, since nothing will be reported.
   */
  private enum DisabledSpan implements Span {
    INSTANCE;

    @Override
    public Span arg(String key, Object value) {
      return this;
    }

    @Override
    public void close() {
      // Nothing was measured, so there is nothing to report.
    }
  }

  private static final class RecordingSpan implements Span {

    private final long startNanos;
    private final ConverterTracer tracer;
    private final String name;
    private final List<Object> args;

    private RecordingSpan(ConverterTracer tracer, String name, Object... initialArgs) {
      // First, so the span covers as little of its own setup as possible.
      startNanos = System.nanoTime();
      this.tracer = tracer;
      this.name = name;
      args = new ArrayList<>(initialArgs.length + 4);
      Collections.addAll(args, initialArgs);
    }

    @Override
    public Span arg(String key, Object value) {
      args.add(key);
      args.add(value);
      return this;
    }

    @Override
    public void close() {
      // Duration first: reporting is the tracer's own cost and does not belong in the region's time.
      long durationNanos = System.nanoTime() - startNanos;
      tracer.onSpanCompleted(name, startNanos, durationNanos, args.toArray());
    }
  }
}
