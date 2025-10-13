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
package org.sonar.go.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ExternalProcessStreamConsumerTest {

  private ExternalProcessStreamConsumer.StreamConsumer createCollectingConsumer(
    List<String> lines, CountDownLatch finishedLatch) {
    return new ExternalProcessStreamConsumer.StreamConsumer() {
      @Override
      public void consumeLine(@Nonnull String line) {
        lines.add(line);
      }

      @Override
      public void finished() {
        finishedLatch.countDown();
      }
    };
  }

  private ExternalProcessStreamConsumer.StreamConsumer createLongRunningConsumer(
    CountDownLatch taskStarted, CountDownLatch shutdownSignal, boolean handleInterrupt) {
    return line -> {
      taskStarted.countDown();
      try {
        shutdownSignal.await();
      } catch (InterruptedException ignored) {
        if (handleInterrupt) {
          Thread.currentThread().interrupt();
        }
        // If handleInterrupt is false, intentionally don't restore interrupt status
      }
    };
  }

  @Test
  void shouldHandleIOExceptionDuringStreamConsumption() {
    var consumer = new ExternalProcessStreamConsumer();
    var linesConsumed = new ArrayList<String>();
    var finishedCalled = new CountDownLatch(1);

    // Create an InputStream that throws IOException when read
    var faultyInputStream = new java.io.InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("Simulated IOException");
      }
    };

    var streamConsumer = createCollectingConsumer(linesConsumed, finishedCalled);

    consumer.consumeStream(faultyInputStream, streamConsumer);

    await().atMost(1, TimeUnit.SECONDS)
      .untilAsserted(() -> assertThat(finishedCalled.getCount()).isEqualTo(1));

    assertThat(finishedCalled.getCount()).isEqualTo(1);
    assertThat(linesConsumed).isEmpty();

    consumer.shutdown();
  }

  @Test
  void shouldHandleNullExecutorServiceInShutdown() {
    var consumer = new ExternalProcessStreamConsumer();

    try {
      var field = ExternalProcessStreamConsumer.class.getDeclaredField("executorService");
      field.setAccessible(true);
      field.set(consumer, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertDoesNotThrow(consumer::shutdown);
  }

  @Test
  void shouldCallFinishedAfterProcessingAllLines() {
    var consumer = new ExternalProcessStreamConsumer();
    var input = "line1\nline2\nline3";
    var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

    var linesProcessed = new ArrayList<String>();
    var finishedCalled = new CountDownLatch(1);
    var streamConsumer = createCollectingConsumer(linesProcessed, finishedCalled);

    consumer.consumeStream(inputStream, streamConsumer);

    await().atMost(5, TimeUnit.SECONDS).until(() -> finishedCalled.getCount() == 0);
    assertThat(linesProcessed).containsExactly("line1", "line2", "line3");

    consumer.shutdown();
  }

  @Test
  void shouldHandleEmptyInputStream() {
    var consumer = new ExternalProcessStreamConsumer();
    var inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

    var linesProcessed = new ArrayList<String>();
    var finishedCalled = new CountDownLatch(1);
    var streamConsumer = createCollectingConsumer(linesProcessed, finishedCalled);

    consumer.consumeStream(inputStream, streamConsumer);

    await().atMost(5, TimeUnit.SECONDS).until(() -> finishedCalled.getCount() == 0);
    assertThat(linesProcessed).isEmpty();

    consumer.shutdown();
  }

  @Test
  void shouldCallShutdownNowWhenAwaitTerminationTimesOut() {
    var consumer = new ExternalProcessStreamConsumer();
    var input = "test line";
    var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

    var taskStarted = new CountDownLatch(1);
    var shutdownSignal = new CountDownLatch(1);
    var streamConsumer = createLongRunningConsumer(taskStarted, shutdownSignal, false);

    consumer.consumeStream(inputStream, streamConsumer);

    await().atMost(5, TimeUnit.SECONDS).until(() -> taskStarted.getCount() == 0);

    consumer.shutdown();
  }

  @Test
  void shouldHandleInterruptedExceptionDuringShutdown() {
    var consumer = new ExternalProcessStreamConsumer();
    var input = "test line";
    var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

    var taskStarted = new CountDownLatch(1);
    var shutdownSignal = new CountDownLatch(1);
    var streamConsumer = createLongRunningConsumer(taskStarted, shutdownSignal, true);

    consumer.consumeStream(inputStream, streamConsumer);

    await().atMost(5, TimeUnit.SECONDS).until(() -> taskStarted.getCount() == 0);

    Thread currentThread = Thread.currentThread();
    var interrupterThread = new Thread(() -> {
      await().pollDelay(50, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS).until(() -> true);
      currentThread.interrupt();
    });

    interrupterThread.start();
    consumer.shutdown();

    assertThat(Thread.interrupted()).isTrue();

    interrupterThread.interrupt();
  }
}
