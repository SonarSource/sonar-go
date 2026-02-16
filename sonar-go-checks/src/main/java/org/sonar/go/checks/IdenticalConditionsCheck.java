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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.utils.ExpressionUtils;
import org.sonar.go.utils.SyntacticEquivalence;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IfTree;
import org.sonar.plugins.go.api.MatchCaseTree;
import org.sonar.plugins.go.api.MatchTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

import static org.sonar.go.utils.ExpressionUtils.extractActualCondition;
import static org.sonar.go.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1862")
public class IdenticalConditionsCheck implements GoCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, tree) -> checkConditions(ctx, collectConditions(tree)));
    init.register(IfTree.class, (ctx, tree) -> {
      if (!(ctx.parent() instanceof IfTree)) {
        checkConditionsWithSymbols(ctx, collectConditionsWithSymbols(tree, new ArrayList<>()));
      }
    });
  }

  private static List<Tree> collectConditions(MatchTree matchTree) {
    return matchTree.cases().stream()
      .map(MatchCaseTree::expression)
      .filter(Objects::nonNull)
      .map(ExpressionUtils::skipParentheses)
      .toList();
  }

  private static List<ConditionWithSymbols> collectConditionsWithSymbols(IfTree ifTree, List<ConditionWithSymbols> list) {
    var condition = ifTree.condition();
    var actualCondition = extractActualCondition(condition);
    var cleanCondition = skipParentheses(actualCondition);
    boolean hasShortStatement = condition != actualCondition;

    Map<String, Symbol> modifiedVariables;
    if (hasShortStatement) {
      modifiedVariables = collectModifiedVariablesInInit(condition, actualCondition);
    } else {
      modifiedVariables = Map.of();
    }

    list.add(new ConditionWithSymbols(cleanCondition, modifiedVariables, hasShortStatement));

    var elseBranch = ifTree.elseBranch();
    if (elseBranch instanceof IfTree elseIfBranch) {
      return collectConditionsWithSymbols(elseIfBranch, list);
    }
    return list;
  }

  /**
   * Collect variables modified in the init statement (declared or reassigned).
   * For {@code if x := foo(); x > 3}, collects x (declaration).
   * For {@code if x = foo(); x > 3}, collects x (reassignment).
   * This helps detect when conditions might have different values despite being syntactically identical.
   */
  private static Map<String, Symbol> collectModifiedVariablesInInit(Tree initAndCondNode, Tree actualCondition) {
    var identifiersInCondition = actualCondition.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .collect(Collectors.toSet());

    // Identifiers in init statement = all identifiers - condition identifiers
    // These are variables being declared or reassigned in the init block
    return initAndCondNode.descendants()
      // Check only identifiers
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      // Because condition AST node can be a complex node for a short-hand statement, we need to keep only identifiers in the init part
      .filter(id -> !identifiersInCondition.contains(id))
      .filter(id -> id.symbol() != null)
      .collect(Collectors.toMap(
        IdentifierTree::name,
        IdentifierTree::symbol,
        (existing, replacement) -> existing));
  }

  private static void checkConditions(CheckContext ctx, List<Tree> conditions) {
    for (var group : SyntacticEquivalence.findDuplicatedGroups(conditions)) {
      var original = group.get(0);
      group.stream().skip(1)
        .forEach(duplicated -> {
          var originalRange = original.metaData().textRange();
          ctx.reportIssue(
            duplicated,
            "This condition duplicates the one on line " + originalRange.start().line() + ".",
            new SecondaryLocation(originalRange, "Original"));
        });
    }
  }

  private static void checkConditionsWithSymbols(CheckContext ctx, List<ConditionWithSymbols> conditionInfos) {
    var conditions = conditionInfos.stream()
      .map(ConditionWithSymbols::condition)
      .toList();

    for (var group : SyntacticEquivalence.findDuplicatedGroups(conditions)) {
      var original = group.get(0);

      group.stream().skip(1)
        .forEach(duplicated -> {
          var duplicatedIndex = conditions.indexOf(duplicated);
          var duplicatedInfo = conditionInfos.get(duplicatedIndex);

          // Skip reporting if the duplicated branch's init statement modifies a variable used in its condition
          // This handles shadowing, reassignment, and one-sided modification cases
          boolean hasModifiedVariables = duplicatedInfo.hasShortStatement() &&
            hasModifiedVariablesAffectingCondition(duplicated, duplicatedInfo.identifierSymbols());

          if (hasModifiedVariables) {
            return;
          }

          var originalRange = original.metaData().textRange();
          ctx.reportIssue(
            duplicated,
            "This condition duplicates the one on line " + originalRange.start().line() + ".",
            new SecondaryLocation(originalRange, "Original"));
        });
    }
  }

  /**
   * Check if variables modified in the duplicated branch's init statement affect its condition evaluation.
   * Returns true if the condition might evaluate differently despite being syntactically identical to the original.
   * <br/>
   * We only check the duplicated branch: if any variable modified in its init statement is also used in
   * its condition, then the conditions should not be reported as duplicates.
   * <br/>
   * Examples:
   * <ul>
   * <li>if x := 5; x > 3 vs if x := 10; x > 3 (skip - duplicated modifies x used in its condition)</li>
   * <li>if x := 5; x > 3 vs if y := 10; x > 3 (report - duplicated modifies y, not x)</li>
   * <li>if w > 3 vs if w = 10; w > 3 (skip - duplicated modifies w used in its condition)</li>
   * </ul>
   */
  private static boolean hasModifiedVariablesAffectingCondition(
    Tree duplicatedCondition,
    Map<String, Symbol> duplicatedModifiedVars) {

    // Get symbols of variables used in the duplicated condition
    var duplicatedConditionSymbols = Stream.concat(Stream.of(duplicatedCondition), duplicatedCondition.descendants())
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::symbol)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    // Check if any variable modified in the duplicated init is used in the duplicated condition
    return duplicatedModifiedVars.values().stream()
      .anyMatch(duplicatedConditionSymbols::contains);
  }

  private record ConditionWithSymbols(Tree condition, Map<String, Symbol> identifierSymbols, boolean hasShortStatement) {
  }
}
