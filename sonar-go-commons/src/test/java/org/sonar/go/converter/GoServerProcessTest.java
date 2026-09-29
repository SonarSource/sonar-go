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
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.go.api.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoServerProcessTest {

  private static final Map<String, String> MAIN_FILE = Map.of("main.go", "package main\n\nfunc main() {}\n");
  private static final List<String> PARSE_ARGUMENTS = List.of("-module_name", "example.com/mod");

  @TempDir
  File tempDir;

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private String executable;
  private final List<Process> startedProcesses = new ArrayList<>();

  @BeforeEach
  void setUp() {
    executable = GoExecutableExtractor.extract(tempDir, new SystemPlatformInfo());
  }

  private static GoServerProcess started(GoServerProcess server) {
    server.start();
    return server;
  }

  private GoServerProcess realServer() {
    return new GoServerProcess(() -> {
      var process = new ProcessBuilder(executable).start();
      startedProcesses.add(process);
      return process;
    });
  }

  @Test
  void shouldAllowOverlappingAnalysesToShareTheProcess() throws IOException {
    try (var server = realServer()) {
      server.start();
      server.start();
      server.close();

      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
      assertThat(startedProcesses).hasSize(1);
      assertThat(startedProcesses.get(0).isAlive()).isTrue();
    }
    assertThat(startedProcesses.get(0).isAlive()).isFalse();
  }

  @Test
  void shouldScaleTheTimeoutWithThePayloadSize() {
    assertThat(GoServerProcess.effectiveTimeout(100, 1024 * 1024)).isEqualTo(GoServerProcess.COMMAND_TIMEOUT_MS_PER_MB);
    assertThat(GoServerProcess.effectiveTimeout(GoServerProcess.DEFAULT_COMMAND_TIMEOUT_MS, 10L * 1024 * 1024))
      .isEqualTo(GoServerProcess.DEFAULT_COMMAND_TIMEOUT_MS);
    assertThat(GoServerProcess.effectiveTimeout(GoServerProcess.DEFAULT_COMMAND_TIMEOUT_MS, 20L * 1024 * 1024))
      .isEqualTo(20 * GoServerProcess.COMMAND_TIMEOUT_MS_PER_MB);
  }

  @Test
  void shouldRunAllCommandsInOneProcess() throws IOException {
    var server = started(realServer());
    try {
      var first = server.execute(PARSE_ARGUMENTS, MAIN_FILE);
      var second = server.execute(PARSE_ARGUMENTS, Map.of("utils.go", "package main\n\nfunc helper() {}\n"));
      var ast = server.execute(List.of("-d"), MAIN_FILE);

      assertThat(first).contains("\"main.go\"").contains("\"error\": null");
      assertThat(second).contains("\"utils.go\"").doesNotContain("\"main.go\"");
      assertThat(ast).contains("Package: token.Pos(1)");
      assertThat(startedProcesses).hasSize(1);
    } finally {
      server.close();
    }
  }

  @Test
  void shouldReportFailedCommandAsParseExceptionAndKeepTheProcess() throws IOException {
    var server = started(realServer());
    try {
      var arguments = List.of("-dump_gc_export_data");
      assertThatThrownBy(() -> server.execute(arguments, MAIN_FILE))
        .isInstanceOf(ParseException.class)
        .hasMessage("Go executable failed: If the dump_gc_export_data flag is set then the gc_export_data_dir flag must be set too");

      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
      assertThat(startedProcesses).hasSize(1);
    } finally {
      server.close();
    }
  }

  @Test
  void shouldStartANewProcessWhenThePreviousOneExited() throws Exception {
    var server = started(realServer());
    try {
      server.execute(PARSE_ARGUMENTS, MAIN_FILE);
      startedProcesses.get(0).destroyForcibly().waitFor();

      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
      assertThat(startedProcesses).hasSize(2);
      assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.startsWith("The Go executable exited with value"));
    } finally {
      server.close();
    }
  }

  @Test
  void shouldStartTheProcessEagerlyAndOnlyOnce() throws IOException {
    var server = realServer();
    try {
      server.start();
      server.start();

      assertThat(startedProcesses).hasSize(1);
      assertThat(startedProcesses.get(0).isAlive()).isTrue();
      server.close();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
      assertThat(startedProcesses.get(0).isAlive()).isTrue();
    } finally {
      server.close();
    }
    assertThat(startedProcesses.get(0).isAlive()).isFalse();
  }

  @Test
  void shouldStopTheProcessWhenClosedUntilStartedAgain() throws IOException {
    var server = started(realServer());
    server.execute(PARSE_ARGUMENTS, MAIN_FILE);

    server.close();
    server.close();

    var stopped = startedProcesses.get(0);
    assertThat(stopped.isAlive()).isFalse();
    assertThat(stopped.exitValue()).as("the process must have stopped on its own").isZero();
    // The stderr of the process is logged, and known to be logged in full once the process is stopped.
    assertThat(logTester.logs(Level.DEBUG)).contains("Starting in server mode");
    assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The Go process is not started");

    // As the next analysis does
    try {
      server.start();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
      assertThat(startedProcesses).hasSize(2);
    } finally {
      server.close();
    }
  }

  @Test
  void shouldStartNothingBeforeStartAndStopEverythingOnClose() throws IOException {
    // Relative to the threads of the servers that other tests of this JVM left open
    int watchdogThreadsBefore = watchdogThreads().size();
    var server = realServer();
    assertThat(startedProcesses).isEmpty();
    assertThat(watchdogThreads()).hasSize(watchdogThreadsBefore);

    server.start();
    server.execute(PARSE_ARGUMENTS, MAIN_FILE);
    assertThat(startedProcesses).hasSize(1);
    assertThat(watchdogThreads()).hasSize(watchdogThreadsBefore + 1);

    server.close();
    assertThat(startedProcesses.get(0).isAlive()).isFalse();
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> watchdogThreads().size() == watchdogThreadsBefore);
  }

  private static List<Thread> watchdogThreads() {
    return Thread.getAllStackTraces().keySet().stream()
      .filter(thread -> thread.getName().equals("go-server-process-watchdog") && thread.isAlive())
      .toList();
  }

  @Test
  void shouldNotRunAnyCommandBeforeStart() {
    var server = realServer();

    assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The Go process is not started");
    server.close();
    assertThat(startedProcesses).isEmpty();
  }

  @Test
  void shouldExtractTheExecutableOfThePlatformWhenStarted(@TempDir File otherDir) throws IOException {
    var platform = new SystemPlatformInfo();
    var extracted = new File(otherDir, GoExecutableExtractor.getExecutableForCurrentOS(platform.osName(), platform.osArch()));
    try (var server = new GoServerProcess(otherDir, platform)) {
      assertThat(extracted).as("nothing is extracted before start()").doesNotExist();

      server.start();

      assertThat(extracted).exists();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).contains("\"main.go\"");
    }
  }

  @Test
  void shouldFailToStartOnAnUnsupportedPlatform() {
    var server = new GoServerProcess(tempDir, new TestPlatformInfo("unsupported-os", "unsupported-arch"));

    assertThatThrownBy(server::start)
      .isInstanceOf(InitializationException.class)
      .hasMessage("Unsupported OS/architecture: unsupported-os/unsupported-arch");
  }

  @Test
  void shouldSendTheArgumentsAndTheFilesOfTheCommand() throws IOException {
    var process = FakeProcess.answering(response((byte) 0, "output"));
    try (var server = started(new GoServerProcess(() -> process))) {
      assertThat(server.execute(List.of("-module_name", "mod"), Map.of("a.go", "package a"))).isEqualTo("output");
    }

    var request = ByteBuffer.wrap(process.stdin.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
    assertThat(request.getInt()).isEqualTo(2);
    assertThat(readFrame(request)).isEqualTo("-module_name");
    assertThat(readFrame(request)).isEqualTo("mod");
    var payload = ByteBuffer.wrap(readFrame(request).getBytes(UTF_8)).order(ByteOrder.LITTLE_ENDIAN);
    assertThat(readFrame(payload)).isEqualTo("a.go");
    assertThat(readFrame(payload)).isEqualTo("package a");
    assertThat(payload.hasRemaining()).isFalse();
    assertThat(request.hasRemaining()).isFalse();
  }

  @Test
  void shouldStopTheProcessWhenItDoesNotRespondInTime() throws IOException {
    var hanging = FakeProcess.hanging();
    var next = FakeProcess.answering(response((byte) 0, "output"));
    var processes = new ArrayList<>(List.of(hanging, next));
    try (var server = started(new GoServerProcess(() -> processes.remove(0), 100))) {
      assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(IOException.class)
        .hasMessage("The Go executable did not respond within 100 ms");
      assertThat(hanging.isAlive()).isFalse();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).as("the next command starts a new process").isEqualTo("output");
    }
  }

  @Test
  void shouldKeepACompleteResponseWhenTheWatchdogStartsAtTheDeadline() throws IOException {
    var released = new CountDownLatch(1);
    var answer = new ByteArrayInputStream(response((byte) 0, "output"));
    var delayed = new InputStream() {
      private void waitForWatchdog() {
        try {
          if (!released.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The watchdog did not run");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        }
      }

      @Override
      public int read() {
        waitForWatchdog();
        return answer.read();
      }

      @Override
      public int read(byte[] bytes, int off, int length) {
        waitForWatchdog();
        return answer.read(bytes, off, length);
      }
    };
    var process = new FakeProcess(delayed, released::countDown);
    try (var server = started(new GoServerProcess(() -> process, 25))) {
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isEqualTo("output");
      assertThat(process.isAlive()).isFalse();
    }
  }

  @Test
  void shouldDiscardTheProcessAfterAnUncheckedFailureWhileReading() throws IOException {
    var broken = new FakeProcess(new InputStream() {
      @Override
      public int read() {
        throw new IllegalStateException("Reader failed");
      }
    }, () -> {
    });
    var next = FakeProcess.answering(response((byte) 0, "output"));
    var processes = new ArrayList<>(List.of(broken, next));
    try (var server = started(new GoServerProcess(() -> processes.remove(0)))) {
      assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Reader failed");
      assertThat(broken.isAlive()).isFalse();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isEqualTo("output");
    }
  }

  @Test
  void shouldDiscardTheProcessAfterAnErrorWhileReading() throws IOException {
    var header = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0).putInt(1024 * 1024).array();
    var broken = new FakeProcess(new InputStream() {
      private final ByteArrayInputStream head = new ByteArrayInputStream(header);

      @Override
      public int read() {
        if (head.available() == 0) {
          throw new AssertionError("Reader failed");
        }
        return head.read();
      }

      @Override
      public int read(byte[] bytes, int off, int len) {
        if (head.available() == 0) {
          throw new AssertionError("Reader failed");
        }
        return head.read(bytes, off, len);
      }
    }, () -> {
    });
    var next = FakeProcess.answering(response((byte) 0, "output"));
    var processes = new ArrayList<>(List.of(broken, next));
    try (var server = started(new GoServerProcess(() -> processes.remove(0)))) {
      assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(AssertionError.class)
        .hasMessage("Reader failed");
      assertThat(broken.isAlive()).isFalse();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isEqualTo("output");
    }
  }

  @Test
  void shouldStopTheProcessWhenItCrashes() throws IOException {
    var crashing = FakeProcess.answering(new byte[] {0, 42});
    var next = FakeProcess.answering(response((byte) 0, "output"));
    var processes = new ArrayList<>(List.of(crashing, next));
    try (var server = started(new GoServerProcess(() -> processes.remove(0)))) {
      assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isInstanceOf(EOFException.class);
      assertThat(crashing.isAlive()).isFalse();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).as("the next command starts a new process").isEqualTo("output");
    }
  }

  @Test
  void shouldCloseTheProcessOutputWhenStopped() throws IOException {
    var outputClosed = new AtomicBoolean();
    var output = new ByteArrayInputStream(response((byte) 0, "output")) {
      @Override
      public void close() throws IOException {
        outputClosed.set(true);
        super.close();
      }
    };
    var process = new FakeProcess(output, () -> {
    });
    try (var server = started(new GoServerProcess(() -> process))) {
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isEqualTo("output");
    }
    assertThat(outputClosed).isTrue();
  }

  @Test
  void shouldFailToStartWhenTheProcessCannotBeStarted() {
    var starts = new int[1];
    var server = new GoServerProcess(() -> {
      starts[0]++;
      throw new IOException("Cannot run program");
    });

    assertThatThrownBy(server::start)
      .isInstanceOf(InitializationException.class)
      .hasMessage("Unable to start the Go executable: Cannot run program");
    assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The Go process is not started");
    assertThatThrownBy(server::start).isInstanceOf(InitializationException.class);
    assertThat(starts[0]).as("a failed start is tried again by the next one").isEqualTo(2);
  }

  @Test
  void shouldFailOnInvalidResponses() {
    try (var invalidStatus = started(new GoServerProcess(() -> FakeProcess.answering(response((byte) 7, ""))));
      var negativeLength = started(new GoServerProcess(() -> FakeProcess.answering(new byte[] {0, -1, -1, -1, -1})))) {
      assertThatThrownBy(() -> invalidStatus.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid response status from the Go executable: 7");
      assertThatThrownBy(() -> negativeLength.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid response length from the Go executable: -1");
    }
  }

  @Test
  void shouldRejectOversizedResponsesBeforeAllocatingThem() throws IOException {
    var tooLarge = FakeProcess.answering(ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
      .put((byte) 0).putInt(GoServerProcess.MAX_RESPONSE_LENGTH + 1).array());
    var next = FakeProcess.answering(response((byte) 0, "output"));
    var processes = new ArrayList<>(List.of(tooLarge, next));
    try (var server = started(new GoServerProcess(() -> processes.remove(0)))) {
      assertThatThrownBy(() -> server.execute(PARSE_ARGUMENTS, MAIN_FILE))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid response length from the Go executable: " + (GoServerProcess.MAX_RESPONSE_LENGTH + 1));
      assertThat(tooLarge.isAlive()).isFalse();
      assertThat(tooLarge.waitsWhileAlive).as("an incomplete response must skip the graceful wait").isZero();
      assertThat(server.execute(PARSE_ARGUMENTS, MAIN_FILE)).isEqualTo("output");
    }
  }

  private static String readFrame(ByteBuffer buffer) {
    var bytes = new byte[buffer.getInt()];
    buffer.get(bytes);
    return new String(bytes, UTF_8);
  }

  private static byte[] response(byte status, String body) {
    var bodyBytes = body.getBytes(UTF_8);
    return ByteBuffer.allocate(5 + bodyBytes.length).order(ByteOrder.LITTLE_ENDIAN)
      .put(status)
      .putInt(bodyBytes.length)
      .put(bodyBytes)
      .array();
  }

  private static final class FakeProcess extends Process {
    private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
    private final InputStream stdout;
    private final Runnable onDestroy;
    private volatile boolean alive = true;
    private int waitsWhileAlive;

    private FakeProcess(InputStream stdout, Runnable onDestroy) {
      this.stdout = stdout;
      this.onDestroy = onDestroy;
    }

    static FakeProcess answering(byte[] stdout) {
      return new FakeProcess(new ByteArrayInputStream(stdout), () -> {
      });
    }

    /** Never answers, until it is destroyed: like for a real process, its stdout is then closed. */
    static FakeProcess hanging() {
      var stdoutWriter = new PipedOutputStream();
      try {
        var stdout = new PipedInputStream(stdoutWriter);
        return new FakeProcess(stdout, () -> {
          try {
            stdoutWriter.close();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public OutputStream getOutputStream() {
      return stdin;
    }

    @Override
    public InputStream getInputStream() {
      return stdout;
    }

    @Override
    public InputStream getErrorStream() {
      return InputStream.nullInputStream();
    }

    @Override
    public int waitFor() {
      return 0;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
      if (alive) {
        waitsWhileAlive++;
      }
      return !alive;
    }

    @Override
    public int exitValue() {
      if (alive) {
        throw new IllegalThreadStateException("The process is still running");
      }
      return 137;
    }

    @Override
    public void destroy() {
      alive = false;
      onDestroy.run();
    }

    @Override
    public Process destroyForcibly() {
      destroy();
      return this;
    }

    @Override
    public boolean isAlive() {
      return alive;
    }

    @Override
    public long pid() {
      return 42;
    }
  }
}
