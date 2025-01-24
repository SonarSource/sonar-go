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
package org.sonar.go.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.go.api.Comment;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.visitors.TreeVisitor;

public class SkipNoSonarLinesVisitor extends TreeVisitor<InputFileContext> {

  private final NoSonarFilter noSonarFilter;

  private Set<Integer> noSonarLines;

  public SkipNoSonarLinesVisitor(NoSonarFilter noSonarFilter) {
    this.noSonarFilter = noSonarFilter;

    register(TopLevelTree.class, (ctx, tree) -> {
      List<Tree> declarations = tree.declarations();
      int firstTokenLine = declarations.isEmpty() ? tree.textRange().end().line() : declarations.get(0).textRange().start().line();
      tree.allComments()
        .forEach(comment -> noSonarLines.addAll(findNoSonarCommentLines(comment, firstTokenLine)));
    });
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    noSonarLines = new HashSet<>();
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    noSonarFilter.noSonarInFile(ctx.inputFile, noSonarLines);
  }

  private static Set<Integer> findNoSonarCommentLines(Comment comment, int firstTokenLine) {
    boolean isFileHeader = comment.textRange().end().line() < firstTokenLine;

    if (!isFileHeader && CommentAnalysisUtils.isNosonarComment(comment)) {
      return CommentAnalysisUtils.findNonEmptyCommentLines(comment.contentRange(), comment.contentText());
    }

    return Set.of();
  }
}
