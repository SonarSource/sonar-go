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
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.BaseTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.go.persistence.JsonTestHelper;
import org.sonar.go.persistence.JsonTree;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.COMMENT_FROM_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.COMMENT_TO_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CONTENT_RANGE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.CONTENT_TEXT;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TEXT;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TOKEN_FROM_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TOKEN_TO_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TREE_METADATA_PROVIDER_FROM_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TREE_METADATA_PROVIDER_TO_JSON;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TYPE;

class JsonTreeConverterTest extends JsonTestHelper {

  private SerializationContext writeContext = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
  private DeserializationContext readContext = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);

  @Test
  void tree_metadata_provider() throws IOException {
    Comment initialComment = comment(1, 0, "// hello", 2, 0);

    TextRange tokenRange = new TextRangeImpl(2, 0, 2, 3);
    Token initialToken = new TokenImpl(tokenRange, "fun", Token.Type.KEYWORD);

    TreeMetaDataProvider provider = new TreeMetaDataProvider(
      singletonList(initialComment),
      singletonList(initialToken));

    String actual = indentedJson(TREE_METADATA_PROVIDER_TO_JSON.apply(writeContext, provider).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("tree_metadata_provider.json"));

    provider = TREE_METADATA_PROVIDER_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(provider.allComments()).hasSize(1);
    Comment comment = provider.allComments().get(0);
    assertThat(comment.text()).isEqualTo("// hello");
    assertThat(comment.contentText()).isEqualTo(" hello");
    assertThat(comment.textRange()).isEqualTo(initialComment.textRange());
    assertThat(comment.contentRange()).isEqualTo(initialComment.contentRange());

    assertThat(provider.allTokens()).hasSize(1);
    Token token = provider.allTokens().get(0);
    assertThat(token.textRange()).isEqualTo(initialToken.textRange());
    assertThat(token.text()).isEqualTo("fun");
    assertThat(token.type()).isEqualTo(Token.Type.KEYWORD);

    assertThat(methodNames(TreeMetaDataProvider.class))
      .containsExactlyInAnyOrder("allComments", "previousToken", "updateTokenType", "firstToken",
        "allTokens", "indexOfFirstToken", "keyword");
  }

  @Test
  void comment() throws IOException {
    Comment initialComment = comment(3, 7, "// hello", 2, 0);
    String actual = indentedJson(COMMENT_TO_JSON.apply(writeContext, initialComment).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("comment.json"));
    Comment comment = COMMENT_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(comment.text()).isEqualTo("// hello");
    assertThat(comment.contentText()).isEqualTo(" hello");
    assertThat(comment.textRange()).isEqualTo(initialComment.textRange());
    assertThat(comment.contentRange()).isEqualTo(initialComment.contentRange());

    assertThat(methodNames(Comment.class))
      .containsExactlyInAnyOrder(TEXT, CONTENT_TEXT, CONTENT_RANGE);
  }

  @Test
  void token_other() throws IOException {
    Token initialToken = otherToken(3, 7, "foo");
    String actual = indentedJson(TOKEN_TO_JSON.apply(writeContext, initialToken).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("token_other.json"));
    Token token = TOKEN_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(token.textRange()).isEqualTo(initialToken.textRange());
    assertThat(token.text()).isEqualTo("foo");
    assertThat(token.type()).isEqualTo(Token.Type.OTHER);

    assertThat(methodNames(Token.class))
      .containsExactlyInAnyOrder(TEXT, TYPE);
  }

  @Test
  void token_keyword() throws IOException {
    Token initialToken = keywordToken(1, 2, "key");
    String actual = indentedJson(TOKEN_TO_JSON.apply(writeContext, initialToken).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("token_keyword.json"));
    Token token = TOKEN_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(token.textRange()).isEqualTo(initialToken.textRange());
    assertThat(token.text()).isEqualTo("key");
    assertThat(token.type()).isEqualTo(Token.Type.KEYWORD);
  }

  @Test
  void nativeTree_emptyKind() throws IOException {
    TreeMetaData metaData = metaData(otherToken(1, 0, "x"));
    IdentifierTree className = TreeCreationUtils.identifier(metaData, "MyClass");
    Tree classDecl = new NativeTreeImpl(metaData, new StringNativeKind(""), Collections.singletonList(className));
    String actual = indentedJson(JsonTree.toJson(classDecl));
    assertThat(actual).isEqualTo(indentedJsonFromFile("native_tree_empty_kind.json"));
  }

  @Test
  void nativeTree_withKind() throws IOException {
    TreeMetaData metaData = metaData(otherToken(1, 0, "x"));
    IdentifierTree className = TreeCreationUtils.identifier(metaData, "MyClass");
    Tree classDecl = new NativeTreeImpl(metaData, new StringNativeKind("kind"), Collections.singletonList(className));
    String actual = indentedJson(JsonTree.toJson(classDecl));
    assertThat(actual).isEqualTo(indentedJsonFromFile("native_tree_with_kind.json"));
  }

  @Test
  void error_missing_type() throws IOException {
    String invalidJson = indentedJsonFromFile("error_missing_type.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Missing non-null value for field '@type' at 'tree/Return/body'" +
      " member: {\"invalid_type\":\"Literal\",\"metaData\":\"1:7:1:11\",\"value\":\"true\"}");
  }

  @Test
  void error_invalid_json_tree() throws IOException {
    String invalidJson = indentedJsonFromFile("error_invalid_json_tree.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Unexpected value for Tree at 'tree/Return/body' member: 1234");
  }

  @Test
  void error_invalid_tree_type() throws IOException {
    String invalidJson = indentedJsonFromFile("error_invalid_tree_type.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Invalid '@type' value at 'tree/Return/body/UnsupportedType' member: UnsupportedType");
  }

  @Test
  void error_unsupported_tree_class() throws IOException {
    Token token = otherToken(1, 0, "x");
    UnsupportedTree tree = new UnsupportedTree(metaData(token));
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.toJson(tree));
    assertThat(e).hasMessage("Unsupported tree class: org.sonar.go.persistence.conversion.JsonTreeConverterTest$UnsupportedTree");
  }

  @Test
  void error_unsupported_implementation_class() throws IOException {
    Token token = otherToken(1, 0, "x");
    UnsupportedTree tree = new UnsupportedTree(metaData(token));
    SerializationContext ctx = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> ctx.newTypedObject(tree));
    assertThat(e).hasMessage("Unsupported implementation class: org.sonar.go.persistence.conversion.JsonTreeConverterTest$UnsupportedTree");
  }

  class UnsupportedTree extends BaseTreeImpl {
    public UnsupportedTree(TreeMetaData metaData) {
      super(metaData);
    }

    @Override
    public List<Tree> children() {
      return emptyList();
    }
  }

  @Test
  void error_unexpected_match_child_class() throws IOException {
    String invalidJson = indentedJsonFromFile("error_unexpected_match_child_class.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Unexpected 'org.sonar.go.impl.IntegerLiteralTreeImpl'" +
      " type for member 'cases[]' instead of" +
      " 'org.sonar.plugins.go.api.MatchCaseTree'" +
      " at 'tree/Match/cases[]/IntegerLiteral'" +
      " member: {\"@type\":\"IntegerLiteral\",\"metaData\":\"1:17:1:19\",\"value\":\"42\"}");
  }

  @Test
  void error_unary_expression_without_child() throws IOException {
    String invalidJson = indentedJsonFromFile("error_unary_expression_without_child.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Unexpected null value for field 'operand' at 'tree/UnaryExpression' member: null");
  }

  @Test
  void error_unary_expression_with_null_child() throws IOException {
    String invalidJson = indentedJsonFromFile("error_unary_expression_with_null_child.json");
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> JsonTree.fromJson(invalidJson));
    assertThat(e).hasMessage("Unexpected null value for field 'operand' at 'tree/UnaryExpression' member: null");
  }

  @Test
  void nullable_child_can_be_omitted() throws IOException {
    JumpTree jump = (JumpTree) JsonTree.fromJson(indentedJsonFromFile("nullable_child_can_be_omitted.json"));
    assertThat(jump.label()).isNull();
  }

}
