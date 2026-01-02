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
package org.sonar.go.utils;

import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

public class ParseUtils {
  private ParseUtils() {
  }

  public static TopLevelTree parseFile(String code) {
    return (TopLevelTree) TestGoConverterSingleFile.parse(code);
  }

  public static BlockTree parseStatements(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverterSingleFile.parse("""
      package main
      func main() {
        %s
      }
      """.formatted(code));
    var mainFunc = topLevelTree.declarations().get(1);
    return (BlockTree) mainFunc.children().get(1);
  }

  public static Tree parse(String code) {
    var mainBlock = parseStatements(code);
    return mainBlock.statementOrExpressions().get(0);
  }
}
