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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonarsource.slang.checks.AllBranchesIdenticalCheck;
import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.BadFunctionNameCheck;
import org.sonarsource.slang.checks.BooleanInversionCheck;
import org.sonarsource.slang.checks.BooleanLiteralCheck;
import org.sonarsource.slang.checks.CodeAfterJumpCheck;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.DuplicateBranchCheck;
import org.sonarsource.slang.checks.DuplicatedFunctionImplementationCheck;
import org.sonarsource.slang.checks.ElseIfWithoutElseCheck;
import org.sonarsource.slang.checks.EmptyBlockCheck;
import org.sonarsource.slang.checks.EmptyCommentCheck;
import org.sonarsource.slang.checks.EmptyFunctionCheck;
import org.sonarsource.slang.checks.FileHeaderCheck;
import org.sonarsource.slang.checks.FixMeCommentCheck;
import org.sonarsource.slang.checks.FunctionCognitiveComplexityCheck;
import org.sonarsource.slang.checks.HardcodedCredentialsCheck;
import org.sonarsource.slang.checks.HardcodedIpCheck;
import org.sonarsource.slang.checks.IdenticalBinaryOperandCheck;
import org.sonarsource.slang.checks.IdenticalConditionsCheck;
import org.sonarsource.slang.checks.IfConditionalAlwaysTrueOrFalseCheck;
import org.sonarsource.slang.checks.MatchCaseTooBigCheck;
import org.sonarsource.slang.checks.MatchWithoutElseCheck;
import org.sonarsource.slang.checks.NestedMatchCheck;
import org.sonarsource.slang.checks.OctalValuesCheck;
import org.sonarsource.slang.checks.OneStatementPerLineCheck;
import org.sonarsource.slang.checks.ParsingErrorCheck;
import org.sonarsource.slang.checks.RedundantParenthesesCheck;
import org.sonarsource.slang.checks.SelfAssignmentCheck;
import org.sonarsource.slang.checks.StringLiteralDuplicatedCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.TodoCommentCheck;
import org.sonarsource.slang.checks.TooComplexExpressionCheck;
import org.sonarsource.slang.checks.TooDeeplyNestedStatementsCheck;
import org.sonarsource.slang.checks.TooLongFunctionCheck;
import org.sonarsource.slang.checks.TooLongLineCheck;
import org.sonarsource.slang.checks.TooManyCasesCheck;
import org.sonarsource.slang.checks.TooManyLinesOfCodeFileCheck;
import org.sonarsource.slang.checks.TooManyParametersCheck;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;
import org.sonarsource.slang.checks.VariableAndParameterNameCheck;
import org.sonarsource.slang.checks.WrongAssignmentOperatorCheck;

public class CheckList {

  // these checks should be explicitly added to 'checks' in Sensor constructor
  // and language RulesDefinition
  // this list should be maintained only for documentation purposes (keep the track of all existing checks in this class) and for testing
  static final Class[] ALL_CHECKS_WITH_LANGUAGE_CONFIG = {
    CommentedCodeCheck.class,
  };

  private CheckList() {
  }

  static List<Class<?>> allChecks() {
    return Arrays.asList(
      AllBranchesIdenticalCheck.class,
      BadClassNameCheck.class,
      BadFunctionNameCheck.class,
      BooleanInversionCheck.class,
      BooleanLiteralCheck.class,
      CodeAfterJumpCheck.class,
      CollapsibleIfStatementsCheck.class,
      DuplicateBranchCheck.class,
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
      OneStatementPerLineCheck.class,
      ParsingErrorCheck.class,
      RedundantParenthesesCheck.class,
      SelfAssignmentCheck.class,
      StringLiteralDuplicatedCheck.class,
      TabsCheck.class,
      TodoCommentCheck.class,
      TooComplexExpressionCheck.class,
      TooDeeplyNestedStatementsCheck.class,
      TooLongFunctionCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeFileCheck.class,
      TooManyCasesCheck.class,
      TooManyParametersCheck.class,
      UnusedFunctionParameterCheck.class,
      UnusedLocalVariableCheck.class,
      UnusedPrivateMethodCheck.class,
      VariableAndParameterNameCheck.class,
      WrongAssignmentOperatorCheck.class);
  }

  public static List<Class<?>> excludeChecks(Class[] blackList) {
    List<Class<?>> checks = new ArrayList<>(allChecks());
    checks.removeAll(Arrays.asList(blackList));
    return checks;
  }

}
