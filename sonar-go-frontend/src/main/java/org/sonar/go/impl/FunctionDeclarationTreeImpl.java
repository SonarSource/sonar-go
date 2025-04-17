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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.utils.NativeKinds;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.cfg.ControlFlowGraph;

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
  private String receiverName;
  private String receiverType;
  private boolean isReceiverNameCalculated;
  private boolean isReceiverTypeCalculated;
  @Nullable
  private final ControlFlowGraph cfg;

  public FunctionDeclarationTreeImpl(
    TreeMetaData metaData,
    @Nullable Tree returnType,
    @Nullable Tree receiver,
    @Nullable IdentifierTree name,
    List<Tree> formalParameters,
    @Nullable Tree typeParameters,
    @Nullable BlockTree body,
    @Nullable ControlFlowGraph cfg) {
    super(metaData);

    this.returnType = returnType;
    this.receiver = receiver;
    this.name = name;
    this.formalParameters = formalParameters;
    this.typeParameters = typeParameters;
    this.body = body;
    this.cfg = cfg;

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

  @CheckForNull
  @Override
  public String receiverName() {
    if (!isReceiverNameCalculated) {
      receiverName = Stream.of(receiver)
        .filter(Objects::nonNull)
        .flatMap(Tree::descendants)
        .filter(NativeKinds::isMethodReceiverTreeIdentifier)
        .map(Tree::children)
        .flatMap(Collection::stream)
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast)
        .map(IdentifierTree::name)
        .findFirst()
        .orElse(null);
      isReceiverNameCalculated = true;
    }
    return receiverName;
  }

  @CheckForNull
  @Override
  public String receiverType(String packageName) {
    if (!isReceiverTypeCalculated) {
      receiverType = Stream.of(receiver)
        .filter(Objects::nonNull)
        .flatMap(Tree::descendants)
        .filter(NativeKinds::isMethodReceiverTreeIdentifier)
        .map(Tree::children)
        .flatMap(Collection::stream)
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast)
        .map(IdentifierTree::type)
        // For the receiver types defined in the source file, the type is often like: "*-.MyStruct".
        // But such types don't exist in sonar-go-to-slang/resources/ast/*.json files.
        // It is so because in production the Go file content is passed to sonar-go-to-slang executable via stdin
        // and then the filename is set to dash "-" (it is also an argument for sonar-go-to-slang executable).
        // The goparser_test.go read those files directly from filesystem, so the type is: "method_receiver.go.MyStruct"
        .map(t -> t.replace("-", packageName))
        .findFirst()
        .orElse(null);
      isReceiverTypeCalculated = true;
    }
    return receiverType;
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
  public ControlFlowGraph cfg() {
    return cfg;
  }

  @Override
  public String signature(String packageName) {
    var sb = new StringBuilder();
    var receiverTypeLocal = receiverType(packageName);
    if (receiverTypeLocal != null) {
      sb.append(receiverTypeLocal);
    } else {
      sb.append(packageName);
    }
    sb.append(".");
    if (name != null) {
      sb.append(name.name());
    } else {
      sb.append("$anonymous_at_line_");
      sb.append(metaData().textRange().start().line());
    }
    return sb.toString();
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
