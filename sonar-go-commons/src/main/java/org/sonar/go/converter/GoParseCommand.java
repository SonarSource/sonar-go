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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoParseCommand extends DefaultCommand {
  private static final Logger LOG = LoggerFactory.getLogger(GoParseCommand.class);
  private final int moduleNameIndex;
  private final int moduleBaseDirIndex;
  private final int gcExportDataDirIndex;

  public GoParseCommand(File workDir, String... extraArgs) {
    this(workDir, new SystemPlatformInfo(), extraArgs);
  }

  public GoParseCommand(File workDir, PlatformInfo platformInfo, String... extraArgs) {
    super(workDir, platformInfo, mergeArgs(extraArgs,
      "-module_name", "<module_name>",
      "-module_base_dir", "<module_base_dir>",
      "-gc_export_data_dir", "<gc_export_data_dir>"));
    moduleNameIndex = command.indexOf("<module_name>");
    moduleBaseDirIndex = command.indexOf("<module_base_dir>");
    gcExportDataDirIndex = command.indexOf("<gc_export_data_dir>");
    command.set(moduleBaseDirIndex, ".");
    command.set(gcExportDataDirIndex, new File(workDir, "go").getAbsolutePath());
  }

  public void setGcExportDataDir(String gcExportDataDir) {
    command.set(gcExportDataDirIndex, gcExportDataDir);
  }

  public void setModuleBaseDir(String moduleBaseDir) {
    command.set(moduleBaseDirIndex, moduleBaseDir);
  }

  private static String[] mergeArgs(String[] args, String... extraArgs) {
    if (args.length == 0) {
      return extraArgs;
    }
    var merged = new String[args.length + extraArgs.length];
    System.arraycopy(args, 0, merged, 0, args.length);
    System.arraycopy(extraArgs, 0, merged, args.length, extraArgs.length);
    return merged;
  }

  public String executeGoParseCommand(Map<String, String> filenameToContentMap, String moduleName)
    throws IOException, InterruptedException {
    command.set(moduleNameIndex, moduleName);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing Go parse data command: {}", String.join(" ", command));
    }
    return super.executeCommand(filenameToContentMap);
  }
}
