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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

@Rule(key = "S1479")
public class TooManyCasesCheck implements GoCheck {

  private static final int DEFAULT_MAX = 30;

  @RuleProperty(
    key = "maximum",
    description = "Maximum number of branches",
    defaultValue = "" + DEFAULT_MAX)
  public int maximum = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, tree) -> {
      var numberOfCases = tree.cases().size();
      if (numberOfCases > maximum) {
        Token matchKeyword = tree.keyword();
        String message = String.format(
          "Reduce the number of %s branches from %s to at most %s.",
          matchKeyword.text(),
          numberOfCases,
          maximum);
        List<SecondaryLocation> secondaryLocations = tree.cases().stream()
          .map(matchCase -> new SecondaryLocation(matchCase.rangeToHighlight(), null))
          .toList();
        ctx.reportIssue(matchKeyword, message, secondaryLocations);
      }
    });
  }

}
