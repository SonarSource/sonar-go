/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GeneratedCodeDetector;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S117")
public class VariableAndParameterNameCheck implements GoCheck {

  private final Map<String, Boolean> generatedFiles = new HashMap<>();

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the names against.",
    defaultValue = GoChecksConstants.GO_NAMING_DEFAULT)
  public String format = GoChecksConstants.GO_NAMING_DEFAULT;

  @Override
  public void initialize(InitContext init) {
    var pattern = Pattern.compile(format);

    init.register(TopLevelTree.class, (ctx, tree) -> generatedFiles.clear());

    init.register(VariableDeclarationTree.class, (ctx, tree) -> {
      if (ctx.ancestors().stream().anyMatch(FunctionDeclarationTree.class::isInstance) && !isGeneratedFile(ctx)) {
        for (var identifier : tree.identifiers()) {
          check(pattern, ctx, identifier, "local variable");
        }
      }
    });

    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (!isGeneratedFile(ctx)) {
        tree.formalParameters().stream()
          .filter(ParameterTree.class::isInstance)
          .map(ParameterTree.class::cast)
          .forEach(param -> check(pattern, ctx, param.identifier(), "parameter"));
      }
    });
  }

  private void check(Pattern pattern, CheckContext ctx, IdentifierTree identifier, String variableKind) {
    if (!pattern.matcher(identifier.name()).matches()) {
      var message = String.format("Rename this %s to match the regular expression \"%s\".", variableKind, this.format);
      ctx.reportIssue(identifier, message);
    }
  }

  /**
   * Determines whether this rule should ignore the current generated source file.
   * @param context the current check context
   * @return {@code true} when the current file is generated
   */
  boolean isGeneratedFile(CheckContext context) {
    if (context.filename().endsWith("_templ.go")) {
      return true;
    }
    return generatedFiles.computeIfAbsent(
      context.inputFile().key(),
      ignored -> GeneratedCodeDetector.isGenerated(context.fileContent()));
  }

}
