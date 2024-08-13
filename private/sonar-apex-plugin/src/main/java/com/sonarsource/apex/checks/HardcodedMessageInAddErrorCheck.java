/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.FunctionUtils;

@Rule(key = "S5390")
public class HardcodedMessageInAddErrorCheck implements SlangCheck {

  private static final String ADD_ERROR_FUNCTION_NAME = "addError";
  private static final String MESSAGE =  "Replace this hardcoded message with a Label.";

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionInvocationTree.class, (ctx, tree) -> {
      if (tree.memberSelect() instanceof MemberSelectTree && FunctionUtils.hasFunctionCallNameIgnoreCase(tree, ADD_ERROR_FUNCTION_NAME)) {
        getFirstStringArg(tree).ifPresent(arg -> ctx.reportIssue(arg, MESSAGE));
      }
    });
  }

  private static Optional<StringLiteralTree> getFirstStringArg(FunctionInvocationTree tree) {
    int nArgs = tree.arguments().size();
    if (nArgs == 1 || nArgs == 2) {
      Tree firstArgument = tree.arguments().get(0);
      if (firstArgument instanceof StringLiteralTree) {
        return Optional.of((StringLiteralTree)firstArgument);
      }
    }
    return Optional.empty();
  }
}
