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
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S101")
public class BadClassNameCheck implements SlangCheck {

  private static final String DEFAULT_FORMAT = "^[A-Z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the class names against.",
    defaultValue = DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  @Override
  public void initialize(InitContext init) {
    Pattern pattern = Pattern.compile(format);
    init.register(ClassDeclarationTree.class, (ctx, tree) -> {
      IdentifierTree identifier = tree.identifier();
      if (identifier != null && !pattern.matcher(identifier.name()).matches()) {
        String message = String.format(
          "Rename class \"%s\" to match the regular expression %s.",
          identifier.name(), format);
        ctx.reportIssue(identifier, message);
      }
    });
  }

}
