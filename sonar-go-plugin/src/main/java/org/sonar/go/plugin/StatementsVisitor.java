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

import org.sonar.go.api.BlockTree;
import org.sonar.go.api.ClassDeclarationTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.ImportDeclarationTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.PackageDeclarationTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.visitors.TreeContext;
import org.sonar.go.visitors.TreeVisitor;

public class StatementsVisitor extends TreeVisitor<TreeContext> {

  private int statements;

  public StatementsVisitor() {

    register(BlockTree.class, (ctx, tree) -> tree.statementOrExpressions().forEach(stmt -> {
      if (!isDeclaration(stmt)) {
        statements++;
      }
    }));

    register(TopLevelTree.class, (ctx, tree) -> tree.declarations().forEach(decl -> {
      if (!isDeclaration(decl) && !isNative(decl) && !isBlock(decl)) {
        statements++;
      }
    }));
  }

  public int statements(Tree tree) {
    statements = 0;
    scan(new TreeContext(), tree);
    return statements;
  }

  @Override
  protected void before(TreeContext ctx, Tree root) {
    statements = 0;
  }

  private static boolean isDeclaration(Tree tree) {
    return tree instanceof ClassDeclarationTree
      || tree instanceof FunctionDeclarationTree
      || tree instanceof PackageDeclarationTree
      || tree instanceof ImportDeclarationTree;
  }

  private static boolean isNative(Tree tree) {
    return tree instanceof NativeTree;
  }

  private static boolean isBlock(Tree tree) {
    return tree instanceof BlockTree;
  }

}
