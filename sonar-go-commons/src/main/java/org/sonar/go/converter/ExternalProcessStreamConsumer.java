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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalProcessStreamConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(ExternalProcessStreamConsumer.class);
  private final ExecutorService executorService;

  public ExternalProcessStreamConsumer() {
    executorService = Executors.newCachedThreadPool(r -> {
      Thread thread = new Thread(r);
      thread.setName("stream-consumer");
      thread.setDaemon(true);
      return thread;
    });
  }

  public final void consumeStream(InputStream inputStream, StreamConsumer streamConsumer) {
    executorService.submit(() -> {
      try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        readErrors(errorReader, streamConsumer);
      } catch (IOException e) {
        LOG.warn("Failure while reading stream", e);
      }
    });
  }

  protected void readErrors(BufferedReader errorReader, StreamConsumer streamConsumer) {
    errorReader.lines().forEach(streamConsumer::consumeLine);
    streamConsumer.finished();
  }

  public void shutdown() {
    if (executorService == null) {
      return;
    }
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  interface StreamConsumer {

    void consumeLine(String line);

    default void finished() {

    }
  }
}
