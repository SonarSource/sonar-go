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

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.api.checks.CheckContext;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;

@Rule(key = "S117")
public class VariableAndParameterNameCheck implements GoCheck {

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the names against.",
    defaultValue = GoChecksConstants.GO_NAMING_DEFAULT)
  public String format = GoChecksConstants.GO_NAMING_DEFAULT;

  @Override
  public void initialize(InitContext init) {
    var pattern = Pattern.compile(format);

    init.register(VariableDeclarationTree.class, (ctx, tree) -> {
      if (ctx.ancestors().stream().anyMatch(FunctionDeclarationTree.class::isInstance)) {
        check(pattern, ctx, tree.identifier(), "local variable");
      }
    });

    init.register(FunctionDeclarationTree.class, (ctx, tree) -> tree.formalParameters().stream()
      .filter(ParameterTree.class::isInstance)
      .map(ParameterTree.class::cast)
      // TODO SONARGO-152: In go, the identifier of a ParameterTree cannot be null.
      .forEach(param -> check(pattern, ctx, param.identifier(), "parameter")));
  }

  private void check(Pattern pattern, CheckContext ctx, @Nullable IdentifierTree identifier, String variableKind) {
    if (identifier != null && !pattern.matcher(identifier.name()).matches()) {
      var message = String.format("Rename this %s to match the regular expression \"%s\".", variableKind, this.format);
      ctx.reportIssue(identifier, message);
    }
  }

}
