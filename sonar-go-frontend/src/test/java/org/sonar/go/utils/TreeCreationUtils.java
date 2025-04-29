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
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.ClassDeclarationTreeImpl;
import org.sonar.go.impl.FloatLiteralTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.impl.ModifierTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PackageDeclarationTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.FloatLiteralTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.ModifierTree;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.PackageDeclarationTree;
import org.sonar.plugins.go.api.PlaceHolderTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.VariableDeclarationTree;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.go.impl.IdentifierTreeImpl.UNKNOWN_PACKAGE;
import static org.sonar.go.impl.IdentifierTreeImpl.UNKNOWN_TYPE;

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

  public static FloatLiteralTree floatLiteral(String value) {
    return new FloatLiteralTreeImpl(null, value);
  }

  public static PlaceHolderTree placeHolderTree() {
    return new PlaceHolderTreeImpl(null, null);
  }

  public static IntegerLiteralTree integerLiteral(TreeMetaData metaData, String value) {
    return new IntegerLiteralTreeImpl(metaData, value);
  }

  public static IntegerLiteralTree integerLiteral(String value, TextRange textRange, String... tokens) {
    return new IntegerLiteralTreeImpl(metaData(textRange, tokens), value);
  }

  public static LiteralTree literal(String value) {
    return new LiteralTreeImpl(null, value);
  }

  public static StringLiteralTree stringLiteral(String value) {
    return new StringLiteralTreeImpl(null, value);
  }

  public static IdentifierTree identifier(String name) {
    return new IdentifierTreeImpl(null, name, UNKNOWN_TYPE, UNKNOWN_PACKAGE, 0);
  }

  public static IdentifierTree identifier(String name, String type) {
    return new IdentifierTreeImpl(null, name, type, UNKNOWN_PACKAGE, 0);
  }

  public static IdentifierTree identifier(String name, String type, String packageName) {
    return new IdentifierTreeImpl(null, name, type, packageName, 0);
  }

  public static IdentifierTree identifier(String name, TextRange textRange, String... tokens) {
    return new IdentifierTreeImpl(metaData(textRange, tokens), name, UNKNOWN_TYPE, UNKNOWN_PACKAGE, 0);
  }

  public static IdentifierTree identifier(TreeMetaData meta, String name) {
    return new IdentifierTreeImpl(meta, name, UNKNOWN_TYPE, UNKNOWN_PACKAGE, 0);
  }

  public static IdentifierTree identifier(TreeMetaData meta, String name, String type) {
    return new IdentifierTreeImpl(meta, name, type, UNKNOWN_PACKAGE, 0);
  }

  public static IdentifierTree identifier(TreeMetaData meta, String name, String type, String packageName) {
    return new IdentifierTreeImpl(meta, name, type, packageName, 0);
  }

  public static IdentifierTree identifier(TreeMetaData meta, String name, String type, String packageName, int id) {
    return new IdentifierTreeImpl(meta, name, type, packageName, id);
  }

  public static MemberSelectTree memberSelect(Tree expression, IdentifierTree identifier) {
    return new MemberSelectTreeImpl(null, expression, identifier);
  }

  public static ClassDeclarationTree classDeclarationTree(IdentifierTree className, Tree classDecl) {
    return new ClassDeclarationTreeImpl(null, className, classDecl);
  }

  public static VariableDeclarationTree variable(String name) {
    return new VariableDeclarationTreeImpl(null, List.of(identifier(name)), null, Collections.emptyList(), false);
  }

  public static VariableDeclarationTree value(String name) {
    return new VariableDeclarationTreeImpl(null, List.of(identifier(name)), null, Collections.emptyList(), true);
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

  public static FunctionDeclarationTree anonymousFunction(BlockTree body) {
    return new FunctionDeclarationTreeImpl(null, null, null, null, Collections.emptyList(), null, body, null);
  }

  public static FunctionDeclarationTree simpleFunction(IdentifierTree name, BlockTree body) {
    return new FunctionDeclarationTreeImpl(null, null, null, name, Collections.emptyList(), null, body, null);
  }

  public static FunctionInvocationTree simpleFunctionCall(Tree memberSelect) {
    return new FunctionInvocationTreeImpl(null, memberSelect, Collections.emptyList());
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

  public static PackageDeclarationTree packageDeclaration(String packageName) {
    return new PackageDeclarationTreeImpl(null, List.of(identifier(packageName)));
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
