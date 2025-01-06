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
package org.sonar.go.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.go.checks.AllBranchesIdenticalCheck;
import org.sonar.go.checks.BadFunctionNameCheck;
import org.sonar.go.checks.BooleanInversionCheck;
import org.sonar.go.checks.BooleanLiteralCheck;
import org.sonar.go.checks.CodeAfterJumpGoCheck;
import org.sonar.go.checks.DuplicateBranchGoCheck;
import org.sonar.go.checks.DuplicatedFunctionImplementationCheck;
import org.sonar.go.checks.ElseIfWithoutElseCheck;
import org.sonar.go.checks.EmptyBlockCheck;
import org.sonar.go.checks.EmptyCommentCheck;
import org.sonar.go.checks.EmptyFunctionCheck;
import org.sonar.go.checks.FileHeaderCheck;
import org.sonar.go.checks.FixMeCommentCheck;
import org.sonar.go.checks.FunctionCognitiveComplexityCheck;
import org.sonar.go.checks.GoCheckList;
import org.sonar.go.checks.HardcodedCredentialsCheck;
import org.sonar.go.checks.HardcodedIpCheck;
import org.sonar.go.checks.IdenticalBinaryOperandCheck;
import org.sonar.go.checks.IdenticalConditionsCheck;
import org.sonar.go.checks.IfConditionalAlwaysTrueOrFalseCheck;
import org.sonar.go.checks.MatchCaseTooBigCheck;
import org.sonar.go.checks.MatchWithoutElseCheck;
import org.sonar.go.checks.NestedMatchCheck;
import org.sonar.go.checks.OctalValuesCheck;
import org.sonar.go.checks.OneStatementPerLineGoCheck;
import org.sonar.go.checks.ParsingErrorCheck;
import org.sonar.go.checks.RedundantParenthesesCheck;
import org.sonar.go.checks.SelfAssignmentCheck;
import org.sonar.go.checks.StringLiteralDuplicatedCheck;
import org.sonar.go.checks.TodoCommentCheck;
import org.sonar.go.checks.TooComplexExpressionCheck;
import org.sonar.go.checks.TooDeeplyNestedStatementsCheck;
import org.sonar.go.checks.TooLongFunctionCheck;
import org.sonar.go.checks.TooLongLineCheck;
import org.sonar.go.checks.TooManyCasesCheck;
import org.sonar.go.checks.TooManyLinesOfCodeFileCheck;
import org.sonar.go.checks.TooManyParametersCheck;
import org.sonar.go.checks.VariableAndParameterNameCheck;
import org.sonar.go.checks.WrongAssignmentOperatorCheck;

import static org.assertj.core.api.Assertions.assertThat;

class GoCheckListTest {

  @Test
  void shouldVerifyChecksSize() {
    assertThat(GoCheckList.checks()).hasSize(38);
  }

  @Test
  void shouldContainParsingErrorCheck() {
    assertThat(GoCheckList.checks()).contains(ParsingErrorCheck.class);
  }

  @Test
  void shouldContainsClasses() {
    assertThat(GoCheckList.checks()).containsOnly(

      AllBranchesIdenticalCheck.class,
      BadFunctionNameCheck.class,
      BooleanInversionCheck.class,
      BooleanLiteralCheck.class,
      CodeAfterJumpGoCheck.class,
      DuplicateBranchGoCheck.class,
      DuplicatedFunctionImplementationCheck.class,
      ElseIfWithoutElseCheck.class,
      EmptyBlockCheck.class,
      EmptyCommentCheck.class,
      EmptyFunctionCheck.class,
      FileHeaderCheck.class,
      FixMeCommentCheck.class,
      FunctionCognitiveComplexityCheck.class,
      HardcodedCredentialsCheck.class,
      HardcodedIpCheck.class,
      IdenticalBinaryOperandCheck.class,
      IdenticalConditionsCheck.class,
      IfConditionalAlwaysTrueOrFalseCheck.class,
      MatchCaseTooBigCheck.class,
      MatchWithoutElseCheck.class,
      NestedMatchCheck.class,
      OctalValuesCheck.class,
      OneStatementPerLineGoCheck.class,
      ParsingErrorCheck.class,
      RedundantParenthesesCheck.class,
      SelfAssignmentCheck.class,
      StringLiteralDuplicatedCheck.class,
      TodoCommentCheck.class,
      TooComplexExpressionCheck.class,
      TooDeeplyNestedStatementsCheck.class,
      TooLongFunctionCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeFileCheck.class,
      TooManyCasesCheck.class,
      TooManyParametersCheck.class,
      VariableAndParameterNameCheck.class,
      WrongAssignmentOperatorCheck.class);
  }
}
