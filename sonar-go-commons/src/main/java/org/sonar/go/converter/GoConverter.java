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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.persistence.ProtoTree;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TreeOrError;

public class GoConverter implements ASTConverter {
  private static final Logger LOG = LoggerFactory.getLogger(GoConverter.class);
  public static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private String gcExportDataDir;
  private String moduleBaseDir = ".";
  private boolean debugTypeCheck;
  private final GoServerProcess process;

  /**
   * Parses in a Go process of its own, extracted to the given directory and started right away. The process is never
   * closed: it stops when the JVM exits, as its stdin is then closed. For callers that parse outside of an analysis, such
   * as the tests of other analyzers; an analysis injects the process that its sensor starts and closes instead.
   *
   * @throws InitializationException when the Go executable cannot be extracted or started
   */
  public GoConverter(File workDir) {
    this(workDir, startedProcess(workDir));
  }

  /**
   * @param workDir the directory whose "go" subdirectory holds the GC export data by default
   * @param process the Go process to parse in, which the analysis starts and closes
   */
  public GoConverter(File workDir, GoServerProcess process) {
    this.gcExportDataDir = new File(workDir, "go").getAbsolutePath();
    this.process = process;
  }

  private static GoServerProcess startedProcess(File workDir) {
    var process = new GoServerProcess(workDir);
    process.start();
    return process;
  }

  @Override
  public Map<String, TreeOrError> parse(Map<String, String> filenameToContentMap, String moduleName) {
    Map<String, TreeOrError> result = HashMap.newHashMap(filenameToContentMap.size());
    Map<String, String> filesToParse = new HashMap<>();
    for (Map.Entry<String, String> entry : filenameToContentMap.entrySet()) {
      String filename = entry.getKey();
      String content = entry.getValue();
      if (content.length() > MAX_SUPPORTED_SOURCE_FILE_SIZE) {
        result.put(filename, TreeOrError.of("The file size is too big and should be excluded," +
          " its size is " + content.length() + " (maximum allowed is " + MAX_SUPPORTED_SOURCE_FILE_SIZE + " bytes)"));
      } else {
        filesToParse.put(filename, content);
      }
    }
    if (filesToParse.isEmpty()) {
      return result;
    }
    var arguments = new ArrayList<>(List.of("-module_name", moduleName, "-module_base_dir", moduleBaseDir, "-gc_export_data_dir", gcExportDataDir));
    if (debugTypeCheck) {
      arguments.add("-debug_type_check");
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing Go parse data command: {}", String.join(" ", arguments));
    }
    try {
      result.putAll(decode(process.execute(arguments, filesToParse)));
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
    return result;
  }

  /**
   * Rebuilds the trees of one batch. Deserializing the response is the last Java-side leg of the round
   * trip, and on a large batch it is not a rounding error next to the parse itself, so it gets a span
   * of its own.
   *
   * <p>A tree the analyzer cannot rebuild is an error for its own file, but a broken framing is what no
   * single file can cause, so {@link ProtoTree#fromProto} throws on it. Failing this batch, the way a
   * response the process could not send does, keeps that from aborting the whole analysis.
   */
  private static Map<String, TreeOrError> decode(byte[] response) {
    try (var span = ConverterTracing.span("tree.decode", "bytes", response.length)) {
      var trees = ProtoTree.fromProto(response);
      span.arg("trees", trees.size());
      return trees;
    } catch (IllegalStateException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }

  public void setGcExportDataDir(String gcExportDataDir) {
    this.gcExportDataDir = gcExportDataDir;
  }

  public void setModuleBaseDir(String moduleBaseDir) {
    this.moduleBaseDir = moduleBaseDir;
  }

  @Override
  public void debugTypeCheck() {
    debugTypeCheck = true;
  }
}
