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
package org.sonar.go.utils;

import org.sonar.go.api.BlockTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.testing.TestGoConverter;

public class ParseUtils {
  private ParseUtils() {
  }

  public static Tree parse(String code) {
    var topLevelTree = (TopLevelTree) TestGoConverter.GO_CONVERTER.parse("""
      package main
      func main() {
        %s
      }
      """.formatted(code));
    var mainFunc = topLevelTree.declarations().get(1);
    var mainBlock = (BlockTree) mainFunc.children().get(1);
    var expressionStatement = (NativeTree) mainBlock.statementOrExpressions().get(0);
    return expressionStatement.children().get(0);
  }
}
