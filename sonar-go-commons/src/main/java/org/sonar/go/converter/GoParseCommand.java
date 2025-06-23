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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoParseCommand extends DefaultCommand {
  private static final Logger LOG = LoggerFactory.getLogger(GoParseCommand.class);

  public GoParseCommand(File workDir, String... extraArgs) {
    super(workDir, mergeArgs(extraArgs,
      "-gc_export_data_dir",
      new File(workDir, "go").getAbsolutePath()));
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

  public String executeGoParseCommand(Map<String, String> filenameToContentMap)
    throws IOException, InterruptedException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing Go parse data command: {}", String.join(" ", command));
    }
    return super.executeCommand(filenameToContentMap);
  }
}
