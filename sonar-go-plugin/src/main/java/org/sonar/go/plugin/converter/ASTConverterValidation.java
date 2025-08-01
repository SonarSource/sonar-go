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
package org.sonar.go.plugin.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.TextPointerImpl;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.TextPointer;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.TreeOrError;

import static org.sonar.go.utils.LogArg.lazyArg;

public class ASTConverterValidation implements ASTConverter {

  private static final Logger LOG = LoggerFactory.getLogger(ASTConverterValidation.class);

  private static final Pattern PUNCTUATOR_PATTERN = Pattern.compile("[^0-9A-Za-z]++");

  private static final Set<String> ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE = new HashSet<>(Collections.singleton("implicit"));

  private final ASTConverter wrapped;

  private final Map<String, String> firstErrorOfEachKind = new TreeMap<>();

  private final ValidationMode mode;

  public enum ValidationMode {
    THROW_EXCEPTION,
    LOG_ERROR
  }

  @Nullable
  private String currentFile = null;

  public ASTConverterValidation(ASTConverter wrapped, ValidationMode mode) {
    this.wrapped = wrapped;
    this.mode = mode;
  }

  public static ASTConverter wrap(ASTConverter converter, Configuration configuration) {
    String mode = configuration.get("sonar.go.converter.validation").orElse(null);
    if (mode == null) {
      return converter;
    } else if (mode.equals("throw")) {
      return new ASTConverterValidation(converter, ValidationMode.THROW_EXCEPTION);
    } else if (mode.equals("log")) {
      return new ASTConverterValidation(converter, ValidationMode.LOG_ERROR);
    } else {
      LOG.warn("Unsupported mode for converter validation: '{}', falling back to no validation", mode);
      return converter;
    }
  }

  @Override
  public Map<String, TreeOrError> parse(Map<String, String> filenameToContentMap, String moduleName) {
    var filenamesToTrees = wrapped.parse(filenameToContentMap, moduleName);
    var result = new HashMap<String, TreeOrError>();
    for (Map.Entry<String, TreeOrError> filenameToTree : filenamesToTrees.entrySet()) {
      currentFile = filenameToTree.getKey();
      var treeOrError = filenameToTree.getValue();
      if (treeOrError.isTree()) {
        var tree = treeOrError.tree();
        try {
          assertTreeIsValid(tree);
          assertTokensMatchSourceCode(tree, filenameToContentMap.get(currentFile));
          result.put(currentFile, treeOrError);
        } catch (RuntimeException e) {
          // Let's acknowledge catching exceptions here is definitely not a good design.
          // We went from file-by-file to batch, this validation now requires to validate multiple files at once,
          // we cannot throw an exception for a single file anymore. Changing this would require a lot of refactoring, we don't want to invest in this
          // right now.
          result.put(currentFile, TreeOrError.of("AST validation failed: " + e.getMessage()));
        }
      } else {
        result.put(currentFile, treeOrError);
      }
    }

    return result;
  }

  @Override
  public void debugTypeCheck() {
    // Do nothing
  }

  @Override
  public void terminate() {
    List<String> errors = errors();
    if (!errors.isEmpty()) {
      String delimiter = "\n  [AST ERROR] ";
      LOG.warn("AST Converter Validation detected {} errors:{}{}", errors.size(), delimiter, lazyArg(() -> String.join(delimiter, errors)));
    }
    wrapped.terminate();
  }

  List<String> errors() {
    return firstErrorOfEachKind.entrySet().stream()
      .map(entry -> entry.getKey() + entry.getValue())
      .toList();
  }

  private void raiseError(String messageKey, String messageDetails, TextPointer position) {
    if (mode == ValidationMode.THROW_EXCEPTION) {
      throw new IllegalStateException("ASTConverterValidationException: " + messageKey + messageDetails +
        " at  " + position.line() + ":" + position.lineOffset());
    } else {
      String positionDetails = String.format(" (line: %d, column: %d)", position.line(), (position.lineOffset() + 1));
      if (currentFile != null) {
        positionDetails += " in file: " + currentFile;
      }
      firstErrorOfEachKind.putIfAbsent(messageKey, messageDetails + positionDetails);
    }
  }

