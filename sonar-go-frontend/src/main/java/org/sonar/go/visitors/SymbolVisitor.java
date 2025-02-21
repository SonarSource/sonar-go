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
package org.sonar.go.visitors;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.symbols.Scope;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.go.utils.NativeKinds;
import org.sonar.go.utils.TreeUtils;
import org.sonar.go.utils.VariableHelper;

import static org.sonar.go.utils.TreeUtils.IS_NOT_EMPTY_NATIVE_TREE;

/**
 * Class used to visit a {@link Tree} and build {@link Symbol} and their {@link Usage} for variables.
 * Those Symbol/Usage can later be used in checks to report issues in the variable flow.
 */
public class SymbolVisitor<C extends TreeContext> extends TreeVisitor<C> {
  private final Deque<Map<String, Symbol>> variablesPerScope = new LinkedList<>();
  private final Deque<Scope> scopes = new LinkedList<>();
  private int memberSelectMet = 0;
  private boolean skipNextBlockScopeInsertion = false;

  public SymbolVisitor() {
    variablesPerScope.addLast(new HashMap<>());
    scopes.addLast(Scope.PACKAGE);

    register(FunctionDeclarationTree.class, (ctx, tree) -> {
      enterScope(Scope.FUNCTION);
      // we don't want to insert the function block as a new block scope
      skipNextBlockScopeInsertion = true;
    });
    register(BlockTree.class, (ctx, tree) -> enterScope(Scope.BLOCK));
    registerOnLeaveTree(BlockTree.class, this::leaveScope);
    // We do it for loop/switch too, so that any variable declared in for loop header is not visible outside of it.
    register(LoopTree.class, (ctx, tree) -> enterScope(Scope.BLOCK));
    registerOnLeaveTree(LoopTree.class, this::leaveScope);
    register(MatchTree.class, (ctx, tree) -> enterScope(Scope.BLOCK));
    registerOnLeaveTree(MatchTree.class, this::leaveScope);

    register(VariableDeclarationTree.class, (ctx, variableDeclarationTree) -> VariableHelper.getVariables(variableDeclarationTree)
      .forEach(variable -> addVariable(variable.type(), variable.identifier(), variable.value())));
    register(ParameterTree.class, (ctx, parameterTree) -> addVariable(parameterTree.type(), parameterTree.identifier(), null));
    register(AssignmentExpressionTree.class, this::processAssignment);
    register(IdentifierTreeImpl.class, this::processIdentifier);
    register(MemberSelectTree.class, this::onMemberSelectEnter);
    registerOnLeaveTree(MemberSelectTree.class, this::onMemberSelectLeave);
  }

  private void enterScope(Scope scope) {
    var newScopeOfVariables = new HashMap<String, Symbol>();
    variablesPerScope.addLast(newScopeOfVariables);
    if (skipNextBlockScopeInsertion) {
      skipNextBlockScopeInsertion = false;
    } else {
      scopes.addLast(scope);
    }
  }

  private void leaveScope(C context, Tree tree) {
    variablesPerScope.removeLast();
    scopes.removeLast();
  }

  private void addVariable(@Nullable Tree type, IdentifierTree identifier, @Nullable Tree value) {
    var symbol = new Symbol(computeType(type), scopes.getLast());
    variablesPerScope.getLast().put(identifier.name(), symbol);
    addVariableUsage(identifier, value, Usage.UsageType.DECLARATION);
  }

  private static String computeType(@Nullable Tree type) {
    if (type == null) {
      return Symbol.UNKNOWN_TYPE;
    } else {
      return TreeUtils.treeToString(type);
    }
  }

  private void processIdentifier(C context, IdentifierTreeImpl identifier) {
    // We don't create a symbol reference if a symbol is already set (variable declaration, parameter or assignement) or if we are in a member
    // select AST node.
    if (identifier.symbol() == null && memberSelectMet == 0) {
      addVariableUsage(identifier, null, Usage.UsageType.REFERENCE);
    }
  }

  private void processAssignment(C context, AssignmentExpressionTree assignmentExpression) {
    var identifiers = extractIdentifiers(assignmentExpression.leftHandSide());
    if (isRightHandSideArrayOfExpression(assignmentExpression.statementOrExpression())) {
      var values = assignmentExpression.leftHandSide().children().stream().filter(IS_NOT_EMPTY_NATIVE_TREE).toList();
      for (int i = 0; i < identifiers.size(); i++) {
        addVariableUsage(identifiers.get(i), values.get(i), Usage.UsageType.ASSIGNMENT);
      }
    } else {
      for (IdentifierTree identifier : identifiers) {
        addVariableUsage(identifier, assignmentExpression.statementOrExpression(), Usage.UsageType.ASSIGNMENT);
      }
    }
  }

  private static List<IdentifierTree> extractIdentifiers(Tree tree) {
    if (tree instanceof IdentifierTree identifier) {
      return List.of(identifier);
    } else if (isLeftHandSideArrayOfExpression(tree)) {
      return tree.children().stream()
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast).toList();
    } else {
      return Collections.emptyList();
    }
  }

  private static boolean isLeftHandSideArrayOfExpression(Tree tree) {
    return NativeKinds.isStringNativeKindOfType(tree, "Lhs", "Expr");
  }

  private static boolean isRightHandSideArrayOfExpression(Tree tree) {
    return NativeKinds.isStringNativeKindOfType(tree, "Rhs", "Expr");
  }

  private void onMemberSelectLeave(C context, MemberSelectTree memberSelectTree) {
    // We manually create a variable reference in member select if the expression is an identifier.
    if (memberSelectTree.expression() instanceof IdentifierTree identifier) {
      addVariableUsage(identifier, null, Usage.UsageType.REFERENCE);
    }
    memberSelectMet++;
  }

  private void onMemberSelectEnter(C context, MemberSelectTree memberSelectTree) {
    memberSelectMet--;
  }

  private void addVariableUsage(IdentifierTree identifier, @Nullable Tree value, Usage.UsageType type) {
    // Look for symbol in all scope starting from the last one
    var iterator = variablesPerScope.descendingIterator();
    while (iterator.hasNext()) {
      var variables = iterator.next();
      var symbol = variables.get(identifier.name());
      if (symbol != null) {
        symbol.getUsages().add(new Usage(identifier, value, type));
        identifier.setSymbol(symbol);
        return;
      }
    }
  }
}
