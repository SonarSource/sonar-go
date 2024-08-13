/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.plugin;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextRangeImpl;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAnalysisUtilsTest {

  private final static String CODE = "// NOSONAR \n" +
    "def fun() { \n" +
    "/* todo */ \n" +
    "}";
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
