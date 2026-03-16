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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S100")
public class BadFunctionNameCheck implements GoCheck {

  public static final String GO_NAMING_DEFAULT_FOR_TESTS = "^(_|[a-zA-Z0-9_]+)$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the function names against in main files.",
    defaultValue = GoChecksConstants.GO_NAMING_DEFAULT)
  public String format = GoChecksConstants.GO_NAMING_DEFAULT;

  @RuleProperty(
    key = "formatForTests",
    description = "Regular expression used to check the function names against in test files.",
    defaultValue = GO_NAMING_DEFAULT_FOR_TESTS)
  public String formatForTests = GO_NAMING_DEFAULT_FOR_TESTS;

  @Override
  public void initialize(InitContext init) {
    Pattern pattern = Pattern.compile(format);
    Pattern testPattern = Pattern.compile(formatForTests);
    init.register(FunctionDeclarationTree.class, (ctx, fnDeclarationTree) -> {
      IdentifierTree name = fnDeclarationTree.name();
      if (name != null) {
        Pattern activePattern = ctx.isTestFile() ? testPattern : pattern;
        String activeFormat = ctx.isTestFile() ? formatForTests : format;
        if (!activePattern.matcher(name.name()).matches()) {
          ctx.reportIssue(name, message(name.name(), activeFormat));
        }
      }
    });
  }

  private static String message(String name, String appliedFormat) {
    return "Rename function \"" + name + "\" to match the regular expression " + appliedFormat;
  }
}
