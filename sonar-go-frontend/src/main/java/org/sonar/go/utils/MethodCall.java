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
package org.sonar.go.utils;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.Tree;

public record MethodCall(String methodFqn, List<Tree> args) {
  public static MethodCall of(FunctionInvocationTree tree) {
    var fqnMethod = treeToString(tree.memberSelect());
    return new MethodCall(fqnMethod, tree.arguments());
  }

  private static String treeToString(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return identifierTree.name();
    } else if (tree instanceof MemberSelectTree memberSelectTree) {
      return treeToString(memberSelectTree.expression()) + "." + memberSelectTree.identifier().name();
    }
    return "";
  }

  public boolean is(String methodFqn) {
    return this.methodFqn.equals(methodFqn);
  }

  public boolean is(String methodFqn, String... args) {
    for (int i = 0; i < args.length; i++) {
      if (this.args.size() <= i || !treeToString(this.args.get(i)).equals(args[i])) {
        return false;
      }
    }
    return this.methodFqn.equals(methodFqn);
  }

  @Nullable
  public Tree getArg(int index) {
    if (this.args.size() > index) {
      return this.args.get(index);
    }
    return null;
  }
}
