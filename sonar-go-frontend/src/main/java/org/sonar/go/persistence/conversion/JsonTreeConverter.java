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
package org.sonar.go.persistence.conversion;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.List;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BinaryExpressionTree.Operator;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.CatchTree;
import org.sonar.go.api.Comment;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.JumpTree;
import org.sonar.go.api.LoopTree;
import org.sonar.go.api.MatchCaseTree;
import org.sonar.go.api.ModifierTree;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.UnaryExpressionTree;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.CatchTreeImpl;
import org.sonar.go.impl.ClassDeclarationTreeImpl;
import org.sonar.go.impl.CommentImpl;
import org.sonar.go.impl.CompositeLiteralTreeImpl;
import org.sonar.go.impl.ExceptionHandlingTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.IfTreeImpl;
import org.sonar.go.impl.ImportDeclarationTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.JumpTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
import org.sonar.go.impl.MatchCaseTreeImpl;
import org.sonar.go.impl.MatchTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.impl.ModifierTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PackageDeclarationTreeImpl;
import org.sonar.go.impl.ParameterTreeImpl;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.ReturnTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.ThrowTreeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.go.impl.UnaryExpressionTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.persistence.conversion.PolymorphicConverter.Deserialize;
import org.sonar.go.persistence.conversion.PolymorphicConverter.Serialize;

public final class JsonTreeConverter {

  public static final String ARGUMENTS = "arguments";
  public static final String BODY = "body";
  public static final String CASES = "cases";
  public static final String CATCH_BLOCK = "catchBlock";
  public static final String CATCH_BLOCKS = "catchBlocks";
  public static final String CATCH_PARAMETER = "catchParameter";
  public static final String CHILDREN = "children";
  public static final String CLASS_TREE = "classTree";
  public static final String COMMENTS = "comments";
  public static final String CONDITION = "condition";
  public static final String CONTENT = "content";
  public static final String CONTENT_RANGE = "contentRange";
  public static final String CONTENT_TEXT = "contentText";
  public static final String DECLARATIONS = "declarations";
  public static final String DEFAULT_VALUE = "defaultValue";
  public static final String ELEMENTS = "elements";
  public static final String ELSE_BRANCH = "elseBranch";
  public static final String ELSE_KEYWORD = "elseKeyword";
  public static final String EXPRESSION = "expression";
  public static final String FINALLY_BLOCK = "finallyBlock";
  public static final String FIRST_CPD_TOKEN = "firstCpdToken";
  public static final String FORMAL_PARAMETERS = "formalParameters";
  public static final String IDENTIFIER = "identifier";
  public static final String IF_KEYWORD = "ifKeyword";
  public static final String INITIALIZER = "initializer";
  public static final String IS_VAL = "isVal";
  public static final String KEYWORD = "keyword";
  public static final String KIND = "kind";
  public static final String LABEL = "label";
  public static final String LEFT_HAND_SIDE = "leftHandSide";
  public static final String LEFT_OPERAND = "leftOperand";
  public static final String LEFT_PARENTHESIS = "leftParenthesis";
  public static final String MEMBER_SELECT = "memberSelect";
  public static final String MODIFIERS = "modifiers";
  public static final String NAME = "name";
  public static final String NATIVE_CHILDREN = "nativeChildren";
  public static final String NATIVE_KIND = "nativeKind";
  public static final String OPERAND = "operand";
  public static final String OPERATOR = "operator";
  public static final String OPERATOR_TOKEN = "operatorToken";
  public static final String PLACE_HOLDER_TOKEN = "placeHolderToken";
  public static final String RANGE = "range";
  public static final String RECEIVER = "receiver";
  public static final String RETURN_TYPE = "returnType";
  public static final String RIGHT_OPERAND = "rightOperand";
  public static final String RIGHT_PARENTHESIS = "rightParenthesis";
  public static final String STATEMENT_OR_EXPRESSION = "statementOrExpression";
  public static final String STATEMENT_OR_EXPRESSIONS = "statementOrExpressions";
  public static final String TEXT = "text";
  public static final String TEXT_RANGE = "textRange";
  public static final String THEN_BRANCH = "thenBranch";
  public static final String TOKENS = "tokens";
  public static final String TRY_BLOCK = "tryBlock";
  public static final String TRY_KEYWORD = "tryKeyword";
  public static final String TYPE = "type";
  public static final String VALUE = "value";
  public static final String OTHER = "OTHER";

