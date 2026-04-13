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

import java.util.ArrayList;
import java.util.List;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  // Rules that define the scope as 'Main' in rspec definition
  private static final List<Class<?>> MAIN_CHECKS = List.of(
    CodeAfterJumpGoCheck.class,
    DuplicateBranchGoCheck.class,
    EmptyCommentCheck.class,
    EmptyFunctionCheck.class,
    StringLiteralDuplicatedCheck.class,
    SwitchCaseTooBigCheck.class,
    TooLongFunctionCheck.class);

  // Rules that define the scope as 'All' in rspec definition
  private static final List<Class<?>> MAIN_AND_TEST_CHECKS = List.of(
    AllBranchesIdenticalCheck.class,
    BadFunctionNameCheck.class,
    BooleanInversionCheck.class,
    BooleanLiteralCheck.class,
    DuplicatedFunctionImplementationCheck.class,
    ElseIfWithoutElseCheck.class,
    EmptyBlockCheck.class,
    FileHeaderCheck.class,
    FixMeCommentCheck.class,
    FunctionCognitiveComplexityCheck.class,
    IdenticalBinaryOperandCheck.class,
    IdenticalConditionsCheck.class,
    IfConditionalAlwaysTrueOrFalseCheck.class,
    SwitchWithoutDefaultCheck.class,
    NestedSwitchCheck.class,
    OctalValuesCheck.class,
    OneStatementPerLineGoCheck.class,
    ParsingErrorCheck.class,
    RedundantParenthesesCheck.class,
    SelfAssignmentCheck.class,
    TodoCommentCheck.class,
    TooComplexExpressionCheck.class,
    TooDeeplyNestedStatementsCheck.class,
    TooLongLineCheck.class,
    TooManyCasesCheck.class,
    TooManyLinesOfCodeFileCheck.class,
    TooManyParametersCheck.class,
    VariableAndParameterNameCheck.class,
    WrongAssignmentOperatorCheck.class);

  public static List<Class<?>> mainChecks() {
    return new ArrayList<>(MAIN_CHECKS);
  }

  public static List<Class<?>> mainAndTestChecks() {
    return new ArrayList<>(MAIN_AND_TEST_CHECKS);
  }

  public static List<Class<?>> allChecks() {
    ArrayList<Class<?>> allChecks = new ArrayList<>(GoCheckList.mainAndTestChecks());
    allChecks.addAll(GoCheckList.mainChecks());
    return allChecks;
  }
}