  private static String kind(Tree tree) {
    return tree.getClass().getSimpleName();
  }

  private void assertTreeIsValid(Tree tree) {
    assertTextRangeIsValid(tree);
    assertTreeHasAtLeastOneToken(tree);
    assertTokensAndChildTokens(tree);
    for (Tree child : tree.children()) {
      if (child == null) {
        raiseError(kind(tree) + " has a null child", "", tree.textRange().start());
      } else if (child.metaData() == null) {
        raiseError(kind(child) + " metaData is null", "", tree.textRange().start());
      } else {
        assertTreeIsValid(child);
      }
    }
  }

  private void assertTextRangeIsValid(Tree tree) {
    TextPointer start = tree.metaData().textRange().start();
    TextPointer end = tree.metaData().textRange().end();

    boolean startOffsetAfterEndOffset = !(tree instanceof TopLevelTree) &&
      start.line() == end.line() &&
      start.lineOffset() >= end.lineOffset();

    if (start.line() <= 0 || end.line() <= 0 ||
      start.line() > end.line() ||
      start.lineOffset() < 0 || end.lineOffset() < 0 ||
      startOffsetAfterEndOffset) {
      raiseError(kind(tree) + " invalid range ", tree.metaData().textRange().toString(), start);
    }
  }

  private void assertTreeHasAtLeastOneToken(Tree tree) {
    if (!(tree instanceof TopLevelTree) && tree.metaData().tokens().isEmpty()) {
      raiseError(kind(tree) + " has no token", "", tree.textRange().start());
    }
  }

  private void assertTokensMatchSourceCode(Tree tree, String code) {
    CodeFormToken codeFormToken = new CodeFormToken(tree.metaData());
    codeFormToken.assertEqualTo(code);
  }

  private void assertTokensAndChildTokens(Tree tree) {
    assertTokensAreInsideRange(tree);
    Set<Token> parentTokens = new HashSet<>(tree.metaData().tokens());
    Map<Token, Tree> childByToken = new HashMap<>();
    for (Tree child : tree.children()) {
      if (child != null && child.metaData() != null && !isAllowedMisplacedTree(child)) {
        assertChildRangeIsInsideParentRange(tree, child);
        assertChildTokens(parentTokens, childByToken, tree, child);
      }
    }
    parentTokens.removeAll(childByToken.keySet());
    assertUnexpectedTokenKind(tree, parentTokens);
  }

  private static boolean isAllowedMisplacedTree(Tree tree) {
    List<Token> tokens = tree.metaData().tokens();
    return tokens.size() == 1 && ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(tokens.get(0).text());
  }

  private void assertUnexpectedTokenKind(Tree tree, Set<Token> tokens) {
    if (tree instanceof NativeTreeImpl || tree instanceof LiteralTreeImpl || tree instanceof PlaceHolderTreeImpl) {
      return;
    }
    List<Token> unexpectedTokens;
    if (tree instanceof IdentifierTree) {
      unexpectedTokens = tokens.stream()
        .filter(token -> token.type() == Token.Type.KEYWORD || token.type() == Token.Type.STRING_LITERAL)
        .toList();
    } else {
      unexpectedTokens = tokens.stream()
        .filter(token -> token.type() != Token.Type.KEYWORD)
        .filter(token -> !PUNCTUATOR_PATTERN.matcher(token.text()).matches())
        .filter(token -> !ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(token.text()))
        .toList();
    }
    if (!unexpectedTokens.isEmpty()) {
      String tokenList = unexpectedTokens.stream()
        .sorted(Comparator.comparing(token -> token.textRange().start()))
        .map(Token::text)
        .collect(Collectors.joining("', '"));
      raiseError("Unexpected tokens in " + kind(tree), ": '" + tokenList + "'", tree.textRange().start());
    }
  }

