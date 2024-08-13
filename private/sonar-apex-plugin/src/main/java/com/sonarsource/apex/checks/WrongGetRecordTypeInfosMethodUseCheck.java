/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import org.sonar.check.Rule;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.FunctionUtils;

@Rule(key = "S5387")
public class WrongGetRecordTypeInfosMethodUseCheck implements SlangCheck {

  private static final String WRONG_GET_RECORD_TYPE_INFOS = "getRecordTypeInfosByName";
  private static final String CORRECT_GET_RECORD_TYPE_INFOS = "getRecordTypeInfosByDeveloperName";

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionInvocationTree.class, (ctx, tree) -> {
      if (isWrongGetRecordTypeInfos(tree)) {
        ctx.reportIssue(tree, "Replace this call to \"" + WRONG_GET_RECORD_TYPE_INFOS + "\" by a call to \"" +
          CORRECT_GET_RECORD_TYPE_INFOS + "\"");
      }
    });
  }

  private static boolean isWrongGetRecordTypeInfos(FunctionInvocationTree tree) {
    return tree.arguments().isEmpty()
      && FunctionUtils.hasFunctionCallNameIgnoreCase(tree, WRONG_GET_RECORD_TYPE_INFOS);
  }
}
