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
package org.sonar.go.impl;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.Type;

import static org.sonar.go.utils.TreeUtils.retrieveLastIdentifier;

public class FunctionInvocationTreeImpl extends BaseTreeImpl implements FunctionInvocationTree {

  private static final String UNKNOWN = "UNKNOWN";
  private final Tree memberSelect;
  private final List<Tree> arguments;
  private final List<Type> returnTypes;

  public FunctionInvocationTreeImpl(TreeMetaData metaData, Tree memberSelect, List<Tree> arguments, List<Type> returnTypes) {
    super(metaData);
    this.memberSelect = memberSelect;
    this.arguments = arguments;
    this.returnTypes = returnTypes;
  }

  @Override
  public List<Tree> arguments() {
    return arguments;
  }

  @Override
  public String signature() {
    var sb = new StringBuilder();
    if (memberSelect instanceof MemberSelectTree memberSelectTree) {
      var expression = memberSelectTree.expression();
      retrieveLastIdentifier(expression)
        .map(FunctionInvocationTreeImpl::getPackageForMemberSelectExpression)
        .ifPresent(idSignature -> sb.append(idSignature).append("."));
      sb.append(memberSelectTree.identifier().name());
    } else if (memberSelect instanceof IdentifierTree identifierTree) {
      // build-in functions like string(), int(), int16(), or local functions or alias import
      var idSignature = getPackageForIdentifierTreeOnly(identifierTree);
      if (!idSignature.isBlank()) {
        sb.append(idSignature);
        sb.append(".");
      }
      sb.append(identifierTree.name());
    } else if (memberSelect instanceof FunctionDeclarationTree functionDeclarationTree) {
      // anonymous function
      sb.append(functionDeclarationTree.signature());
    }
    return sb.toString();
  }

  @Override
  public List<Type> returnTypes() {
    return returnTypes;
  }

  private static String getPackageForMemberSelectExpression(IdentifierTree identifierTree) {
    var result = identifierTree.packageName();
    if (UNKNOWN.equals(result)) {
      // functions from libraries, e.g.: net/http.Cookie.String() or method receiver
      result = identifierTree.type();
    }
    return result;
  }

  private static String getPackageForIdentifierTreeOnly(IdentifierTree identifierTree) {
    var packageName = identifierTree.packageName();
    if (!UNKNOWN.equals(packageName)) {
      return packageName;
    }
    return "";
  }

  @Override
  public Tree memberSelect() {
    return memberSelect;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(memberSelect);
    children.addAll(arguments);
    return children;
  }
}
