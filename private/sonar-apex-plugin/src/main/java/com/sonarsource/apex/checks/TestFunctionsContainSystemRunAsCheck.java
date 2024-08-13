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
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isTestClass;

@Rule(key = "S5386")
public class TestFunctionsContainSystemRunAsCheck implements SlangCheck {

  private static final String MESSAGE = "Enclose your test code in \"System.runAs()\"";

  private static final String RUN_AS_BLOCK_KIND = "RunAsBlock";

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
        if (descendantsContainsRunAs(helperMethod)) {
          // Helper function containing start and stop, the code might have been refactored, we don't report any issues for this class
          return;
        }
      }

      for (FunctionDeclarationTree func: functionDeclarations.get(true)) {
        if (func.body() != null && !descendantsContainsRunAs(func)) {
          ctx.reportIssue(func.rangeToHighlight(), MESSAGE);
        }
      }
    });
  }

  private static boolean descendantsContainsRunAs(FunctionDeclarationTree fnDeclarationTree) {
    return fnDeclarationTree.descendants()
      .anyMatch(TestFunctionsContainSystemRunAsCheck::isSystemRunAs);
  }

  private static boolean isSystemRunAs(Tree tree) {
    return tree instanceof NativeTree && RUN_AS_BLOCK_KIND.equals(((NativeTree) tree).nativeKind().toString());
  }

}

