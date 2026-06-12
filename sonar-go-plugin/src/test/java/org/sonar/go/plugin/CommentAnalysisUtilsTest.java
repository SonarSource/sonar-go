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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.CommentImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.TextRange;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAnalysisUtilsTest {

  private static final String CODE = """
    // NOSONAR
    def fun() {
    /* todo */
    }""";
  private static final TextRange CODE_TEXT_RANGE = new TextRangeImpl(1, 0, 4, 0);

  @Test
  void testNosonarComment() {
    TextRange noSonarCommentTextRange = new TextRangeImpl(1, 2, 1, 10);
    Comment nosonarComment = new CommentImpl(CODE, " NOSONAR ", CODE_TEXT_RANGE, noSonarCommentTextRange);
    assertThat(CommentAnalysisUtils.isNosonarComment(nosonarComment)).isTrue();
  }

  @Test
  void testNotNosonarComment() {
    TextRange todoCommentTextRange = new TextRangeImpl(3, 2, 3, 8);
    Comment todoComment = new CommentImpl(CODE, " todo ", CODE_TEXT_RANGE, todoCommentTextRange);
    assertThat(CommentAnalysisUtils.isNosonarComment(todoComment)).isFalse();
  }

  @Test
  void testNolintRecognisedForms() {
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:all"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:gosec"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:errcheck,gosec"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:staticcheck,gosec,unused"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:errcheck,all"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint // bare with reason"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:gosec // explanation"))).isTrue();
  }

  @Test
  void testNolintRejectsUnhonoredLinters() {
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:errcheck"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:staticcheck"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:unconvert"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:revive // unexported-return."))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:unconvert,nolintlint"))).isFalse();
  }

  @Test
  void testNolintToleratesWhitespaceAfterSlashes() {
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("// nolint"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("// nolint:gosec"))).isTrue();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//   nolint:errcheck,gosec"))).isTrue();
  }

  @Test
  void testNolintRejectsUppercase() {
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//NoLint"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolint:GoSec"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//NOLINT"))).isFalse();
  }

  @Test
  void testNolintRejectsMidComment() {
    assertThat(CommentAnalysisUtils.isNolintDirective(
      nolintComment("// this is not a directive, just a comment about nolint:gosec"))).isFalse();
  }

  @Test
  void testNolintRejectsMalformedKeyword() {
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//nolintgosec"))).isFalse();
    assertThat(CommentAnalysisUtils.isNolintDirective(nolintComment("//no-lint:gosec"))).isFalse();
  }

  private static Comment nolintComment(String text) {
    String contentText = text.startsWith("//") ? text.substring(2) : text;
    TextRange range = new TextRangeImpl(1, 0, 1, text.length());
    TextRange contentRange = new TextRangeImpl(1, 2, 1, text.length());
    return new CommentImpl(text, contentText, range, contentRange);
  }

  @Test
  void testAddNonBlankSingleLineComment() {
    testAddCommentLines("single line comment",
      new TextRangeImpl(2, 2, 2, 17),
      Set.of(2));
  }

  @Test
  void testAddBlankSingleLineComment() {
    testAddCommentLines("*#=|",
      new TextRangeImpl(2, 2, 2, 6),
      Set.of());
  }

  @Test
  void testAddNonBlankMultiLineComment() {
    testAddCommentLines("multi \nline \ncomment",
      new TextRangeImpl(7, 2, 9, 7),
      Set.of(7, 8, 9));
  }

  @Test
  void testAddBlankMultiLineComment() {
    testAddCommentLines("  \n#= \n*|",
      new TextRangeImpl(7, 2, 9, 4),
      Set.of());
  }

  private void testAddCommentLines(String comment, TextRange commentTextRange, Set<Integer> expectedCommentLines) {
    assertThat(CommentAnalysisUtils.findNonEmptyCommentLines(commentTextRange, comment)).containsAll(expectedCommentLines);
  }
}
