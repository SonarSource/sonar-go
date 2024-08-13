/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.Locations;
import apex.jorje.parser.impl.ApexLexer;
import apex.jorje.parser.impl.BaseApexLexer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;

public final class Lexer {

  /** {@link ApexLexer#mBLOCK_COMMENT_START()} */
  private static final int BLOCK_COMMENT_START_LENGTH = "/*".length();

  /** {@link ApexLexer#mBLOCK_COMMENT_END()} */
  private static final int BLOCK_COMMENT_END_LENGTH = "*/".length();

  /** {@link ApexLexer#mEOL_COMMENT()} {@link ApexLexer#mSLASH()} */
  private static final int EOL_COMMENT_LENGTH = "//".length();

  enum WordType {
    KEYWORD,
    IDENTIFIER,
    KEYWORD_OR_IDENTIFIER
  }

  static final Set<String> RESERVED_WORDS = new TreeSet<>();
  static final Set<String> IDENTIFIERS = new HashSet<>();
  static final Set<String> KEYWORDS = new HashSet<>();
  static final Set<String> IDENTIFIERS_OR_KEYWORDS = new TreeSet<>();

  private static void set(String word, WordType type) {
    RESERVED_WORDS.add(word);
    switch (type) {
      case KEYWORD:
        KEYWORDS.add(word);
        break;
      case IDENTIFIER:
        IDENTIFIERS.add(word);
        break;
      case KEYWORD_OR_IDENTIFIER:
        IDENTIFIERS_OR_KEYWORDS.add(word);
        break;
    }
  }

