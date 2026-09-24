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

/**
 * Notified by {@link MemoryMonitor} each time it takes a heap-usage snapshot, so an external caller
 * (a benchmark harness) can plot it on a trace timeline. No-op in a normal analysis. Unlike
 * {@link DurationTracer}, no nanoTime reading is passed: a snapshot is instantaneous, and the
 * notification fires synchronously at the moment it's taken, so the caller's own "now" is already correct.
 */
public interface MemoryTracer {

  MemoryTracer NOOP = (name, usedMb, peakMb) -> {
  };

  void onMemoryRecorded(String name, long usedMb, long peakMb);
}
