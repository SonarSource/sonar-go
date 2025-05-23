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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.persistence.JsonTree;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.Tree;

public class GoConverter implements ASTConverter {

  public static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private static final Logger LOG = LoggerFactory.getLogger(GoConverter.class);
  @Nullable
  private final Command command;

  public GoConverter(File workDir) {
    this(DefaultCommand.createCommand(workDir));
  }

  // Visible for testing
  public GoConverter(@Nullable Command command) {
    this.command = command;
    if (command != null) {
      LOG.debug("Go converter command: {}", command.getCommand());
    }
  }

  @Override
  public Tree parse(String content) {
    if (command == null) {
      throw new ParseException("Go converter is not initialized");
    } else if (content.length() > MAX_SUPPORTED_SOURCE_FILE_SIZE) {
      throw new ParseException("The file size is too big and should be excluded," +
        " its size is " + content.length() + " (maximum allowed is " + MAX_SUPPORTED_SOURCE_FILE_SIZE + " bytes)");
    }
    try {
      var json = command.executeCommand(content);
      return JsonTree.fromJson(json);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParseException("Go executable interrupted: " + e.getMessage(), null, e);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }
}
