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
package org.sonar.go.checks;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S103")
public class TooLongLineCheck implements GoCheck {
  private static final int DEFAULT_MAXIMUM_LINE_LENGTH = 120;
  private static final String DEFAULT_MAXIMUM_LINE_LENGTH_VALUE = "" + DEFAULT_MAXIMUM_LINE_LENGTH;
  private static final String MESSAGE = "Split this {0} characters long line (which is greater than {1} authorized).";

  @RuleProperty(
    key = "maximumLineLength",
    description = "The maximum authorized line length.",
    defaultValue = DEFAULT_MAXIMUM_LINE_LENGTH_VALUE)
  int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENGTH;

  private final Map<Integer, TextRange> longLines = new HashMap<>();
  private final Set<Integer> linesWithTree = new HashSet<>();
  private final Set<Integer> linesLongUrlComments = new HashSet<>();

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (this::handleTopLevelTree));

    init.register(ImportSpecificationTree.class, (ctx, tree) -> longLines.remove(tree.textRange().start().line()));

    init.register(Tree.class, (ctx, tree) -> {
      var startLine = tree.textRange().start().line();
      linesWithTree.add(startLine);
    });

    init.registerOnLeave((ctx, tree) -> longLines.entrySet().stream()
      // remove lines that contains code Trees, to raise on long tailing URL comments
      .filter(entry -> !containsOnlyLongUrlComment(entry.getKey()))
      .forEach(entry -> {
        var message = MessageFormat.format(MESSAGE, entry.getValue().end().lineOffset(), maximumLineLength);
        ctx.reportIssue(entry.getValue(), message);
      }));
  }

  private boolean containsOnlyLongUrlComment(Integer lineNumber) {
    return linesLongUrlComments.contains(lineNumber) && !linesWithTree.contains(lineNumber);
  }

  private void handleTopLevelTree(CheckContext ctx, TopLevelTree topLevelTree) {
    longLines.clear();
    linesWithTree.clear();
    linesLongUrlComments.clear();
    var lines = ctx.fileContent().split("\r\n|\n|\r", -1);
    IntStream.range(0, lines.length)
      .filter(lineNumber -> lines[lineNumber].length() > maximumLineLength)
      .forEach(lineNumber -> {
        var lineLength = lines[lineNumber].length();
        var longLine = getLineRange(lineNumber + 1, lineLength);
        longLines.put(lineNumber + 1, longLine);
      });

    topLevelTree.allComments().forEach(comment -> {
      if (comment.textRange().end().lineOffset() > maximumLineLength) {
        var commentText = comment.contentText().trim();
        if (isOnlyUrl(commentText)) {
          var lineNumber = comment.textRange().start().line();
          linesLongUrlComments.add(lineNumber);
        }
      }
    });
  }

  private static boolean isOnlyUrl(String commentText) {
    return commentText.contains("://") && !(commentText.contains(" ") || commentText.contains("\t"));
  }

  private static TextRange getLineRange(int lineNumber, int lineLength) {
    return new TextRangeImpl(
      new TextPointerImpl(lineNumber, 0),
      new TextPointerImpl(lineNumber, lineLength));
  }
}
