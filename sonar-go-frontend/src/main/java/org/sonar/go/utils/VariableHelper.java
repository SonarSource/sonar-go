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
package org.sonar.go.utils;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;

public class VariableHelper {

  private VariableHelper() {
  }

  /**
   * Return the list of variables defined in the given variable declaration.
   * Those variables are stored in a list of {@link Variable} objects, each containing the variable identifier and its value.
   * In case of {@code var a, b = 1, 2}, it will associate {@code a} to {@code 1} and {@code b} to {@code 2}.
   * In case of {@code var a, b}, it will associate both {@code a} and {@code b} to {@code null}.
   * In case of {@code var a, b = foo()}, it will associate both {@code a} and {@code b} to {@code foo()}.
   */
  public static List<Variable> getVariables(VariableDeclarationTree variableDeclarationTree) {
    var result = new ArrayList<Variable>();
    for (int i = 0; i < variableDeclarationTree.identifiers().size(); i++) {
      IdentifierTree name = variableDeclarationTree.identifiers().get(i);

      Tree value;
      if (variableDeclarationTree.initializers().isEmpty()) {
        value = null;
      } else if (i < variableDeclarationTree.initializers().size()) {
        value = variableDeclarationTree.initializers().get(i);
      } else {
        value = variableDeclarationTree.initializers().get(0);
      }

      result.add(new Variable(name, variableDeclarationTree.type(), value));
    }
    return result;
  }

  public record Variable(IdentifierTree identifier, @Nullable Tree type, @Nullable Tree value) {
  }
}
