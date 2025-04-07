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
package org.sonar.go.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S138")
public class TooLongFunctionCheck implements GoCheck {

  private static final int DEFAULT_MAX = 120;
  private static final String DEFAULT_MAX_VALUE = "" + DEFAULT_MAX;

  @RuleProperty(
    key = "max",
    description = "Maximum authorized lines of code in a function",
    defaultValue = DEFAULT_MAX_VALUE)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      var body = tree.body();
      if (body == null) {
        return;
      }
      var numberOfLinesOfCode = body.metaData().linesOfCode().size();
      if (numberOfLinesOfCode > max) {
        var message = String.format(
          "This function has %s lines of code, which is greater than the %s authorized. Split it into smaller functions.",
          numberOfLinesOfCode,
          max);
        ctx.reportIssue(tree.rangeToHighlight(), message);
      }
    });
  }
}
