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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.LeftRightHandSide;
import org.sonar.go.api.ParameterTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.go.utils.VariableHelper;

/**
 * Class used to visit a {@link Tree} and build {@link Symbol} and their {@link Usage} for variables.
 * Those Symbol/Usage can later be used in checks to report issues in the variable flow.
 */
public class SymbolVisitor<C extends TreeContext> extends TreeVisitor<C> {
  private final Map<Integer, Symbol> symbolTable = new HashMap<>();

  public SymbolVisitor() {
    register(VariableDeclarationTree.class, (ctx, variableDeclarationTree) -> VariableHelper.getVariables(variableDeclarationTree)
      .forEach(variable -> addVariable(variable.identifier(), variable.value())));
    register(ParameterTree.class, (ctx, parameterTree) -> addVariable(parameterTree.identifier(), null));
    register(AssignmentExpressionTree.class, this::processAssignment);
    register(IdentifierTreeImpl.class, this::processIdentifier);
    registerOnLeaveTree(TopLevelTree.class, (ctx, tree) -> symbolTable.clear());
  }

  private void addVariable(IdentifierTree identifier, @Nullable Tree value) {
    if (identifier.id() != 0) {
      symbolTable.computeIfAbsent(identifier.id(), id -> new Symbol(identifier.type()));
      addVariableUsage(identifier, value, Usage.UsageType.DECLARATION);
    }
  }

  private void processIdentifier(C context, IdentifierTreeImpl identifier) {
    // We don't create a symbol reference if a symbol is already set (variable declaration, parameter or assignment)
    if (identifier.symbol() == null) {
      addVariableUsage(identifier, null, Usage.UsageType.REFERENCE);
    }
  }

  private void processAssignment(C context, AssignmentExpressionTree assignmentExpression) {
    var leftHandSide = assignmentExpression.leftHandSide();
    var identifiers = extractIdentifiers(leftHandSide);
    if (isLeftOrRightHandSide(assignmentExpression.statementOrExpression()) && leftHandSide instanceof LeftRightHandSide left) {
      var values = left.getChildrenSkipEmptyNativeTrees();
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
    } else if (tree instanceof LeftRightHandSide leftRightHandSide) {
      return leftRightHandSide.extractIdentifiers();
    } else {
      return Collections.emptyList();
    }
  }

  private static boolean isLeftOrRightHandSide(Tree tree) {
    return tree instanceof LeftRightHandSide;
  }

  private void addVariableUsage(IdentifierTree identifier, @Nullable Tree value, Usage.UsageType type) {
    if (identifier.id() != 0) {
      var symbol = symbolTable.get(identifier.id());
      if (symbol != null) {
        symbol.getUsages().add(new Usage(identifier, value, type));
        identifier.setSymbol(symbol);
      }
    }
  }
}
