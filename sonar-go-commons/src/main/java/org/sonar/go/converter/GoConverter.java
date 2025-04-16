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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.persistence.JsonTree;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoConverter implements ASTConverter {

  private static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private static final Logger LOG = LoggerFactory.getLogger(GoConverter.class);
  private static final long PROCESS_TIMEOUT_MS = 5_000;

  @Nullable
  private final ProcessBuilder processBuilder;
  private final ExternalProcessStreamConsumer errorConsumer;

  public GoConverter(File workDir) {
    this(DefaultCommand.createCommand(workDir));
  }

  // Visible for testing
  public GoConverter(@Nullable Command command) {
    if (command != null) {
      processBuilder = new ProcessBuilder(command.getCommand());
    } else {
      processBuilder = null;
    }
    errorConsumer = new ExternalProcessStreamConsumer();
  }

  @Override
  public Tree parse(String content) {
    if (processBuilder == null) {
      throw new ParseException("Go converter is not initialized");
    } else if (content.length() > MAX_SUPPORTED_SOURCE_FILE_SIZE) {
      throw new ParseException("The file size is too big and should be excluded," +
        " its size is " + content.length() + " (maximum allowed is " + MAX_SUPPORTED_SOURCE_FILE_SIZE + " bytes)");
    }
    try {
      var json = executeGoToJsonProcess(content);
      return JsonTree.fromJson(json);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParseException("Go parser external process interrupted: " + e.getMessage(), null, e);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }

  private String executeGoToJsonProcess(String content) throws IOException, InterruptedException {
    Process process = processBuilder.start();
    errorConsumer.consumeStream(process.getErrorStream(), LOG::debug);
    try (OutputStream out = process.getOutputStream()) {
      out.write(content.getBytes(UTF_8));
    }
    String output;
    try (InputStream in = process.getInputStream()) {
      output = readAsString(in);
    }
    boolean exited = process.waitFor(PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    if (exited && process.exitValue() != 0) {
      throw new ParseException("Go parser external process returned non-zero exit value: " + process.exitValue());
    }
    if (process.isAlive()) {
      process.destroyForcibly();
      throw new ParseException("Go parser external process took too long. External process killed forcibly");
    }
    return output;
  }

  private static String readAsString(InputStream in) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    copy(in, outputStream);
    return outputStream.toString(UTF_8);
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      out.write(buffer, 0, read);
    }
  }

  public interface Command {
    List<String> getCommand();
  }

  public static class DefaultCommand implements Command {

    protected final String command;

    public DefaultCommand(File workDir) {
      try {
        command = extract(workDir);
      } catch (IOException e) {
        throw new IllegalStateException(e.getMessage(), e);
      }
    }

    static Command createCommand(File workDir) {
      try {
        return new DefaultCommand(workDir);
      } catch (IllegalStateException e) {
        // GoConverter cannot be fully initialized (e.g. because of unsupported platform), continue in a no-op mode
        return null;
      }
    }

    @Override
    public List<String> getCommand() {
      return Arrays.asList(command, "-");
    }

    private static String extract(File workDir) throws IOException {
      String executable = getExecutableForCurrentOS(System.getProperty("os.name"), System.getProperty("os.arch"));
      byte[] executableData = getBytesFromResource(executable);
      File dest = new File(workDir, executable);
      if (!fileMatch(dest, executableData)) {
        Files.write(dest.toPath(), executableData);
        dest.setExecutable(true);
      }
      return dest.getAbsolutePath();
    }

    static boolean fileMatch(File dest, byte[] expectedContent) throws IOException {
      if (!dest.exists()) {
        return false;
      }
      byte[] actualContent = Files.readAllBytes(dest.toPath());
      return Arrays.equals(actualContent, expectedContent);
    }

    static byte[] getBytesFromResource(String executable) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (InputStream in = GoConverter.class.getClassLoader().getResourceAsStream(executable)) {
        if (in == null) {
          throw new IllegalStateException(executable + " binary not found on class path");
        }
        copy(in, out);
      }
      return out.toByteArray();
    }

    static String getExecutableForCurrentOS(String osName, String arch) {
      var os = osName.toLowerCase(Locale.ROOT);
      var extension = "";
      var isPlatformSupported = true;
      String suffix;

      if (os.contains("win")) {
        suffix = "windows";
        extension = ".exe";
      } else if (os.contains("mac")) {
        suffix = "darwin";
      } else {
        suffix = "linux";
      }

      if ("aarch64".equals(arch) || "arm64".equals(arch) || "armv8".equals(arch)) {
        suffix += "-arm64";
      } else if ("x86_64".equals(arch) || "amd64".equals(arch) || "x64".equals(arch)) {
        suffix += "-amd64";
      } else {
        isPlatformSupported = false;
      }

      if (isPlatformSupported) {
        var binaryName = "sonar-go-to-slang-" + suffix + extension;
        LOG.debug("Using Go converter binary: {}", binaryName);
        return binaryName;
      } else {
        throw new IllegalStateException("Unsupported OS/architecture: " + osName + "/" + arch);
      }
    }
  }
}
