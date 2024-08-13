/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isNativeTreeKind;
import static java.util.Arrays.asList;

@Rule(key = "S5376")
public class TriggersProcessRecordsInBulkCheck implements SlangCheck {

  private static final String MESSAGE = "Iterate over Trigger's records to process them all.";
  private static final String TRIGGER_VARIABLE_KIND = "TriggerVariableExpr";
  private static final String ARRAY_ACCESS_KIND = "ArrayExpr";
  private static final String GET_FUNCTION_IDENTIFIER = "get";
  private static final List<String> ARRAY_ACCESS_RECORD_MEMBER_NAMES = asList("new", "old");
  private static final List<String> FUNCTION_CALL_RECORDS_MEMBER_NAMES = asList("newmap", "oldmap");
  private static final int ARRAY_ACCESS_CHILDREN_COUNT = 2;

  @Override
  public void initialize(InitContext init) {
    init.register(NativeTree.class, (ctx, nativeTree) -> {
      if (isInvalidArrayAccessOnTriggerVariable(nativeTree)) {
        ctx.reportIssue(nativeTree, MESSAGE);
      }
    });

    init.register(FunctionInvocationTree.class, (ctx, functionInvocationTree) -> {
      if (isInvalidGetFunctionCallOnTriggerMap(functionInvocationTree)) {
        ctx.reportIssue(functionInvocationTree, MESSAGE);
      }
    });
  }

  private static boolean isInvalidArrayAccessOnTriggerVariable(NativeTree nativeTree) {
    if (!ARRAY_ACCESS_KIND.equals(nativeTree.nativeKind().toString()) || nativeTree.children().size() != ARRAY_ACCESS_CHILDREN_COUNT) {
      return false;
    }

    Tree triggerVariable = nativeTree.children().get(0);
    Tree arrayAccessParam = nativeTree.children().get(1);

    return isTriggerRecordAccess(triggerVariable, ARRAY_ACCESS_RECORD_MEMBER_NAMES)
      && arrayAccessParam instanceof LiteralTree;
  }

  private static boolean isInvalidGetFunctionCallOnTriggerMap(FunctionInvocationTree functionInvocationTree) {
    if (functionInvocationTree.arguments().size() != 1 || !(functionInvocationTree.arguments().get(0) instanceof LiteralTree)) {
      return false;
    }

    Tree memberSelectTree = functionInvocationTree.memberSelect();
    if (!(memberSelectTree instanceof MemberSelectTree)) {
      return false;
    }

    MemberSelectTree memberSelect = (MemberSelectTree) memberSelectTree;

    return isTriggerRecordAccess(memberSelect.expression(), FUNCTION_CALL_RECORDS_MEMBER_NAMES)
      && GET_FUNCTION_IDENTIFIER.equals(memberSelect.identifier().identifier());
  }

  private static boolean isTriggerRecordAccess(Tree tree, List<String> expectedChildrenIdentifiers) {
    return isNativeTreeKind(tree, TRIGGER_VARIABLE_KIND)
      && tree.children().size() == 1
      && tree.children().get(0) instanceof IdentifierTree
      && expectedChildrenIdentifiers.contains(((IdentifierTree) tree.children().get(0)).identifier());
  }

}