  public static final PolymorphicConverter POLYMORPHIC_CONVERTER = new PolymorphicConverter();

  public static final Serialize<Token> TOKEN_TO_JSON = (ctx, token) -> {
    JsonObject json = Json.object()
      .add(TEXT_RANGE, ctx.toJson(token.textRange()))
      .add(TEXT, token.text());

    return token.type().name().equals(OTHER)
      ? json
      : json.add(TYPE, ctx.toJson(token.type()));
  };

  public static final Deserialize<Token> TOKEN_FROM_JSON = (ctx, json) -> new TokenImpl(
    ctx.fieldToRange(json, TEXT_RANGE),
    ctx.fieldToString(json, TEXT),
    ctx.fieldToEnum(json, TYPE, OTHER, Token.Type.class));

  public static final Serialize<Comment> COMMENT_TO_JSON = (ctx, comment) -> Json.object()
    .add(TEXT, comment.text())
    .add(CONTENT_TEXT, comment.contentText())
    .add(RANGE, ctx.toJson(comment.textRange()))
    .add(CONTENT_RANGE, ctx.toJson(comment.contentRange()));

  public static final Deserialize<Comment> COMMENT_FROM_JSON = (ctx, json) -> new CommentImpl(
    ctx.fieldToString(json, TEXT),
    ctx.fieldToString(json, CONTENT_TEXT),
    ctx.fieldToRange(json, RANGE),
    ctx.fieldToRange(json, CONTENT_RANGE));

  public static final Serialize<TreeMetaDataProvider> TREE_METADATA_PROVIDER_TO_JSON = (ctx, provider) -> Json.object()
    .add(COMMENTS, ctx.toJsonArray(provider.allComments(), COMMENT_TO_JSON))
    .add(TOKENS, ctx.toJsonArray(provider.allTokens(), TOKEN_TO_JSON));

  public static final Deserialize<TreeMetaDataProvider> TREE_METADATA_PROVIDER_FROM_JSON = (ctx, json) -> new TreeMetaDataProvider(
    ctx.objectList(json.get(COMMENTS), COMMENT_FROM_JSON),
    ctx.objectList(json.get(TOKENS), TOKEN_FROM_JSON));

  static {

    register(AssignmentExpressionTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(OPERATOR, ctx.toJson(tree.operator()))
        .add(LEFT_HAND_SIDE, ctx.toJson(tree.leftHandSide()))
        .add(STATEMENT_OR_EXPRESSION, ctx.toJson(tree.statementOrExpression())),

      (ctx, json) -> new AssignmentExpressionTreeImpl(
        ctx.metaData(json),
        ctx.fieldToEnum(json, OPERATOR, AssignmentExpressionTree.Operator.class),
        ctx.fieldToObject(json, LEFT_HAND_SIDE, Tree.class),
        ctx.fieldToObject(json, STATEMENT_OR_EXPRESSION, Tree.class)));

    register(BinaryExpressionTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(OPERATOR, ctx.toJson(tree.operator()))
        .add(OPERATOR_TOKEN, ctx.toJson(tree.operatorToken()))
        .add(LEFT_OPERAND, ctx.toJson(tree.leftOperand()))
        .add(RIGHT_OPERAND, ctx.toJson(tree.rightOperand())),

      (ctx, json) -> new BinaryExpressionTreeImpl(
        ctx.metaData(json),
        ctx.fieldToEnum(json, OPERATOR, Operator.class),
        ctx.fieldToToken(json, OPERATOR_TOKEN),
        ctx.fieldToObject(json, LEFT_OPERAND, Tree.class),
        ctx.fieldToObject(json, RIGHT_OPERAND, Tree.class)));

    register(BlockTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(STATEMENT_OR_EXPRESSIONS, ctx.toJsonArray(tree.children())),

      (ctx, json) -> new BlockTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObjectList(json, STATEMENT_OR_EXPRESSIONS, Tree.class)));

    register(CatchTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CATCH_PARAMETER, ctx.toJson(tree.catchParameter()))
        .add(CATCH_BLOCK, ctx.toJson(tree.catchBlock()))
        .add(KEYWORD, ctx.toJson(tree.keyword())),

