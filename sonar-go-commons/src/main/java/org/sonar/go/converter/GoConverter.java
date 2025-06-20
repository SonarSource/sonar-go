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
import javax.annotation.Nullable;
import org.sonar.go.persistence.JsonTree;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TreeOrError;

public class GoConverter implements ASTConverter {

  public static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private final Command command;

  public GoConverter(File workDir) {
    this(DefaultCommand.createCommand(workDir));
  }

  // Visible for testing
  public GoConverter(@Nullable Command command) {
    this.command = command;
  }

  @Override
  public Map<String, TreeOrError> parse(Map<String, String> filenameToContentMap) {
    for (String content : filenameToContentMap.values()) {
      if (content.length() > MAX_SUPPORTED_SOURCE_FILE_SIZE) {
        throw new ParseException("The file size is too big and should be excluded," +
          " its size is " + content.length() + " (maximum allowed is " + MAX_SUPPORTED_SOURCE_FILE_SIZE + " bytes)");
      }
    }
    try {
      var json = command.executeCommand(filenameToContentMap);
      return JsonTree.fromJson(json);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParseException("Go executable interrupted: " + e.getMessage(), null, e);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }
}
