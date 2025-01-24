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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonarsource.analyzer.commons.TokenLocation;

@Rule(key = "S1135")
public class TodoCommentCheck implements GoCheck {

  private final Pattern todoPattern = Pattern.compile("(?i)(^|[[^\\p{L}]&&\\D])(todo)($|[[^\\p{L}]&&\\D])");

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> tree.allComments().forEach(comment -> {
      var matcher = todoPattern.matcher(comment.text());
      if (matcher.find()) {
        var start = comment.textRange().start();
        var location = new TokenLocation(start.line(), start.lineOffset(), comment.text().substring(0, matcher.start(2)));
        var todoRange = new TextRangeImpl(
          new TextPointerImpl(location.endLine(), location.endLineOffset()),
          new TextPointerImpl(location.endLine(), location.endLineOffset() + 4));
        ctx.reportIssue(todoRange, "Complete the task associated to this TODO comment.");
      }
    }));
  }
}
