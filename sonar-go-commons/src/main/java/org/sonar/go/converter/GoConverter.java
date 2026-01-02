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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.persistence.JsonTree;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TreeOrError;

public class GoConverter implements ASTConverter {
  private static final Logger LOG = LoggerFactory.getLogger(GoConverter.class);
  public static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private final GoParseCommand command;
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);

  public GoConverter(File workDir) {
    this(workDir, new SystemPlatformInfo());
  }

  public GoConverter(File workDir, PlatformInfo platformInfo) {
    GoParseCommand commandOrNull;
    try {
      commandOrNull = new GoParseCommand(workDir, platformInfo);
      isInitialized.set(true);
    } catch (InitializationException e) {
      LOG.warn("Go converter initialization failed: {}", e.getMessage());
      commandOrNull = null;
    }
    this.command = commandOrNull;
  }

  // Visible for testing
  public GoConverter(GoParseCommand command) {
    this.command = command;
    this.isInitialized.set(true);
  }

  @Override
  public Map<String, TreeOrError> parse(Map<String, String> filenameToContentMap, String moduleName) {
    Map<String, TreeOrError> result = new HashMap<>(filenameToContentMap.size());
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
    try {
      var json = command.executeGoParseCommand(filesToParse, moduleName);
      result.putAll(JsonTree.fromJson(json));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParseException("Go executable interrupted: " + e.getMessage(), null, e);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
    return result;
  }

  @Override
  public void debugTypeCheck() {
    command.debugTypeCheck();
  }

  @Override
  public boolean isInitialized() {
    return isInitialized.get();
  }
}
