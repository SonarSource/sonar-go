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

import java.text.MessageFormat;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;

@Rule(key = "S103")
public class TooLongLineCheck implements SlangCheck {
  private static final int DEFAULT_MAXIMUM_LINE_LENGTH = 120;
  private static final String DEFAULT_MAXIMUM_LINE_LENGTH_VALUE = "" + DEFAULT_MAXIMUM_LINE_LENGTH;

  @RuleProperty(
    key = "maximumLineLength",
    description = "The maximum authorized line length.",
    defaultValue = DEFAULT_MAXIMUM_LINE_LENGTH_VALUE)
  int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENGTH;

  private static final String MESSAGE = "Split this {0} characters long line (which is greater than {1} authorized).";

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, ((ctx, topLevelTree) -> {
      String[] lines = ctx.fileContent().split("\r\n|\n|\r", -1);
      IntStream.range(0, lines.length)
        .filter(lineNumber -> lines[lineNumber].length() > maximumLineLength)
        .forEach(lineNumber -> {
          int lineLength = lines[lineNumber].length();
          TextRange longLine = getLineRange(lineNumber + 1, lineLength);
          ctx.reportIssue(longLine, MessageFormat.format(MESSAGE, lineLength, maximumLineLength));
        });
    }));
  }

  private static TextRange getLineRange(int lineNumber, int lineLength) {
    return new TextRangeImpl(
      new TextPointerImpl(lineNumber, 0),
      new TextPointerImpl(lineNumber, lineLength));
  }
}
