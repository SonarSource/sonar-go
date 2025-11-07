/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.plugins.go.api.TextPointer;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonarsource.analyzer.commons.TokenLocation;

@Rule(key = "S1134")
public class FixMeCommentCheck implements GoCheck {

  private final Pattern fixMePattern = Pattern.compile("(?i)(^|[[^\\p{L}]&&\\D])(fixme)($|[[^\\p{L}]&&\\D])");

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> tree.allComments().forEach(comment -> {
      Matcher matcher = fixMePattern.matcher(comment.text());
      if (matcher.find()) {
        TextPointer start = comment.textRange().start();
        TokenLocation location = new TokenLocation(
          start.line(),
          start.lineOffset(),
          comment.text().substring(0, matcher.start(2)));
        TextRange fixMeRange = new TextRangeImpl(
          new TextPointerImpl(location.endLine(), location.endLineOffset()),
          new TextPointerImpl(location.endLine(), location.endLineOffset() + 5));
        ctx.reportIssue(fixMeRange, "Take the required action to fix the issue indicated by this \"FIXME\" comment.");
      }
    }));
  }

}
