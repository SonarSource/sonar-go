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
package org.sonar.go.converter;

import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Isolated
class ExternalProcessStreamConsumerThreadTest {

  @Test
  void shouldNotLeakThreads() {
    var threadsBefore = getStreamConsumerThreadsName();

    var consumer = new ExternalProcessStreamConsumer();
    var input = "test line";
    var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

    var taskFinished = new CountDownLatch(1);
    var streamConsumer = new ExternalProcessStreamConsumer.StreamConsumer() {
      @Override
      public void consumeLine(@Nonnull String line) {
        // Process the line
      }

      @Override
      public void finished() {
        taskFinished.countDown();
      }
    };

    consumer.consumeStream(inputStream, streamConsumer);
    await().atMost(5, TimeUnit.SECONDS).until(() -> taskFinished.getCount() == 0);
    consumer.shutdown();

    // Give some time for thread cleanup
    await().pollDelay(10, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS).until(() -> true);

    var threadsAfterStop = getStreamConsumerThreadsName();
    assertThat(threadsAfterStop).isEqualTo(threadsBefore);
  }

  private static List<String> getStreamConsumerThreadsName() {
    var result = new ArrayList<String>();
    var threadMXBean = ManagementFactory.getThreadMXBean();
    var threads = threadMXBean.dumpAllThreads(true, true);
    for (ThreadInfo threadInfo : threads) {
      if (threadInfo.getThreadName().contains("stream-consumer")) {
        result.add(threadInfo.getThreadName());
      }
    }
    return result;
  }
}
