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
package org.sonar.go.checks;

import java.text.MessageFormat;
import org.sonar.check.Rule;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S1821")
public class NestedSwitchCheck implements GoCheck {
  private static final String MESSAGE = "Refactor the code to eliminate this nested \"{0}\".";

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, matchTree) -> ctx.ancestors().stream()
      .filter(MatchTree.class::isInstance)
      .findFirst()
      .ifPresent(parentMatch -> reportIssue(ctx, matchTree)));
  }

  private static void reportIssue(CheckContext ctx, MatchTree matchTree) {
    ctx.reportIssue(matchTree.keyword(), MessageFormat.format(MESSAGE, matchTree.keyword().text()));
  }
}