  private void assertTokensAreInsideRange(Tree tree) {
    TextRange parentRange = tree.metaData().textRange();
    tree.metaData().tokens().stream()
      .filter(token -> !ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(token.text()))
      .filter(token -> !token.textRange().isInside(parentRange))
      .findFirst()
      .ifPresent(token -> raiseError(
        kind(tree) + " contains a token outside its range",
        " range: " + parentRange + " tokenRange: " + token.textRange() + " token: '" + token.text() + "'",
        token.textRange().start()));
  }

  private void assertChildRangeIsInsideParentRange(Tree parent, Tree child) {
    TextRange parentRange = parent.metaData().textRange();
    TextRange childRange = child.metaData().textRange();
    if (!childRange.isInside(parentRange)) {
      raiseError(kind(parent) + " contains a child " + kind(child) + " outside its range",
        ", parentRange: " + parentRange + " childRange: " + childRange,
        childRange.start());
    }
  }

  private void assertChildTokens(Set<Token> parentTokens, Map<Token, Tree> childByToken, Tree parent, Tree child) {
    for (Token token : child.metaData().tokens()) {
      if (!parentTokens.contains(token)) {
        raiseError(kind(child) + " contains a token missing in its parent " + kind(parent),
          ", token: '" + token.text() + "'",
          token.textRange().start());
      }
      Tree intersectingChild = childByToken.get(token);
      if (intersectingChild != null) {
        raiseError(kind(parent) + " has a token used by both children " + kind(intersectingChild) + " and " + kind(child),
          ", token: '" + token.text() + "'",
          token.textRange().start());
      } else {
        childByToken.put(token, child);
      }
    }
  }

  private class CodeFormToken {

    private final StringBuilder code = new StringBuilder();
    private final List<Comment> commentsInside;
    private int lastLine = 1;
    private int lastLineOffset = 0;
    private int lastComment = 0;

    private CodeFormToken(TreeMetaData metaData) {
      this.commentsInside = metaData.commentsInside();
      metaData.tokens().forEach(this::add);
      addRemainingComments();
    }

    private void add(Token token) {
      while (lastComment < commentsInside.size() &&
        commentsInside.get(lastComment).textRange().start().compareTo(token.textRange().start()) < 0) {
        Comment comment = commentsInside.get(lastComment);
        addTextAt(comment.text(), comment.textRange());
        lastComment++;
      }
      addTextAt(token.text(), token.textRange());
    }

    private void addRemainingComments() {
      for (int i = lastComment; i < commentsInside.size(); i++) {
        addTextAt(commentsInside.get(i).text(), commentsInside.get(i).textRange());
      }
    }

    private void addTextAt(String text, TextRange textRange) {
      while (lastLine < textRange.start().line()) {
        code.append("\n");
        lastLine++;
        lastLineOffset = 0;
      }
      while (lastLineOffset < textRange.start().lineOffset()) {
        code.append(' ');
        lastLineOffset++;
      }
      code.append(text);
      lastLine = textRange.end().line();
      lastLineOffset = textRange.end().lineOffset();
    }

    private void assertEqualTo(String expectedCode) {
      String[] actualLines = lines(this.code.toString());
      String[] expectedLines = lines(expectedCode);
      for (int i = 0; i < actualLines.length && i < expectedLines.length; i++) {
        if (!actualLines[i].equals(expectedLines[i])) {
          raiseError("Unexpected AST difference", ":\n" +
            "      Actual   : " + actualLines[i] + "\n" +
            "      Expected : " + expectedLines[i] + "\n",
            new TextPointerImpl(i + 1, 0));
        }
      }
      if (actualLines.length != expectedLines.length) {
        raiseError(
          "Unexpected AST number of lines",
          " actual: " + actualLines.length + ", expected: " + expectedLines.length,
          new TextPointerImpl(Math.min(actualLines.length, expectedLines.length), 0));
      }
    }

    private String[] lines(String code) {
      return code
        .replace('\t', ' ')
        .replaceFirst("[\r\n ]+$", "")
        .split(" *(\r\n|\n|\r)", -1);
    }
  }

}
