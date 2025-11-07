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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.go.checks.complexity.CognitiveComplexity;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

@Rule(key = "S3776")
public class FunctionCognitiveComplexityCheck implements GoCheck {

  private static final int DEFAULT_THRESHOLD = 15;

  @RuleProperty(
    key = "threshold",
    description = "The maximum authorized complexity.",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.name() == null) {
        return;
      }

      CognitiveComplexity complexity = new CognitiveComplexity(tree);
      if (complexity.value() > threshold) {
        String message = String.format(
          "Refactor this method to reduce its Cognitive Complexity from %s to the %s allowed.",
          complexity.value(),
          threshold);
        List<SecondaryLocation> secondaryLocations = complexity.increments().stream()
          .map(FunctionCognitiveComplexityCheck::secondaryLocation)
          .toList();
        Double gap = (double) complexity.value() - threshold;
        ctx.reportIssue(tree::rangeToHighlight, message, secondaryLocations, gap);
      }
    });
  }

  private static SecondaryLocation secondaryLocation(CognitiveComplexity.Increment increment) {
    int nestingLevel = increment.nestingLevel();
    String message = "+" + (nestingLevel + 1);
    if (nestingLevel > 0) {
      message += " (incl " + nestingLevel + " for nesting)";
    }
    return new SecondaryLocation(increment.token().textRange(), message);
  }

}
