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
package org.sonar.go.report;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.plugin.GoFolder;
import org.sonarsource.analyzer.commons.ProgressReport;

/**
 * This class in an adaptation to {@link ProgressReport}, but with specificities for the Go analyzer.
 * The Go analyzer is not processing files one by one, but by directory.
 * As the parsing is processed by the go executable, it is not possible to display the progress of file processing.
 * This class allows to customize the progress report message to mention folder instead of files, while also specifying the size of the folder and the current state of the process
 * (parsing, executing checks...).
 */
public class GoProgressReport {

  private static final Logger LOG = LoggerFactory.getLogger(GoProgressReport.class);
  private final long period;
  private final TimeUnit timeUnit;
  private final ScheduledExecutorService scheduler;
  private ScheduledFuture<?> scheduledTask;
  private Iterator<GoFolder> itFolder;
  private long currentFolderIndex;
  private long totalFolders;
  private long totalFiles;
  private GoFolder currentFolder;
  @Nullable
  private Step currentStep;

  public GoProgressReport(String threadName, long period) {
    this.period = period;
    this.timeUnit = TimeUnit.MILLISECONDS;
    this.scheduler = Executors.newSingleThreadScheduledExecutor((Runnable run) -> {
      var thread = new Thread(run, threadName);
      thread.setDaemon(true);
      return thread;
    });
  }

  public void start(Collection<GoFolder> folders) {
    totalFolders = folders.size();
    currentFolderIndex = 0;
    itFolder = folders.iterator();
    totalFiles = folders.stream()
      .map(f -> f.files().size())
      .reduce(0, Integer::sum);

    log("%d %s (%d %s) to be analyzed".formatted(totalFolders, pluralizeFolder(folders.size()), totalFiles, pluralizeFile(totalFiles)));
    nextFolder();
    scheduledTask = scheduler.scheduleAtFixedRate(() -> log(
      "%d / %d %s analyzed, current folder: %s (%d %s%s)"
        .formatted(currentFolderIndex, totalFolders, pluralizeFolder(currentFolderIndex), currentFolder.name(), currentFolder.files().size(),
          pluralizeFile(currentFolder.files().size()), formatStep(currentStep))),
      period, period, timeUnit);
  }

  private static String pluralizeFolder(long count) {
    return pluralizeWord("folder", count);
  }

  private static String pluralizeFile(long count) {
    return pluralizeWord("file", count);
  }

  private static String pluralizeWord(String word, long count) {
    if (count == 1L) {
      return word;
    }
    return word + "s";
  }

  private static String pluralizeHas(long count) {
    if (count == 1L) {
      return "has";
    }
    return "have";
  }

  private static String formatStep(Step step) {
    if (step == null) {
      return "";
    }
    return ", %s".formatted(step.getDisplayName());
  }

  public void stop() {
    stopScheduler();
    log(
      "%d/%d %s (%d %s) %s been analyzed".formatted(totalFolders, totalFolders, pluralizeFolder(totalFolders), totalFiles, pluralizeFile(totalFiles), pluralizeHas(totalFolders)));
  }

  public void cancel() {
    stopScheduler();
  }

  private void stopScheduler() {
    if (scheduledTask == null) {
      throw new IllegalStateException("Cannot stop: start method has to be called first");
    }
    scheduledTask.cancel(false);
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public synchronized void nextFolder() {
    if (itFolder.hasNext()) {
      currentFolderIndex++;
      currentFolder = itFolder.next();
      setStep(null);
    }
  }

  public synchronized void setStep(@Nullable Step step) {
    currentStep = step;
  }

  private static void log(String message) {
    LOG.info(message);
  }

  public enum Step {
    CACHING,
    PARSING,
    HANDLING_PARSE_ERRORS,
    ANALYZING;

    public String getDisplayName() {
      return name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
  }
}
