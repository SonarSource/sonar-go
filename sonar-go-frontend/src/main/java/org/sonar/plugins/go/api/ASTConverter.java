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
package org.sonar.plugins.go.api;

import java.util.Map;

public interface ASTConverter {

  /**
   * Parses the given content and returns a map of file names to their corresponding AST trees.
   *
   * @param content the content to parse
   * @param filename the name of the current file being parsed
   * @return a map where keys are file names and values are their corresponding AST trees
   * @throws ParseException if an error occurs during parsing
   */
  Map<String, Tree> parse(String content, String filename);

  default void terminate() {
    // Nothing to do by default
  }

}
