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
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S100")
public class BadFunctionNameCheck implements SlangCheck {

  public static final String DEFAULT_FORMAT = "^[a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the function names against.",
    defaultValue = "^(_|[a-zA-Z0-9]+)$"
  )
  public String format = DEFAULT_FORMAT;

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
