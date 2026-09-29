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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ExternalProcessStreamConsumerThreadTest {

  @Test
  void shouldNotLeakThreads() {
    var consumer = new ExternalProcessStreamConsumer();
    var input = "test line";
    var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

    var taskFinished = new CountDownLatch(1);
    var worker = new AtomicReference<Thread>();
    var streamConsumer = new ExternalProcessStreamConsumer.StreamConsumer() {
      @Override
      public void consumeLine(@Nonnull String line) {
        // Process the line
      }

      @Override
      public void finished() {
        worker.set(Thread.currentThread());
        taskFinished.countDown();
      }
    };

    consumer.consumeStream(inputStream, streamConsumer);
    await().atMost(5, TimeUnit.SECONDS).until(() -> taskFinished.getCount() == 0);
    assertThat(worker.get().getName()).isEqualTo("stream-consumer");
    consumer.shutdown();

    await().atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> assertThat(worker.get().isAlive()).isFalse());
  }
}