  static {
    set("abstract", WordType.KEYWORD);
    set("activate", WordType.IDENTIFIER);
    set("after", WordType.KEYWORD_OR_IDENTIFIER);
    set("and", WordType.KEYWORD);
    set("any", WordType.IDENTIFIER);
    set("array", WordType.IDENTIFIER);
    set("as", WordType.KEYWORD);
    set("asc", WordType.KEYWORD);
    set("autonomous", WordType.IDENTIFIER);
    set("before", WordType.KEYWORD_OR_IDENTIFIER);
    set("begin", WordType.IDENTIFIER);
    set("bigdecimal", WordType.IDENTIFIER);
    set("blob", WordType.IDENTIFIER);
    set("break", WordType.KEYWORD);
    set("bulk", WordType.KEYWORD);
    set("by", WordType.KEYWORD);
    set("byte", WordType.IDENTIFIER);
    set("case", WordType.IDENTIFIER);
    set("cast", WordType.IDENTIFIER);
    set("catch", WordType.KEYWORD);
    set("char", WordType.IDENTIFIER);
    set("class", WordType.KEYWORD);
    set("collect", WordType.IDENTIFIER);
    set("commit", WordType.KEYWORD);
    set("const", WordType.IDENTIFIER);
    set("continue", WordType.KEYWORD);
    set("convertcurrency", WordType.IDENTIFIER);
    set("count", WordType.IDENTIFIER);
    set("decimal", WordType.IDENTIFIER);
    set("default", WordType.IDENTIFIER);
    set("delete", WordType.KEYWORD);
    set("desc", WordType.KEYWORD);
    set("do", WordType.KEYWORD);
    set("else", WordType.KEYWORD);
    set("end", WordType.IDENTIFIER);
    set("enum", WordType.KEYWORD);
    set("exception", WordType.IDENTIFIER);
    set("excludes", WordType.IDENTIFIER);
    set("exit", WordType.IDENTIFIER);
    set("export", WordType.IDENTIFIER);
    set("extends", WordType.KEYWORD);
    set("false", WordType.KEYWORD);
    set("final", WordType.KEYWORD);
    set("finally", WordType.KEYWORD);
    set("first", WordType.IDENTIFIER);
    set("float", WordType.IDENTIFIER);
    set("for", WordType.KEYWORD);
    set("from", WordType.KEYWORD);
    set("future", WordType.IDENTIFIER);
    set("global", WordType.KEYWORD);
    set("goto", WordType.IDENTIFIER);
    set("group", WordType.IDENTIFIER);
    set("having", WordType.KEYWORD);
    set("hint", WordType.IDENTIFIER);
    set("if", WordType.KEYWORD);
    set("implements", WordType.KEYWORD);
    set("import", WordType.IDENTIFIER);
    set("in", WordType.KEYWORD);
    set("includes", WordType.IDENTIFIER);
    set("inner", WordType.IDENTIFIER);
    set("insert", WordType.KEYWORD);
    set("instanceof", WordType.KEYWORD);
    set("int", WordType.IDENTIFIER);
    set("interface", WordType.KEYWORD);
    set("into", WordType.IDENTIFIER);
    set("join", WordType.IDENTIFIER);
    set("last", WordType.IDENTIFIER);
    set("last_90_days", WordType.IDENTIFIER);
    set("last_month", WordType.IDENTIFIER);
    set("last_n_days", WordType.IDENTIFIER);
    set("last_week", WordType.IDENTIFIER);
    set("like", WordType.KEYWORD);
    set("limit", WordType.KEYWORD);
    set("list", WordType.IDENTIFIER);
    set("long", WordType.IDENTIFIER);
    set("loop", WordType.IDENTIFIER);
    set("map", WordType.IDENTIFIER);
    set("merge", WordType.KEYWORD);
    set("new", WordType.KEYWORD);
    set("next_90_days", WordType.IDENTIFIER);
    set("next_month", WordType.IDENTIFIER);
    set("next_n_days", WordType.IDENTIFIER);
    set("next_week", WordType.IDENTIFIER);
    set("not", WordType.KEYWORD);
    set("null", WordType.KEYWORD);
    set("nulls", WordType.KEYWORD);
    set("number", WordType.IDENTIFIER);
    set("object", WordType.IDENTIFIER);
    set("of", WordType.IDENTIFIER);
    set("on", WordType.KEYWORD);
    set("or", WordType.KEYWORD);
    set("order", WordType.IDENTIFIER);
    set("outer", WordType.IDENTIFIER);
    set("override", WordType.KEYWORD);
    set("package", WordType.IDENTIFIER);
    set("parallel", WordType.IDENTIFIER);
    set("pragma", WordType.IDENTIFIER);
    set("private", WordType.KEYWORD);
    set("protected", WordType.KEYWORD);
    set("public", WordType.KEYWORD);
    set("retrieve", WordType.IDENTIFIER);
    set("return", WordType.KEYWORD);
    set("returning", WordType.IDENTIFIER);
    set("rollback", WordType.IDENTIFIER);
    set("savepoint", WordType.IDENTIFIER);
    set("search", WordType.IDENTIFIER);
    set("select", WordType.KEYWORD);
    set("set", WordType.IDENTIFIER);
    set("sharing", WordType.IDENTIFIER);
    set("short", WordType.IDENTIFIER);
    set("sort", WordType.IDENTIFIER);
    set("stat", WordType.IDENTIFIER);
    set("static", WordType.KEYWORD);
    set("super", WordType.KEYWORD);
    set("switch", WordType.KEYWORD_OR_IDENTIFIER);
    set("synchronized", WordType.IDENTIFIER);
    set("system", WordType.IDENTIFIER);
    set("testmethod", WordType.KEYWORD);
    set("then", WordType.IDENTIFIER);
    set("this", WordType.KEYWORD);
    set("this_month", WordType.IDENTIFIER);
    set("this_week", WordType.IDENTIFIER);
    set("throw", WordType.KEYWORD);
    set("today", WordType.IDENTIFIER);
    set("tolabel", WordType.IDENTIFIER);
    set("tomorrow", WordType.IDENTIFIER);
    set("transaction", WordType.IDENTIFIER);
    set("trigger", WordType.KEYWORD);
    set("true", WordType.KEYWORD);
    set("try", WordType.KEYWORD);
    set("type", WordType.IDENTIFIER);
    set("undelete", WordType.KEYWORD);
    set("update", WordType.KEYWORD);
    set("upsert", WordType.KEYWORD);
    set("using", WordType.KEYWORD);
    set("virtual", WordType.KEYWORD);
    set("webservice", WordType.KEYWORD);
    set("when", WordType.KEYWORD_OR_IDENTIFIER);
    set("where", WordType.KEYWORD);
    set("while", WordType.KEYWORD);
    set("with", WordType.IDENTIFIER);
    set("yesterday", WordType.IDENTIFIER);
  }

  private Lexer() {
    // utility class
  }

  // Converts ANTLR tokens provided by the ApexLexer into Slang tokens
  static Tokens parse(String code) {
    LineSet lineSet = new LineSet(code);
    Tokens tokens = new Tokens(lineSet);
    ApexLexer lexer = new InternalApexLexer(new ANTLRStringStream(code));
    CommonToken token = (CommonToken) lexer.nextToken();
    while (token.getType() != org.antlr.runtime.Token.EOF) {
      switch (token.getType()) {
        case ApexLexer.BLOCK_COMMENT:
          tokens.addBlockComment(token);
          break;
        case ApexLexer.EOL_COMMENT:
          tokens.addEolComment(token);
          break;
        case ApexLexer.WS:
          // ignore whiteSpace
          break;
        case ApexLexer.IDENTIFIER:
          tokens.addIdentifier(token);
          break;
        default:
          tokens.addNonCommentToken(token);
      }
      token = (CommonToken) lexer.nextToken();
    }
    return tokens;
  }

