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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.DuplicateBranchCheck;
import org.sonarsource.slang.checks.api.CheckContext;

@Rule(key = "S1871")
public class DuplicateBranchGoCheck extends DuplicateBranchCheck {
  @Override
  protected void checkConditionalStructure(CheckContext ctx, Tree tree, ConditionalStructure conditional) {
    /*
     * If we enter a type switch, we may find branches with similar ASTs but different semantics.
     * In this case, we stop exploring the conditional structure to avoid raising FPs.
     */
    if (tree instanceof MatchTree && isTypeSwitch((MatchTree) tree)) {
      return;
    }
    super.checkConditionalStructure(ctx, tree, conditional);
  }

  private static boolean isTypeSwitch(MatchTree matchTree) {
    Tree expression = matchTree.expression();
    return expression != null && endsWithTypeSwitchGuard(expression);
  }

  private static boolean endsWithTypeSwitchGuard(Tree matchTreeExpression) {
    List<Token> tokens = matchTreeExpression.metaData().tokens();
    int size = tokens.size();
    return size >= 4 && tokens.subList(size - 4, size).stream()
      .map(Token::text)
      .collect(Collectors.joining("")).equals(".(type)");
  }
}