      (ctx, json) -> new CatchTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, CATCH_PARAMETER, Tree.class),
        ctx.fieldToObject(json, CATCH_BLOCK, Tree.class),
        ctx.fieldToToken(json, KEYWORD)));

    register(ClassDeclarationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(IDENTIFIER, RangeConverter.treeReference(tree.identifier()))
        .add(CLASS_TREE, ctx.toJson(tree.classTree())),

      (ctx, json) -> {
        Tree classTree = ctx.fieldToObject(json, CLASS_TREE, Tree.class);
        String identifierReference = ctx.fieldToNullableString(json, IDENTIFIER);
        IdentifierTree identifier = RangeConverter.resolveNullableTree(classTree, identifierReference, IdentifierTree.class);
        return new ClassDeclarationTreeImpl(
          ctx.metaData(json),
          identifier,
          classTree);
      });

    register(CompositeLiteralTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(TYPE, ctx.toJson(tree.type()))
        .add(ELEMENTS, ctx.toJsonArray(tree.elements())),

      (ctx, json) -> new CompositeLiteralTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, TYPE, Tree.class),
        ctx.fieldToObjectList(json, ELEMENTS, Tree.class)));

    register(ExceptionHandlingTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(TRY_BLOCK, ctx.toJson(tree.tryBlock()))
        .add(TRY_KEYWORD, ctx.toJson(tree.tryKeyword()))
        .add(CATCH_BLOCKS, ctx.toJsonArray(tree.catchBlocks()))
        .add(FINALLY_BLOCK, ctx.toJson(tree.finallyBlock())),

      (ctx, json) -> new ExceptionHandlingTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, TRY_BLOCK, Tree.class),
        ctx.fieldToToken(json, TRY_KEYWORD),
        ctx.fieldToObjectList(json, CATCH_BLOCKS, CatchTree.class),
        ctx.fieldToNullableObject(json, FINALLY_BLOCK, Tree.class)));

    register(FunctionDeclarationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(RETURN_TYPE, ctx.toJson(tree.returnType()))
        .add(RECEIVER, ctx.toJson(tree.receiver()))
        .add(NAME, ctx.toJson(tree.name()))
        .add(FORMAL_PARAMETERS, ctx.toJsonArray(tree.formalParameters()))
        .add(BODY, ctx.toJson(tree.body()))
        .add(NATIVE_CHILDREN, ctx.toJsonArray(tree.nativeChildren())),

      (ctx, json) -> new FunctionDeclarationTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, RETURN_TYPE, Tree.class),
        ctx.fieldToNullableObject(json, RECEIVER, Tree.class),
        ctx.fieldToNullableObject(json, NAME, IdentifierTree.class),
        ctx.fieldToObjectList(json, FORMAL_PARAMETERS, Tree.class),
        ctx.fieldToNullableObject(json, BODY, BlockTree.class),
        ctx.fieldToObjectList(json, NATIVE_CHILDREN, Tree.class)));

    register(FunctionInvocationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(MEMBER_SELECT, ctx.toJson(tree.memberSelect()))
        .add(ARGUMENTS, ctx.toJsonArray(tree.arguments())),

      (ctx, json) -> new FunctionInvocationTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, MEMBER_SELECT, Tree.class),
        ctx.fieldToObjectList(json, ARGUMENTS, Tree.class)));

    register(IdentifierTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(NAME, tree.name()),

      (ctx, json) -> new IdentifierTreeImpl(
        ctx.metaData(json),
        ctx.fieldToString(json, NAME)));

    register(IfTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CONDITION, ctx.toJson(tree.condition()))
        .add(THEN_BRANCH, ctx.toJson(tree.thenBranch()))
        .add(ELSE_BRANCH, ctx.toJson(tree.elseBranch()))
        .add(IF_KEYWORD, ctx.toJson(tree.ifKeyword()))
        .add(ELSE_KEYWORD, ctx.toJson(tree.elseKeyword())),

      (ctx, json) -> new IfTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, CONDITION, Tree.class),
        ctx.fieldToObject(json, THEN_BRANCH, Tree.class),
        ctx.fieldToNullableObject(json, ELSE_BRANCH, Tree.class),
        ctx.fieldToToken(json, IF_KEYWORD),
        ctx.fieldToNullableToken(json, ELSE_KEYWORD)));

    register(ImportDeclarationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CHILDREN, ctx.toJsonArray(tree.children())),

      (ctx, json) -> new ImportDeclarationTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObjectList(json, CHILDREN, Tree.class)));

    register(IntegerLiteralTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(VALUE, tree.value()),

      (ctx, json) -> new IntegerLiteralTreeImpl(
        ctx.metaData(json),
        ctx.fieldToString(json, VALUE)));

    register(JumpTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(LABEL, ctx.toJson(tree.label()))
        .add(KEYWORD, ctx.toJson(tree.keyword()))
        .add(KIND, ctx.toJson(tree.kind())),

      (ctx, json) -> new JumpTreeImpl(
        ctx.metaData(json),
        ctx.fieldToToken(json, KEYWORD),
        ctx.fieldToEnum(json, KIND, JumpTree.JumpKind.class),
        ctx.fieldToNullableObject(json, LABEL, IdentifierTree.class)));

    register(LiteralTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(VALUE, tree.value()),

      (ctx, json) -> new LiteralTreeImpl(
        ctx.metaData(json),
        ctx.fieldToString(json, VALUE)));

    register(LoopTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CONDITION, ctx.toJson(tree.condition()))
        .add(BODY, ctx.toJson(tree.body()))
        .add(KIND, ctx.toJson(tree.kind()))
        .add(KEYWORD, ctx.toJson(tree.keyword())),

      (ctx, json) -> new LoopTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, CONDITION, Tree.class),
        ctx.fieldToObject(json, BODY, Tree.class),
        ctx.fieldToEnum(json, KIND, LoopTree.LoopKind.class),
        ctx.fieldToToken(json, KEYWORD)));

    register(MatchTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(EXPRESSION, ctx.toJson(tree.expression()))
        .add(CASES, ctx.toJsonArray(tree.cases()))
        .add(KEYWORD, ctx.toJson(tree.keyword())),

      (ctx, json) -> new MatchTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, EXPRESSION, Tree.class),
        ctx.fieldToObjectList(json, CASES, MatchCaseTree.class),
        ctx.fieldToToken(json, KEYWORD)));

    register(MatchCaseTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(EXPRESSION, ctx.toJson(tree.expression()))
        .add(BODY, ctx.toJson(tree.body())),

      (ctx, json) -> new MatchCaseTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNullableObject(json, EXPRESSION, Tree.class),
        ctx.fieldToNullableObject(json, BODY, Tree.class)));

    register(MemberSelectTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(EXPRESSION, ctx.toJson(tree.expression()))
        .add(IDENTIFIER, ctx.toJson(tree.identifier())),

      (ctx, json) -> new MemberSelectTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, EXPRESSION, Tree.class),
        ctx.fieldToObject(json, IDENTIFIER, IdentifierTree.class)));

    register(ModifierTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(KIND, ctx.toJson(tree.kind())),

      (ctx, json) -> new ModifierTreeImpl(
        ctx.metaData(json),
        ctx.fieldToEnum(json, KIND, ModifierTree.Kind.class)));

    register(NativeTreeImpl.class,

      (ctx, tree) -> {
        JsonObject json = ctx.newTypedObject(tree);

        if (!tree.nativeKind().toString().isEmpty()) {
          json.add(NATIVE_KIND, ctx.toJson(tree.nativeKind()));
        }

        return json.add(CHILDREN, ctx.toJsonArray(tree.children()));
      },

      (ctx, json) -> new NativeTreeImpl(
        ctx.metaData(json),
        ctx.fieldToNativeKind(json, NATIVE_KIND),
        ctx.fieldToObjectList(json, CHILDREN, Tree.class)));

    register(PackageDeclarationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CHILDREN, ctx.toJsonArray(tree.children())),

      (ctx, json) -> new PackageDeclarationTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObjectList(json, CHILDREN, Tree.class)));

    register(ParameterTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(IDENTIFIER, ctx.toJson(tree.identifier()))
        .add(TYPE, ctx.toJson(tree.type()))
        .add(DEFAULT_VALUE, ctx.toJson(tree.defaultValue()))
        .add(MODIFIERS, ctx.toJsonArray(tree.modifiers())),

      (ctx, json) -> new ParameterTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, IDENTIFIER, IdentifierTree.class),
        ctx.fieldToNullableObject(json, TYPE, Tree.class),
        ctx.fieldToNullableObject(json, DEFAULT_VALUE, Tree.class),
        ctx.fieldToObjectList(json, MODIFIERS, Tree.class)));

    register(ParenthesizedExpressionTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(EXPRESSION, ctx.toJson(tree.expression()))
        .add(LEFT_PARENTHESIS, ctx.toJson(tree.leftParenthesis()))
        .add(RIGHT_PARENTHESIS, ctx.toJson(tree.rightParenthesis())),

      (ctx, json) -> new ParenthesizedExpressionTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, EXPRESSION, Tree.class),
        ctx.fieldToToken(json, LEFT_PARENTHESIS),
        ctx.fieldToToken(json, RIGHT_PARENTHESIS)));

    register(PlaceHolderTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(PLACE_HOLDER_TOKEN, ctx.toJson(tree.placeHolderToken())),

      (ctx, json) -> new PlaceHolderTreeImpl(
        ctx.metaData(json),
        ctx.fieldToToken(json, PLACE_HOLDER_TOKEN)));

    register(ReturnTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(BODY, ctx.toJson(tree.body()))
        .add(KEYWORD, ctx.toJson(tree.keyword())),

      (ctx, json) -> new ReturnTreeImpl(
        ctx.metaData(json),
        ctx.fieldToToken(json, KEYWORD),
        ctx.fieldToNullableObject(json, BODY, Tree.class)));

    register(StringLiteralTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(CONTENT, tree.content())
        .add(VALUE, tree.value()),

      (ctx, json) -> new StringLiteralTreeImpl(
        ctx.metaData(json),
        ctx.fieldToString(json, VALUE),
        ctx.fieldToString(json, CONTENT)));

    register(ThrowTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(KEYWORD, ctx.toJson(tree.keyword()))
        .add(BODY, ctx.toJson(tree.body())),

      (ctx, json) -> new ThrowTreeImpl(
        ctx.metaData(json),
        ctx.fieldToToken(json, KEYWORD),
        ctx.fieldToNullableObject(json, BODY, Tree.class)));

    register(TopLevelTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(DECLARATIONS, ctx.toJsonArray(tree.declarations()))
        .add(FIRST_CPD_TOKEN, ctx.toJson(tree.firstCpdToken())),

      (ctx, json) -> {
        List<Tree> declarations = ctx.fieldToObjectList(json, DECLARATIONS, Tree.class);
        Token firstCpdToken = ctx.fieldToNullableToken(json, FIRST_CPD_TOKEN);
        TreeMetaData metaData = ctx.metaData(json);
        return new TopLevelTreeImpl(metaData, declarations, metaData.commentsInside(), firstCpdToken);
      });

    register(UnaryExpressionTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(OPERATOR, ctx.toJson(tree.operator()))
        .add(OPERAND, ctx.toJson(tree.operand())),

      (ctx, json) -> new UnaryExpressionTreeImpl(
        ctx.metaData(json),
        ctx.fieldToEnum(json, OPERATOR, UnaryExpressionTree.Operator.class),
        ctx.fieldToObject(json, OPERAND, Tree.class)));

    register(VariableDeclarationTreeImpl.class,

      (ctx, tree) -> ctx.newTypedObject(tree)
        .add(IDENTIFIER, ctx.toJson(tree.identifier()))
        .add(TYPE, ctx.toJson(tree.type()))
        .add(INITIALIZER, ctx.toJson(tree.initializer()))
        .add(IS_VAL, tree.isVal()),

      (ctx, json) -> new VariableDeclarationTreeImpl(
        ctx.metaData(json),
        ctx.fieldToObject(json, IDENTIFIER, IdentifierTree.class),
        ctx.fieldToNullableObject(json, TYPE, Tree.class),
        ctx.fieldToNullableObject(json, INITIALIZER, Tree.class),
        json.getBoolean(IS_VAL, false)));

  }

  private JsonTreeConverter() {
  }

  private static <T> void register(Class<T> treeClass, Serialize<T> treeToJson, Deserialize<T> jsonToTree) {
    String jsonType = treeClass.getSimpleName().replaceFirst("(TreeImpl|Impl)$", "");
    POLYMORPHIC_CONVERTER.register(treeClass, jsonType, treeToJson, jsonToTree);
  }

}
