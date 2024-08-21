/*
 * SonarSource Go
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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.text.MessageFormat;
import org.sonar.check.Rule;

@Rule(key = "S1821")
public class NestedMatchCheck implements SlangCheck {
  private static final String MESSAGE = "Refactor the code to eliminate this nested \"{0}\".";

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, matchTree) -> ctx.ancestors().stream()
      .filter(MatchTree.class::isInstance)
      .findFirst()
      .ifPresent(parentMatch ->
        ctx.reportIssue(matchTree.keyword(), MessageFormat.format(MESSAGE, matchTree.keyword().text()))
      ));
  }
}