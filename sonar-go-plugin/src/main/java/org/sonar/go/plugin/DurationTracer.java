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

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Notified by {@link DurationStatistics} as each timed region completes, so an external caller (a
 * benchmark harness) can turn its per-visitor/per-check timing into real trace spans. No-op in a
 * normal analysis.
 */
public interface DurationTracer {

  DurationTracer NOOP = (id, detail, startNanos, durationNanos, breakdown) -> {
  };

  /**
   * @param id the id passed to {@code DurationStatistics.time(...)} — a visitor's simple class name or a rule key
   * @param detail what the region ran on, e.g. the file being visited — null where the caller didn't supply one
   * @param startNanos {@link System#nanoTime()} reading taken right before the timed block ran
   * @param durationNanos elapsed nanoseconds the block took
   * @param breakdown elapsed nanoseconds per untraced id that ran nested inside this region (e.g. a rule
   * key, for a {@code ChecksVisitor} region) — empty when nothing ran nested, never null
   */
  void onSpanCompleted(String id, @Nullable String detail, long startNanos, long durationNanos, Map<String, Long> breakdown);
}
