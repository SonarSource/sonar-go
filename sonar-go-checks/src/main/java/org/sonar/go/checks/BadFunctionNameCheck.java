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
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.InitContext;

@Rule(key = "S100")
public class BadFunctionNameCheck implements GoCheck {

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the function names against.",
    defaultValue = GoChecksConstants.GO_NAMING_DEFAULT)
  public String format = GoChecksConstants.GO_NAMING_DEFAULT;

  private String message(String name) {
    return "Rename function \"" + name + "\" to match the regular expression " + format;
  }

  @Override
  public void initialize(InitContext init) {
    Pattern pattern = Pattern.compile(format);
    init.register(FunctionDeclarationTree.class, (ctx, fnDeclarationTree) -> {
      IdentifierTree name = fnDeclarationTree.name();
      if (!fnDeclarationTree.isConstructor() && name != null && !pattern.matcher(name.name()).matches()) {
        ctx.reportIssue(fnDeclarationTree.name(), message(name.name()));
      }
    });
  }
}
