/*
 * SonarSource SLang
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
package org.sonarsource.slang.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.IndexExpressionTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.Type;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S-slice-equal")
public class SliceEqualSimplificationCheck implements SlangCheck {

  private static final String MESSAGE = "Use slices.Equal() instead of custom slice comparison";

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, this::visitFunctionDeclaration);
  }

  private void visitFunctionDeclaration(CheckContext ctx, FunctionDeclarationTree function) {
    if (isSliceEqualityFunction(function)) {
      ctx.reportIssue(function.name(), MESSAGE);
    }
  }

  private static boolean isSliceEqualityFunction(FunctionDeclarationTree function) {
    List<ParameterTree> params = function.formalParameters();
    if (params.size() != 2) {
      return false;
    }

    ParameterTree param1 = params.get(0);
    ParameterTree param2 = params.get(1);

    // Semantic checks would be needed here to verify types.
    // For example:
    // if (!isSlice(param1.type()) || !isSlice(param2.type())) {
    //   return false;
    // }
    // if (!areElementTypesComparableAndIdentical(param1.type(), param2.type())) {
    //   return false;
    // }
    // if (!isBool(function.returnType())) {
    //   return false;
    // }

    BlockTree body = function.body();
    if (body == null || body.statementOrExpressions().size() != 3) {
      return false;
    }

    List<Tree> statements = body.statementOrExpressions();
    String p1Name = param1.identifier().name();
    String p2Name = param2.identifier().name();

    return isLengthCheck(statements.get(0), p1Name, p2Name) &&
      isElementComparisonLoop(statements.get(1), p1Name, p2Name) &&
      isReturnTrue(statements.get(2));
  }

  private static boolean isLengthCheck(Tree statement, String p1Name, String p2Name) {
    if (!(statement instanceof IfTree)) {
      return false;
    }
    IfTree ifTree = (IfTree) statement;
    if (ifTree.elseClause() != null || !(ifTree.condition() instanceof BinaryExpressionTree)) {
      return false;
    }
    BinaryExpressionTree condition = (BinaryExpressionTree) ifTree.condition();
    if (condition.operator() != BinaryExpressionTree.Operator.NOT_EQUAL_TO) {
      return false;
    }

    if (!isLenCallOnParam(condition.leftOperand(), p1Name, p2Name) || !isLenCallOnParam(condition.rightOperand(), p1Name, p2Name)) {
      return false;
    }

    BlockTree thenBlock = ifTree.thenClause();
    return thenBlock.statementOrExpressions().size() == 1 && isReturnFalse(thenBlock.statementOrExpressions().get(0));
  }

  private static boolean isLenCallOnParam(Tree tree, String p1Name, String p2Name) {
    if (!(tree instanceof FunctionInvocationTree)) {
      return false;
    }
    FunctionInvocationTree call = (FunctionInvocationTree) tree;
    if (!"len".equals(call.memberSelect().name()) || call.arguments().size() != 1) {
      return false;
    }
    Tree arg = call.arguments().get(0);
    if (!(arg instanceof IdentifierTree)) {
      return false;
    }
    String argName = ((IdentifierTree) arg).name();
    return argName.equals(p1Name) || argName.equals(p2Name);
  }

  private static boolean isElementComparisonLoop(Tree statement, String p1Name, String p2Name) {
    if (!(statement instanceof LoopTree)) {
      return false;
    }
    LoopTree loop = (LoopTree) statement;

    if (!(loop.body() instanceof BlockTree)) {
      return false;
    }
    BlockTree loopBody = (BlockTree) loop.body();
    if (loopBody.statementOrExpressions().size() != 1) {
      return false;
    }
    Tree loopStatement = loopBody.statementOrExpressions().get(0);
    return isElementComparisonIf(loopStatement, p1Name, p2Name);
  }

  private static boolean isElementComparisonIf(Tree statement, String p1Name, String p2Name) {
    if (!(statement instanceof IfTree)) {
      return false;
    }
    IfTree ifTree = (IfTree) statement;
    if (ifTree.elseClause() != null || !(ifTree.condition() instanceof BinaryExpressionTree)) {
      return false;
    }
    BinaryExpressionTree condition = (BinaryExpressionTree) ifTree.condition();
    if (condition.operator() != BinaryExpressionTree.Operator.NOT_EQUAL_TO) {
      return false;
    }

    if (!isIndexAccessOnParam(condition.leftOperand(), p1Name, p2Name) || !isIndexAccessOnParam(condition.rightOperand(), p1Name, p2Name)) {
      return false;
    }

    BlockTree thenBlock = ifTree.thenClause();
    return thenBlock.statementOrExpressions().size() == 1 && isReturnFalse(thenBlock.statementOrExpressions().get(0));
  }

  private static boolean isIndexAccessOnParam(Tree tree, String p1Name, String p2Name) {
    if (!(tree instanceof IndexExpressionTree)) {
      return false;
    }
    IndexExpressionTree indexExpr = (IndexExpressionTree) tree;
    if (!(indexExpr.base() instanceof IdentifierTree)) {
      return false;
    }
    String baseName = ((IdentifierTree) indexExpr.base()).name();
    return baseName.equals(p1Name) || baseName.equals(p2Name);
  }

  private static boolean isReturnFalse(Tree statement) {
    if (!(statement instanceof ReturnTree)) {
      return false;
    }
    ReturnTree returnTree = (ReturnTree) statement;
    Tree body = returnTree.body();
    return body instanceof IdentifierTree && "false".equals(((IdentifierTree) body).name());
  }

  private static boolean isReturnTrue(Tree statement) {
    if (!(statement instanceof ReturnTree)) {
      return false;
    }
    ReturnTree returnTree = (ReturnTree) statement;
    Tree body = returnTree.body();
    return body instanceof IdentifierTree && "true".equals(((IdentifierTree) body).name());
  }
}
