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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

public class FunctionDeclarationTreeImpl extends BaseTreeImpl implements FunctionDeclarationTree {

  @Nullable
  private final Tree returnType;
  @Nullable
  private final Tree receiver;
  @Nullable
  private final IdentifierTree name;
  private final List<Tree> formalParameters;
  @Nullable
  private final Tree typeParameters;
  @Nullable
  private final BlockTree body;
  private final List<Tree> children = new ArrayList<>();

  public FunctionDeclarationTreeImpl(
    TreeMetaData metaData,
    @Nullable Tree returnType,
    @Nullable Tree receiver,
    @Nullable IdentifierTree name,
    List<Tree> formalParameters,
    @Nullable Tree typeParameters,
    @Nullable BlockTree body) {
    super(metaData);

    this.returnType = returnType;
    this.receiver = receiver;
    this.name = name;
    this.formalParameters = formalParameters;
    this.typeParameters = typeParameters;
    this.body = body;

    if (returnType != null) {
      this.children.add(returnType);
    }
    if (receiver != null) {
      this.children.add(receiver);
    }
    if (name != null) {
      this.children.add(name);
    }
    this.children.addAll(formalParameters);
    if (typeParameters != null) {
      this.children.add(typeParameters);
    }
    if (body != null) {
      this.children.add(body);
    }
  }

  @CheckForNull
  @Override
  public Tree returnType() {
    return returnType;
  }

  @CheckForNull
  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public List<Tree> formalParameters() {
    return formalParameters;
  }

  @CheckForNull
  @Override
  public Tree typeParameters() {
    return typeParameters;
  }

  @CheckForNull
  @Override
  public BlockTree body() {
    return body;
  }

  @CheckForNull
  @Override
  public Tree receiver() {
    return receiver;
  }

  @Override
  public TextRange rangeToHighlight() {
    if (name != null) {
      return name.metaData().textRange();
    }
    if (body == null) {
      return metaData().textRange();
    }
    TextRange bodyRange = body.metaData().textRange();
    List<TextRange> tokenRangesBeforeBody = metaData().tokens().stream()
      .map(Token::textRange)
      .filter(t -> t.start().compareTo(bodyRange.start()) < 0)
      .toList();
    if (tokenRangesBeforeBody.isEmpty()) {
      return bodyRange;
    }
    return TextRanges.merge(tokenRangesBeforeBody);
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
