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
package org.sonar.go.report;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.go.plugin.GoFolder;
import org.sonar.plugins.go.api.GoInputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

class GoProgressReportTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private GoProgressReport progressReport;

  @AfterEach
  void tearDown() {
    if (progressReport != null) {
      progressReport.stop();
    }
  }

  @Test
  void shouldLogStartMessage() {
    progressReport = new GoProgressReport("test-thread", 1000);

    progressReport.start(List.of(new GoFolder("folder1", inputFiles(1))));
    assertThat(logTester.logs(Level.INFO)).containsOnly("1 folder (1 file) to be analyzed");
  }

  @Test
  void shouldLogStartMessagePluralized() {
    progressReport = new GoProgressReport("test-thread", 1000);

    progressReport.start(List.of(new GoFolder("folder1", inputFiles(2)), new GoFolder("folder2", inputFiles(2))));
    assertThat(logTester.logs(Level.INFO)).containsOnly("2 folders (4 files) to be analyzed");
  }

  @Test
  void shouldLogPeriodicMessages() {
    progressReport = new GoProgressReport("test-thread", 150);
    progressReport.start(List.of(new GoFolder("folder1", inputFiles(3)), new GoFolder("folder2", inputFiles(5))));

    waitNextLog();
    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "2 folders (8 files) to be analyzed",
        "1 / 2 folder analyzed, current folder: folder1 (3 files)");
    progressReport.setStep(GoProgressReport.Step.PARSING);

    waitNextLog();
    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "2 folders (8 files) to be analyzed",
        "1 / 2 folder analyzed, current folder: folder1 (3 files)",
        "1 / 2 folder analyzed, current folder: folder1 (3 files, parsing)");
    progressReport.nextFolder();

    waitNextLog();
    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "2 folders (8 files) to be analyzed",
        "1 / 2 folder analyzed, current folder: folder1 (3 files)",
        "1 / 2 folder analyzed, current folder: folder1 (3 files, parsing)",
        "2 / 2 folders analyzed, current folder: folder2 (5 files)");
    progressReport.setStep(GoProgressReport.Step.CACHING);

    waitNextLog();
    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "2 folders (8 files) to be analyzed",
        "1 / 2 folder analyzed, current folder: folder1 (3 files)",
        "1 / 2 folder analyzed, current folder: folder1 (3 files, parsing)",
        "2 / 2 folders analyzed, current folder: folder2 (5 files)",
        "2 / 2 folders analyzed, current folder: folder2 (5 files, caching)");
  }

  @Test
  void shouldLogFinalMessagesOnStopOneFolder() {
    progressReport = new GoProgressReport("test-thread", 150);
    progressReport.start(List.of(new GoFolder("folder1", inputFiles(3))));
    progressReport.nextFolder();
    progressReport.stop();

    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "1 folder (3 files) to be analyzed",
        "1/1 folder (3 files) has been analyzed");
  }

  @Test
  void shouldLogFinalMessagesOnStopPluralized() {
    progressReport = new GoProgressReport("test-thread", 150);
    progressReport.start(List.of(new GoFolder("folder1", inputFiles(3)), new GoFolder("folder2", inputFiles(5))));
    progressReport.nextFolder();
    progressReport.stop();

    assertThat(logTester.logs(Level.INFO))
      .containsExactly(
        "2 folders (8 files) to be analyzed",
        "2/2 folders (8 files) have been analyzed");
  }

  @Test
  void shouldStopPeriodicLoggingAfterStop() {
    progressReport = new GoProgressReport("test-thread", 30);
    progressReport.start(List.of(new GoFolder("folder1", inputFiles(3))));

    await().atMost(200, TimeUnit.MILLISECONDS)
      .untilAsserted(() -> assertThat(logTester.logs(Level.INFO)).hasSizeGreaterThan(2));

    progressReport.stop();
    int logCountBeforeStop = logTester.logs(Level.INFO).size();

    await().pollDelay(60, TimeUnit.MILLISECONDS)
      .atMost(200, TimeUnit.MILLISECONDS)
      .untilAsserted(() -> assertThat(logTester.logs(Level.INFO)).hasSize(logCountBeforeStop));
  }

  @Test
  void shouldHandleMultipleStopCallsGracefully() {
    progressReport = new GoProgressReport("test-thread", 100);
    progressReport.start(List.of(new GoFolder("folder1", inputFiles(3))));

    assertThatNoException().isThrownBy(() -> {
      progressReport.stop();
      progressReport.stop();
    });
  }

  @Test
  void stopWithoutStartShouldThrowAnException() {
    var myProgressReport = new GoProgressReport("test-thread", 100);
    assertThatThrownBy(myProgressReport::stop)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cannot stop: start method has to be called first");
  }

  List<GoInputFile> inputFiles(int size) {
    // create a list of mocked InputFile of the provided size
    return java.util.stream.IntStream.range(0, size)
      .mapToObj(i -> mock(GoInputFile.class))
      .toList();
  }

  void waitNextLog() {
    var currentLogCount = logTester.logs(Level.INFO).size();
    await().pollInterval(40, TimeUnit.MILLISECONDS)
      .atMost(500, TimeUnit.MILLISECONDS)
      .untilAsserted(() -> assertThat(logTester.logs(Level.INFO)).hasSizeGreaterThan(currentLogCount));
  }
}
