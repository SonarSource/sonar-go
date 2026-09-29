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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.go.api.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A sonar-go-to-slang process, which runs every command sent to it instead of a new process being started for each of
 * them. A command is sent with its options and the files to analyze, and returns the output of the executable for
 * them. The protocol is described in server.go. The parsing and the GC export data export can share one process, as it
 * runs one command at a time.
 *
 * <p>{@link #start()} acquires the process for an analysis and {@link #close()} releases it. Overlapping analyses of a
 * plugin instance share it; the last close stops it. Commands can only run in between. A command fails with an {@link IOException} when the
 * process crashes or stops responding; the process is then stopped, and the next command starts a new one. A
 * {@link ParseException} leaves the process running because the server completed that response.
 *
 * <p>Each leg of the round trip is wrapped in a {@link ConverterTracing} span, which costs nothing when untraced:
 * "batch.encode", "write.stdin" and "drain.stdout" for every command, and "spawn" and "waitFor" once per process,
 * around starting and reaping it.
 */
public class GoServerProcess implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(GoServerProcess.class);
  static final long DEFAULT_COMMAND_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
  static final long COMMAND_TIMEOUT_MS_PER_MB = TimeUnit.SECONDS.toMillis(30);
  // Same limit as in server.go: a longer frame would be rejected by the server.
  static final long MAX_FRAME_LENGTH = 1 << 30;
  static final int MAX_RESPONSE_LENGTH = 256 << 20;
  private static final long SHUTDOWN_TIMEOUT_MS = 5_000;
  private static final byte STATUS_OK = 0;
  private static final byte STATUS_ERROR = 1;
  private static final int BUFFER_SIZE = 64 * 1024;
  private static final int FILENAME_AND_CONTENT_LENGTH = 8;

  // Where start() extracts the executable to; null when the executable is given
  @Nullable
  private final File workDir;
  @Nullable
  private final PlatformInfo platformInfo;
  // Extracted by start(), or given
  @Nullable
  private String executable;
  private final ProcessStarter processStarter;
  private final long commandTimeoutMs;
  // Created by start() and shut down by close(), like the process
  @Nullable
  private ScheduledThreadPoolExecutor watchdog;
  @Nullable
  private RunningProcess running;
  private final AtomicInteger activeAnalyses;

  public GoServerProcess(File workDir) {
    this(workDir, new SystemPlatformInfo());
  }

  /**
   * Runs the executable of the platform, which {@link #start()} extracts to the given directory unless this JVM already
   * did.
   */
  public GoServerProcess(File workDir, PlatformInfo platformInfo) {
    this(workDir, platformInfo, null, DEFAULT_COMMAND_TIMEOUT_MS);
  }

  GoServerProcess(ProcessStarter processStarter) {
    this(processStarter, DEFAULT_COMMAND_TIMEOUT_MS);
  }

  // Visible for testing
  GoServerProcess(ProcessStarter processStarter, long commandTimeoutMs) {
    this(null, null, processStarter, commandTimeoutMs);
  }

  /**
   * Nothing is extracted nor started before {@link #start()}.
   */
  private GoServerProcess(@Nullable File workDir, @Nullable PlatformInfo platformInfo,
    @Nullable ProcessStarter processStarter, long commandTimeoutMs) {
    this.workDir = workDir;
    this.platformInfo = platformInfo;
    this.processStarter = processStarter != null ? processStarter : this::startExecutable;
    this.commandTimeoutMs = commandTimeoutMs;
    this.activeAnalyses = new AtomicInteger(0);
  }

  /**
   * Acquires the process for one analysis, starting it for the first analysis.
   *
   * @throws InitializationException when the executable cannot be extracted, or the process cannot be started
   */
  public synchronized void start() {
    if (activeAnalyses.get() == 0) {
      if (workDir != null && platformInfo != null) {
        executable = GoExecutableExtractor.extract(workDir, platformInfo);
      }
      try {
        ensureRunning();
      } catch (IOException e) {
        throw new InitializationException("Unable to start the Go executable: " + e.getMessage(), e);
      }
      watchdog = new ScheduledThreadPoolExecutor(1, runnable -> {
        var thread = new Thread(runnable, "go-server-process-watchdog");
        thread.setDaemon(true);
        return thread;
      });
      // Otherwise every completed command would leave its canceled timeout in the queue until it expires.
      watchdog.setRemoveOnCancelPolicy(true);
    }
    activeAnalyses.incrementAndGet();
  }

  /**
   * Runs one command in the process, starting a new one first when the previous one has exited.
   *
   * @param arguments the options of the command, as defined in main.go
   * @return the output of the executable for the command
   * @throws IOException when the process could not be started, crashed or stopped responding
   * @throws ParseException when the executable failed on this input
   * @throws IllegalStateException when the process is not started
   */
  public synchronized String execute(List<String> arguments, Map<String, String> filenameToContentMap) throws IOException {
    if (activeAnalyses.get() == 0) {
      throw new IllegalStateException("The Go process is not started");
    }
    List<ByteBuffer> payload;
    try (var span = ConverterTracing.span("batch.encode", "files", filenameToContentMap.size())) {
      payload = convertToBytesArray(filenameToContentMap);
    }
    long payloadLength = payload.stream().mapToLong(buffer -> buffer.array().length).sum();
    if (payloadLength > MAX_FRAME_LENGTH) {
      throw new IOException("The files are too big to be sent to the Go executable: " + payloadLength + " bytes");
    }

    long effectiveTimeoutMs = effectiveTimeout(commandTimeoutMs, payloadLength);
    var timedOut = new AtomicBoolean();
    boolean responseComplete = false;
    try {
      var process = ensureRunning();
      var destroyedProcess = process.process();
      var timeout = watchdog.schedule(() -> {
        timedOut.set(true);
        // Unblocks the read of the response, which then fails.
        destroyedProcess.destroyForcibly();
      }, effectiveTimeoutMs, TimeUnit.MILLISECONDS);
      String response;
      try {
        try (var span = ConverterTracing.span("write.stdin")) {
          span.arg("bytes", writeRequest(process.stdin(), arguments, payload, (int) payloadLength));
        }
        try (var span = ConverterTracing.span("drain.stdout")) {
          response = readResponse(process.stdout());
          responseComplete = true;
          span.arg("chars", response.length());
        }
      } finally {
        if (!timeout.cancel(false)) {
          awaitWatchdogTask(timeout);
          stopRunningProcess(false);
        }
      }
      // Once the complete response has been read it is valid, even if the watchdog started at the deadline.
      return response;
    } catch (IOException e) {
      if (timedOut.get()) {
        throw new IOException("The Go executable did not respond within " + effectiveTimeoutMs + " ms", e);
      }
      throw e;
    } catch (ParseException e) {
      // The error response was read in full, so the next request can use the same process.
      responseComplete = true;
      throw e;
    } finally {
      if (!responseComplete) {
        // An incomplete exchange leaves the streams out of sync, even when an Error interrupts the read.
        stopRunningProcess(false);
      }
    }
  }

  static long effectiveTimeout(long minimumTimeoutMs, long payloadLength) {
    return Math.max(minimumTimeoutMs, payloadLength / (1024 * 1024) * COMMAND_TIMEOUT_MS_PER_MB);
  }

  /**
   * Releases one analysis. The last release stops the process after outstanding commands complete.
   */
  @Override
  public synchronized void close() {
    if (activeAnalyses.get() == 0) {
      return;
    }
    if (activeAnalyses.decrementAndGet() > 0) {
      return;
    }
    stopRunningProcess(true);
    stopWatchdog();
  }

  private static void awaitWatchdogTask(ScheduledFuture<?> timeout) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          timeout.get();
          return;
        } catch (InterruptedException e) {
          interrupted = true;
        } catch (CancellationException | ExecutionException e) {
          LOG.debug("The watchdog task for Go executable failed: {}", e.getMessage());
          return;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void stopWatchdog() {
    if (watchdog != null) {
      watchdog.shutdownNow();
      watchdog = null;
    }
  }

  private void stopRunningProcess(boolean graceful) {
    if (running == null) {
      return;
    }
    var process = running.process();
    try (var span = ConverterTracing.span("waitFor")) {
      // The end of the input is what makes the server stop after a complete response.
      if (graceful && closeInput(running.stdin())) {
        waitForExit(process);
      }
      process.destroyForcibly();
      // On Windows, the executable stays locked until the process has actually exited.
      waitForExit(process);
      if (!process.isAlive()) {
        span.arg("exitCode", process.exitValue());
      }
    }
    if (!graceful) {
      closeInput(running.stdin());
    }
    try {
      running.stdout().close();
    } catch (IOException e) {
      LOG.debug("Unable to close the Go executable output: {}", e.getMessage());
    }
    running.stderrConsumer().shutdown();
    running = null;
  }

  private static boolean closeInput(DataOutputStream stdin) {
    try {
      stdin.close();
      return true;
    } catch (IOException e) {
      LOG.debug("Unable to close the Go executable input: {}", e.getMessage());
      return false;
    }
  }

  private Process startExecutable() throws IOException {
    return new ProcessBuilder(executable).start();
  }

  private RunningProcess ensureRunning() throws IOException {
    if (running != null && !running.process().isAlive()) {
      LOG.debug("The Go executable exited with value {}, starting it again", running.process().exitValue());
      stopRunningProcess(false);
    }
    if (running == null) {
      try (var span = ConverterTracing.span("spawn")) {
        running = RunningProcess.start(processStarter);
        // The process closes the matching flow arrow using this same pid as the flow id.
        span.arg("pid", running.process().pid());
      }
    }
    return running;
  }

  /**
   * Each ByteBuffer on the result list contains bytes in the following format:
   * <pre>
   * N (4 bytes) file name length
   * file name (N bytes)
   * M (4 bytes) file content length
   * file content (M bytes)
   * <pre/>
   */
  private static List<ByteBuffer> convertToBytesArray(Map<String, String> filenameToContentMap) {
    List<ByteBuffer> buffers = new ArrayList<>();
    for (Map.Entry<String, String> filenameToContent : filenameToContentMap.entrySet()) {
      var filenameBytes = filenameToContent.getKey().getBytes(UTF_8);
      var contentBytes = filenameToContent.getValue().getBytes(UTF_8);
      int capacity = filenameBytes.length + contentBytes.length + FILENAME_AND_CONTENT_LENGTH;
      var byteBuffer = ByteBuffer.allocate(capacity)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(filenameBytes.length)
        .put(filenameBytes)
        .putInt(contentBytes.length)
        .put(contentBytes);
      buffers.add(byteBuffer);
    }
    return buffers;
  }

  private static String readResponse(DataInputStream in) throws IOException {
    byte status = in.readByte();
    if (status != STATUS_OK && status != STATUS_ERROR) {
      throw new IOException("Invalid response status from the Go executable: " + status);
    }
    int length = Integer.reverseBytes(in.readInt());
    if (length < 0 || length > MAX_RESPONSE_LENGTH) {
      throw new IOException("Invalid response length from the Go executable: " + length);
    }
    var body = new byte[length];
    in.readFully(body);
    var response = new String(body, UTF_8);
    if (status == STATUS_ERROR) {
      throw new ParseException("Go executable failed: " + response);
    }
    return response;
  }

  /**
   * Returns the number of bytes written.
   */
  private static long writeRequest(DataOutputStream out, List<String> arguments, List<ByteBuffer> payload, int payloadLength) throws IOException {
    // Counted here rather than with out.size(), which saturates once the process has been sent 2 GB in total.
    long written = Integer.BYTES + Integer.BYTES + (long) payloadLength;
    out.writeInt(Integer.reverseBytes(arguments.size()));
    for (String argument : arguments) {
      var bytes = argument.getBytes(UTF_8);
      out.writeInt(Integer.reverseBytes(bytes.length));
      out.write(bytes);
      written += Integer.BYTES + bytes.length;
    }
    out.writeInt(Integer.reverseBytes(payloadLength));
    for (ByteBuffer buffer : payload) {
      out.write(buffer.array());
    }
    out.flush();
    return written;
  }

  private static void waitForExit(Process process) {
    try {
      process.waitFor(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @FunctionalInterface
  interface ProcessStarter {
    Process start() throws IOException;
  }

  private record RunningProcess(Process process, DataOutputStream stdin, DataInputStream stdout, ExternalProcessStreamConsumer stderrConsumer) {
    static RunningProcess start(ProcessStarter processStarter) throws IOException {
      var process = processStarter.start();
      var stderrConsumer = new ExternalProcessStreamConsumer();
      stderrConsumer.consumeStream(process.getErrorStream(), LOG::debug);
      return new RunningProcess(process,
        new DataOutputStream(new BufferedOutputStream(process.getOutputStream(), BUFFER_SIZE)),
        new DataInputStream(new BufferedInputStream(process.getInputStream(), BUFFER_SIZE)),
        stderrConsumer);
    }
  }
}
