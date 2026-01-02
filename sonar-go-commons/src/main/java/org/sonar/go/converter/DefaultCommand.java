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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.go.api.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultCommand.class);
  private static final long PROCESS_TIMEOUT_MS = 5_000;
  private static final int COPY_BUFFER_SIZE = 8192;
  private static final int FILENAME_AND_CONTENT_LENGTH = 8;

  protected final List<String> command;

  public DefaultCommand(File workDir, String... arguments) {
    this(workDir, new SystemPlatformInfo(), arguments);
  }

  public DefaultCommand(File workDir, PlatformInfo platformInfo, String... arguments) {
    try {
      command = new ArrayList<>();
      var executable = extract(workDir, platformInfo);
      command.add(executable);
      command.addAll(Arrays.asList(arguments));
    } catch (IOException e) {
      throw new InitializationException(e.getMessage(), e);
    }
  }

  @Override
  public List<String> getCommand() {
    return command;
  }

  @Override
  public String executeCommand(Map<String, String> filenameToContentMap) throws IOException, InterruptedException {
    var byteBuffers = convertToBytesArray(filenameToContentMap);

    var processBuilder = new ProcessBuilder(getCommand());
    var errorConsumer = new ExternalProcessStreamConsumer();

    var process = processBuilder.start();
    try {
      errorConsumer.consumeStream(process.getErrorStream(), LOG::debug);
      try (var out = process.getOutputStream()) {
        for (ByteBuffer byteBuffer : byteBuffers) {
          out.write(byteBuffer.array());
        }
      }
      String output;
      try (var in = process.getInputStream()) {
        output = readAsString(in);
      }
      boolean exited = process.waitFor(PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      if (exited && process.exitValue() != 0) {
        throw new ParseException("Go executable returned non-zero exit value: " + process.exitValue());
      }
      if (process.isAlive()) {
        process.destroyForcibly();
        throw new ParseException("Go executable took too long. External process killed forcibly");
      }
      return output;
    } finally {
      errorConsumer.shutdown();
    }
  }

  public void debugTypeCheck() {
    command.add("-debug_type_check");
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

  private static String readAsString(InputStream in) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    copy(in, outputStream);
    return outputStream.toString(UTF_8);
  }

  private static String extract(File workDir, PlatformInfo platformInfo) throws IOException {
    var executable = getExecutableForCurrentOS(platformInfo.osName(), platformInfo.osArch());
    byte[] executableData = getBytesFromResource(executable);
    var dest = new File(workDir, executable);
    if (!fileMatch(dest, executableData)) {
      workDir.mkdirs();
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
    var out = new ByteArrayOutputStream();
    try (InputStream in = DefaultCommand.class.getClassLoader().getResourceAsStream(executable)) {
      if (in == null) {
        throw new InitializationException(executable + " binary not found on class path");
      }
      copy(in, out);
    }
    return out.toByteArray();
  }

  static String getExecutableForCurrentOS(String osName, String arch) {
    var os = osName.toLowerCase(Locale.ROOT);
    var extension = "";
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
    }

    var isPlatformSupported = switch (suffix) {
      // should be kept in sync with the platforms defined in make.sh
      case "windows-amd64", "linux-amd64", "darwin-amd64", "linux-arm64", "darwin-arm64" -> true;
      default -> false;
    };

    if (isPlatformSupported) {
      var binaryName = "sonar-go-to-slang-" + suffix + extension;
      LOG.debug("Using Go converter binary: {}", binaryName);
      return binaryName;
    } else {
      throw new InitializationException("Unsupported OS/architecture: " + osName + "/" + arch);
    }
  }

  public static void copy(InputStream in, OutputStream out) throws IOException {
    var buffer = new byte[COPY_BUFFER_SIZE];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      out.write(buffer, 0, read);
    }
  }
}
