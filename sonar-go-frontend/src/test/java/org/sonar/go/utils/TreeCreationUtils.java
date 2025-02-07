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
package org.sonar.go.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.FunctionDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.api.LiteralTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.ModifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.PlaceHolderTree;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.Token;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.VariableDeclarationTree;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
import org.sonar.go.impl.ModifierTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TreeCreationUtils {
  private TreeCreationUtils() {
  }

  public static LoopTree loop(Tree condition, Tree body, LoopTree.LoopKind kind, String keyword) {
    Token tokenKeyword = new TokenImpl(null, keyword, Token.Type.KEYWORD);
    return new LoopTreeImpl(null, condition, body, kind, tokenKeyword);
  }

  public static IntegerLiteralTree integerLiteral(String value) {
    return new IntegerLiteralTreeImpl(null, value);
  }

  public static PlaceHolderTree placeHolderTree() {
    return new PlaceHolderTreeImpl(null, null);
  }

  public static IntegerLiteralTree integerLiteral(String value, TextRange textRange, String... tokens) {
    return new IntegerLiteralTreeImpl(metaData(textRange, tokens), value);
  }

  public static LiteralTree literal(String value) {
    return new LiteralTreeImpl(null, value);
  }

  public static IdentifierTree identifier(String name) {
    return new IdentifierTreeImpl(null, name);
  }

  public static IdentifierTree identifier(String name, TextRange textRange, String... tokens) {
    return new IdentifierTreeImpl(metaData(textRange, tokens), name);
  }

  public static VariableDeclarationTree variable(String name) {
    return new VariableDeclarationTreeImpl(null, identifier(name), null, null, false);
  }

  public static VariableDeclarationTree value(String name) {
    return new VariableDeclarationTreeImpl(null, identifier(name), null, null, true);
  }

  public static BinaryExpressionTree binary(BinaryExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand) {
    return new BinaryExpressionTreeImpl(null, operator, null, leftOperand, rightOperand);
  }

  public static BinaryExpressionTree binary(BinaryExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand, TextRange textRange, String... tokens) {
    return new BinaryExpressionTreeImpl(metaData(textRange, tokens), operator, new TokenImpl(new TextRangeImpl(1, 0, 1, 0), operator.toString(), null), leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(Tree leftOperand, Tree rightOperand) {
    return assignment(AssignmentExpressionTree.Operator.EQUAL, leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(Tree leftOperand, Tree rightOperand, TextRange textRange, String... tokens) {
    return assignment(AssignmentExpressionTree.Operator.EQUAL, leftOperand, rightOperand, textRange, tokens);
  }

  public static BlockTree block(List<Tree> body) {
    return new BlockTreeImpl(null, body);
  }

  public static BlockTree block(List<Tree> body, TextRange textRange, String... tokens) {
    return new BlockTreeImpl(metaData(textRange, tokens), body);
  }

  public static FunctionDeclarationTree simpleFunction(IdentifierTree name, BlockTree body) {
    return new FunctionDeclarationTreeImpl(null, null, null, name, Collections.emptyList(), body, emptyList());
  }

  public static AssignmentExpressionTree assignment(AssignmentExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand) {
    return new AssignmentExpressionTreeImpl(null, operator, leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(AssignmentExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand, TextRange textRange, String... tokens) {
    return new AssignmentExpressionTreeImpl(metaData(textRange, tokens), operator, leftOperand, rightOperand);
  }

  public static NativeTree simpleNative(NativeKind kind, List<Tree> children) {
    return new NativeTreeImpl(null, kind, children);
  }

  public static NativeTree simpleNative(NativeKind kind, List<String> tokens, List<Tree> children) {
    return new NativeTreeImpl(metaData(tokens), kind, children);
  }

  public static ModifierTree simpleModifier(ModifierTree.Kind kind) {
    return new ModifierTreeImpl(null, kind);
  }

  public static TopLevelTree topLevel(List<Tree> declarations) {
    return new TopLevelTreeImpl(null, declarations, null);
  }

  private static TreeMetaData metaData(List<String> tokens) {
    TreeMetaData metaData = mock(TreeMetaData.class);
    mockTokens(metaData, tokens);
    return metaData;
  }

  private static TreeMetaData metaData(TextRange textRange, String... tokens) {
    TreeMetaData metaData = mock(TreeMetaData.class);
    mockTokens(metaData, Arrays.asList(tokens));
    mockTextRange(metaData, textRange);
    return metaData;
  }

  private static void mockTokens(TreeMetaData metaData, List<String> tokens) {
    when(metaData.tokens()).thenReturn(tokens.stream()
      .map(text -> new TokenImpl(null, text, null))
      .collect(Collectors.toList()));
  }

  private static void mockTextRange(TreeMetaData metaData, TextRange textRange) {
    when(metaData.textRange()).thenReturn(textRange);
  }
}
