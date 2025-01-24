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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S107")
public class TooManyParametersCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 7;

  @RuleProperty(
    key = "Max",
    description = "Maximum authorized number of parameters",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.formalParameters().size() > max) {
        String message = String.format(
          "This function has %s parameters, which is greater than the %s authorized.",
          tree.formalParameters().size(),
          max);
        var secondaryLocations = tree.formalParameters().stream()
          .skip(max)
          .map(SecondaryLocation::new)
          .toList();

        IdentifierTree name = tree.name();
        if (name == null) {
          ctx.reportIssue(tree, message, secondaryLocations);
        } else {
          ctx.reportIssue(name, message, secondaryLocations);
        }
      }
    });
  }
}
