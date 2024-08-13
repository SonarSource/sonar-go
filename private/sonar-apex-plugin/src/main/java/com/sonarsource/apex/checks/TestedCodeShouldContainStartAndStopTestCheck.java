/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.FunctionUtils;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isTestClass;

@Rule(key = "S5394")
public class TestedCodeShouldContainStartAndStopTestCheck implements SlangCheck {

  private static final String MESSAGE =  "Add \"Test.StartTest()\" and \"Test.StopTest()\" to your test";

  private static final String START_TEST_METHOD_NAME = "StartTest";
  private static final String STOP_TEST_METHOD_NAME = "StopTest";
  private static final String TEST_OBJECT_NAME = "Test";

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classDeclarationTree) -> {
      if (!isTestClass(classDeclarationTree)) {
        return;
      }

      Map<Boolean, List<FunctionDeclarationTree>> functionDeclarations = classDeclarationTree.descendants()
        .filter(FunctionDeclarationTree.class::isInstance)
        .map(FunctionDeclarationTree.class::cast)
        .collect(Collectors.partitioningBy(ExpressionUtils::isTestFunction));

      for(FunctionDeclarationTree helperMethod: functionDeclarations.get(false)) {
        if (descendantsContainsStartAndStop(helperMethod)) {
          // Helper function containing start and stop, the code might have been refactored, we don't report any issues for this class
          return;
        }
      }

      for (FunctionDeclarationTree func: functionDeclarations.get(true)) {
        if (func.body() != null && !descendantsContainsStartAndStop(func)) {
          ctx.reportIssue(func.rangeToHighlight(), MESSAGE);
        }
      }
    });
  }

  private static boolean descendantsContainsStartAndStop(Tree tree) {
    List<FunctionInvocationTree> functionCallInBody = tree.descendants()
      .filter(FunctionInvocationTree.class::isInstance)
      .map(FunctionInvocationTree.class::cast)
      .collect(Collectors.toList());
    return containsStartAndStop(functionCallInBody);
  }

  private static boolean containsStartAndStop(List<FunctionInvocationTree> functionCallInBody) {
    boolean seenStart = false;
    boolean seenStop = false;
    for (FunctionInvocationTree f: functionCallInBody) {
      seenStart = seenStart || isStartTest(f);
      seenStop = seenStop || isStopTest(f);
    }
    return seenStart && seenStop;
  }

  private static boolean isStartTest(FunctionInvocationTree functionInvocationTree) {
    return FunctionUtils.hasFunctionCallFullNameIgnoreCase(functionInvocationTree, TEST_OBJECT_NAME, START_TEST_METHOD_NAME);
  }

  private static boolean isStopTest(FunctionInvocationTree functionInvocationTree) {
    return FunctionUtils.hasFunctionCallFullNameIgnoreCase(functionInvocationTree, TEST_OBJECT_NAME, STOP_TEST_METHOD_NAME);
  }
}
