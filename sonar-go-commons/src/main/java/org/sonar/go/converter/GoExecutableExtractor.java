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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the sonar-go-to-slang executable of the current platform, bundled as a resource, to run it.
 */
public final class GoExecutableExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(GoExecutableExtractor.class);
  private static final int COPY_BUFFER_SIZE = 8192;
  // The executables already extracted by this JVM. Checking that an executable is up to date means reading it whole
  // from the plugin, which takes about 30 ms, and an analysis in SonarQube for IDE runs on every change of a file.
  private static final Set<String> EXTRACTED = ConcurrentHashMap.newKeySet();

  private GoExecutableExtractor() {
  }

  /**
   * Extracts the Go executable of the platform to the given directory, unless this JVM already did, and returns its path.
   *
   * @throws InitializationException when the platform is not supported, or the executable cannot be extracted
   */
  public static String extract(File workDir, PlatformInfo platformInfo) {
    try {
      return extractOrFail(workDir, platformInfo);
    } catch (IOException e) {
      throw new InitializationException(e.getMessage(), e);
    }
  }

  private static String extractOrFail(File workDir, PlatformInfo platformInfo) throws IOException {
    var executable = getExecutableForCurrentOS(platformInfo.osName(), platformInfo.osArch());
    var dest = new File(workDir, executable).getAbsoluteFile();
    var path = dest.getPath();
    if (EXTRACTED.contains(path) && dest.isFile()) {
      return path;
    }
    byte[] executableData = getBytesFromResource(executable);
    if (!fileMatch(dest, executableData)) {
      workDir.mkdirs();
      Files.write(dest.toPath(), executableData);
      if (!dest.setExecutable(true)) {
        throw new IOException("Unable to make the Go executable executable: " + path);
      }
    }
    EXTRACTED.add(path);
    return path;
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
    try (InputStream in = GoExecutableExtractor.class.getClassLoader().getResourceAsStream(executable)) {
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

  private static void copy(InputStream in, OutputStream out) throws IOException {
    var buffer = new byte[COPY_BUFFER_SIZE];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      out.write(buffer, 0, read);
    }
  }
}