  public static class Tokens {

    final LineSet lineSet;
    final List<Token> tokensWithoutComments = new ArrayList<>();
    final List<Comment> comments = new ArrayList<>();
    private TextPointer start = new TextPointerImpl(1, 0);
    private TextPointer end = new TextPointerImpl(1, 0);

    Tokens(LineSet lineSet) {
      this.lineSet = lineSet;
    }

    private void addBlockComment(CommonToken token) {
      String text = text(token);
      TextRange textRange = textRange(token);
      String contentText = text.substring(BLOCK_COMMENT_START_LENGTH, text.length() - BLOCK_COMMENT_END_LENGTH);
      TextRange contentRange = new TextRangeImpl(
        new TextPointerImpl(textRange.start().line(), textRange.start().lineOffset() + BLOCK_COMMENT_START_LENGTH),
        new TextPointerImpl(textRange.end().line(), textRange.end().lineOffset() - BLOCK_COMMENT_END_LENGTH));
      comments.add(new CommentImpl(text, contentText, textRange, contentRange));
      increaseRange(textRange);
    }

    private void addEolComment(CommonToken token) {
      String text = text(token);
      TextRange textRange = textRange(token);
      String contentText = text.substring(EOL_COMMENT_LENGTH);
      TextRange contentRange = new TextRangeImpl(
        new TextPointerImpl(textRange.start().line(), textRange.start().lineOffset() + EOL_COMMENT_LENGTH),
        textRange.end());
      comments.add(new CommentImpl(text, contentText, textRange, contentRange));
      increaseRange(textRange);
    }

    private void addIdentifier(CommonToken token) {
      String text = text(token);
      if (text.indexOf('.') == -1) {
        addNonCommentToken(token);
        return;
      }
      int tokenStart = token.getStartIndex();
      int textStart = 0;
      int dotPos = text.indexOf('.');
      while (dotPos != -1) {
        int textEnd = dotPos;
        String subText = text.substring(textStart, textEnd);
        TextRange subTextRange = lineSet.range(tokenStart + textStart, tokenStart + textEnd);
        tokensWithoutComments.add(new TokenImpl(subTextRange, subText, Token.Type.OTHER));
        TextRange dotRange = lineSet.range(tokenStart + dotPos, tokenStart + dotPos + 1);
        tokensWithoutComments.add(new TokenImpl(dotRange, ".", Token.Type.OTHER));
        textStart = textEnd + 1;
        dotPos = text.indexOf('.', textStart);
      }
      TextRange subTextRange = lineSet.range(tokenStart + textStart, token.getStopIndex() + 1);
      String subText = text.substring(textStart);
      tokensWithoutComments.add(new TokenImpl(subTextRange, subText, Token.Type.OTHER));
      increaseRange(subTextRange);
    }

    private void addNonCommentToken(CommonToken token) {
      String text = text(token);
      TextRange textRange = textRange(token);
      Token.Type type;
      if (token.getType() == ApexLexer.STRING_LITERAL) {
        type = Token.Type.STRING_LITERAL;
      } else if (KEYWORDS.contains(text.toLowerCase(Locale.ROOT))) {
        type = Token.Type.KEYWORD;
      } else {
        type = Token.Type.OTHER;
      }
      tokensWithoutComments.add(new TokenImpl(textRange, text, type));
      increaseRange(textRange);
    }

    private void increaseRange(TextRange range) {
      if (end.compareTo(range.end()) < 0) {
        end = range.end();
      }
    }

    TextRange range() {
      return new TextRangeImpl(start, end);
    }

    private String text(CommonToken token) {
      // Can't rely on token.getText(), e.g. with STRING_LITERAL, surrounding simple quotes are missing
      return lineSet.substring(token.getStartIndex(), token.getStopIndex() + 1);
    }

    private TextRange textRange(CommonToken token) {
      return lineSet.range(token.getStartIndex(), token.getStopIndex() + 1);
    }

  }

  static class InternalApexLexer extends ApexLexer {

    static {
      // Disable unwanted ApexLexer INFO log (for example: "INFO: Deduped array ApexLexer.DFA22_transition")
      Logger.getLogger(BaseApexLexer.class.getName()).setLevel(java.util.logging.Level.WARNING);
      // Fulfill apex.jorje.data.Location "getStartIndex()" and "getEndIndex()" during parsing
      Locations.useIndexFactory();
    }

    InternalApexLexer(CharStream input) {
      super(input);
    }

    // Override to prevent "System.err.println"
    @Override
    public void emitErrorMessage(String msg) {
      throw new IllegalStateException("Parse error:" + msg);
    }

  }

}
