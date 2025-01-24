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
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;

@Rule(key = "S104")
public class TooManyLinesOfCodeFileCheck implements GoCheck {

  private static final int DEFAULT_MAX = 750;
  private static final String DEFAULT_MAX_VALUE = "" + DEFAULT_MAX;

  @RuleProperty(
    key = "Max",
    description = "Maximum authorized lines of code in a file.",
    defaultValue = DEFAULT_MAX_VALUE)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      var numberOfLinesOfCode = tree.metaData().linesOfCode().size();
      if (numberOfLinesOfCode > max) {
        String message = String.format(
          "File \"%s\" has %s lines, which is greater than %s authorized. Split it into smaller files.",
          ctx.filename(), numberOfLinesOfCode, max);
        ctx.reportFileIssue(message);
      }
    });
  }

}
