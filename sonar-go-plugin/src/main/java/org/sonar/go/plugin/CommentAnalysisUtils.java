/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.TextRange;

public class CommentAnalysisUtils {
  public static final String NOSONAR_PREFIX = "NOSONAR";

  // Lowercase //nolint(:<name>) suppresses go linters.
  // Known limitation: the pattern operates on stripped comment text, so `/* nolint */` block
  // comments are honored even though golangci-lint only recognises line comments.
  private static final Pattern NOLINT_PATTERN = Pattern.compile(
    "^nolint(?::([a-z0-9_,-]+))?(?:\\s.*)?$");

  private static final Set<String> HONORED_LINTERS = Set.of("all", "gosec");

  private static final boolean[] IS_NON_BLANK_CHAR_IN_COMMENTS = new boolean[127];
  static {
    for (int c = 0; c < IS_NON_BLANK_CHAR_IN_COMMENTS.length; c++) {
      IS_NON_BLANK_CHAR_IN_COMMENTS[c] = c > ' ' && "*#-=|".indexOf(c) == -1;
    }
  }

  private CommentAnalysisUtils() {
  }

  static boolean isNosonarComment(Comment comment) {
    return comment.contentText().trim().toUpperCase(Locale.ENGLISH).startsWith(NOSONAR_PREFIX);
  }

  // Initial scope: bare //nolint and lists naming `all` or `gosec` are treated as blanket
  // per-line suppression. Other linter names (`errcheck`, `staticcheck`, ...) are not honored
  // yet; wider scopes and richer per-linter mapping are tracked in SONARGO-815.
  static boolean isNolintDirective(Comment comment) {
    var matcher = NOLINT_PATTERN.matcher(comment.contentText().stripLeading());
    if (!matcher.matches()) {
      return false;
    }
    String linterList = matcher.group(1);
    if (linterList == null) {
      return true;
    }
    for (String linter : linterList.split(",")) {
      if (HONORED_LINTERS.contains(linter)) {
        return true;
      }
    }
    return false;
  }

  static Set<Integer> findNonEmptyCommentLines(TextRange range, String content) {
    Set<Integer> lineNumbers = new HashSet<>();

    int startLine = range.start().line();
    if (startLine == range.end().line()) {
      if (isNotBlank(content)) {
        lineNumbers.add(startLine);
      }
    } else {
      String[] lines = content.split("\r\n|\n|\r", -1);
      for (int i = 0; i < lines.length; i++) {
        if (isNotBlank(lines[i])) {
          lineNumbers.add(startLine + i);
        }
      }
    }

    return lineNumbers;
  }

  private static boolean isNotBlank(String line) {
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch >= IS_NON_BLANK_CHAR_IN_COMMENTS.length || IS_NON_BLANK_CHAR_IN_COMMENTS[ch]) {
        return true;
      }
    }
    return false;
  }
}
