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
package org.sonar.go.persistence;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.WriterConfig;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.go.impl.CommentImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TextRanges;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.HasTextRange;
import org.sonar.plugins.go.api.TextPointer;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonTestHelper {

  protected TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(Collections.emptyList(), Collections.emptyList());

  protected Token token(int line, int lineOffset, String text, Token.Type type) {
    TextRange tokenRange = new TextRangeImpl(line, lineOffset, line, lineOffset + text.length());
    Token token = new TokenImpl(tokenRange, text, type);
    metaDataProvider.allTokens().add(token);
    metaDataProvider.allTokens().sort(TreeMetaDataProvider.COMPARATOR);
    return token;
  }

  protected Token stringToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.STRING_LITERAL);
  }

  protected Comment comment(int line, int lineOffset, String commentText, int prefixLength, int suffixLength) {
    String commentContentText = commentText.substring(prefixLength, commentText.length() - suffixLength);
    TextRange commentRange = new TextRangeImpl(line, lineOffset, line, lineOffset + commentText.length());
    TextRange commentContentRange = new TextRangeImpl(line, lineOffset + prefixLength,
      line, lineOffset + commentText.length() - suffixLength);
    CommentImpl comment = new CommentImpl(commentText, commentContentText, commentRange, commentContentRange);
    metaDataProvider.allComments().add(comment);
    metaDataProvider.allComments().sort(TreeMetaDataProvider.COMPARATOR);
    return comment;
  }

  protected Token otherToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.OTHER);
  }

  protected Token keywordToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.KEYWORD);
  }

  protected TreeMetaData metaData(TextRange textRange) {
    return metaDataProvider.metaData(textRange);
  }

  protected TreeMetaData metaData(Token token) {
    return metaData(token.textRange());
  }

  protected TreeMetaData metaData(HasTextRange from, HasTextRange to) {
    return metaData(TextRanges.merge(Arrays.asList(from.textRange(), to.textRange())));
  }

  protected static String indentedJson(String json) throws IOException {
    return Json.parse(json).toString(WriterConfig.PRETTY_PRINT);
  }

  protected static String indentedJsonFromFile(String fileName) throws IOException {
    Path path = Paths.get("src", "test", "resources", "org", "sonar", "go", "persistence", fileName);
    return indentedJson(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
  }

  public static <T extends Tree> T checkJsonSerializationDeserialization(T initialTree, String fileName) throws IOException {
    String initialTreeAsJson = indentedJson(JsonTree.toJson(initialTree));
    String expectedJson = indentedJsonFromFile(fileName);
    assertThat(initialTreeAsJson)
      .describedAs("Comparing tree serialized into json with " + fileName)
      .isEqualTo(expectedJson);

    T loadedTree = (T) JsonTree.fromJsonSingleTree(initialTreeAsJson);
    String loadedTreeAsJson = indentedJson(JsonTree.toJson(loadedTree));
    assertThat(loadedTreeAsJson)
      .describedAs("Comparing tree de-serialized/serialized into json with " + fileName)
      .isEqualTo(expectedJson);

    return loadedTree;
  }

  public static String tokens(Tree tree) {
    return tokens(tree.metaData());
  }

  public static String tokens(TreeMetaData metaData) {
    return tokens(metaData.tokens());
  }

  public static String tokens(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    TextPointer start = tokens.get(0).textRange().start();
    TextPointer end = tokens.get(tokens.size() - 1).textRange().end();
    return start.line() + ":" + start.lineOffset() + ":" + end.line() + ":" + end.lineOffset() + " - " +
      tokens.stream().map(Token::text).collect(Collectors.joining(" "));
  }

  public static String token(Token token) {
    TextPointer start = token.textRange().start();
    TextPointer end = token.textRange().end();
    return start.line() + ":" + start.lineOffset() + ":" + end.line() + ":" + end.lineOffset() + " - " +
      token.text();
  }

  public static List<String> methodNames(Class<?> cls) {
    List<String> ignoredMethods = Arrays.asList(
      "children", "descendants", "descendantsDepthFirst", "metaData", "textRange", "wait", "equals", "toString",
      "hashCode", "getClass", "notify", "notifyAll");
    return new ArrayList<>(Stream.of(cls.getMethods())
      .map(Method::getName)
      .filter(name -> !ignoredMethods.contains(name))
      .collect(Collectors.toSet()));
  }

}
