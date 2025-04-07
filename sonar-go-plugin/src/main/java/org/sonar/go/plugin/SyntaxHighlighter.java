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

import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CONSTANT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.STRING;

public class SyntaxHighlighter extends TreeVisitor<InputFileContext> {

  private NewHighlighting newHighlighting;

  public SyntaxHighlighter() {
    register(TopLevelTree.class, (ctx, tree) -> {
      tree.allComments().forEach(
        comment -> highlight(ctx, comment.textRange(), COMMENT));
      tree.metaData().tokens().stream()
        .filter(t -> t.type() == Token.Type.KEYWORD)
        .forEach(token -> highlight(ctx, token.textRange(), KEYWORD));
    });

    register(LiteralTree.class, (ctx, tree) -> highlight(
      ctx,
      tree.metaData().textRange(),
      tree instanceof StringLiteralTree ? STRING : CONSTANT));
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    newHighlighting = ctx.sensorContext.newHighlighting()
      .onFile(ctx.inputFile);
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    newHighlighting.save();
  }

  private void highlight(InputFileContext ctx, TextRange range, TypeOfText typeOfText) {
    var textRange = ctx.textRange(range);
    if (textRange != null) {
      newHighlighting.highlight(textRange, typeOfText);
    }
  }

}
