/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.Location;
import apex.jorje.data.ast.CompilationUnit;
import apex.jorje.parser.impl.ApexLexer;
import apex.jorje.parser.impl.ApexParser;
import apex.jorje.parser.impl.CaseInsensitiveReaderStream;
import apex.jorje.services.Version;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.impl.TextPointerImpl;

class Parser {

  @FunctionalInterface
  interface GrammarEntryPoint {
    CompilationUnit apply(ApexParser parser) throws RecognitionException;
  }

  private static final GrammarEntryPoint NAMED_COMPILATION_UNIT = ApexParser::compilationUnit;

  private static final GrammarEntryPoint ANONYMOUS_FREE_CODE = ApexParser::anonymousBlockUnit;

  private Parser() {
    // utility class
  }

  static CompilationUnit parse(String source) {
    ParserOutput result = parse(source, NAMED_COMPILATION_UNIT);
    if (!result.exceptions.isEmpty()) {
      result = parse(source, ANONYMOUS_FREE_CODE);
    }
    return result.getOrElseThrowParseException();
  }

  static CompilationUnit parseFreeCode(String source) {
    return parse(source, ANONYMOUS_FREE_CODE).getOrElseThrowParseException();
  }

  private static ParserOutput parse(String source, GrammarEntryPoint grammar) {
    CharStream stream = CaseInsensitiveReaderStream.create(source);
    ApexLexer lexer = new Lexer.InternalApexLexer(stream);
    TokenStream tokenStream = new CommonTokenStream(lexer);
    ApexParser parser = new ApexParser(tokenStream);
    parser.setVersion(Version.CURRENT);
    CompilationUnit compilationUnit = null;
    List<Exception> exceptions = new ArrayList<>();
    try {
      compilationUnit = grammar.apply(parser);
    } catch (RecognitionException e) {
      exceptions.add(e);
    }
    exceptions.addAll(lexer.getParseErrors());
    exceptions.addAll(lexer.getInternalErrors());
    exceptions.addAll(parser.getParseErrors());
    exceptions.addAll(parser.getInternalErrors());
    return new ParserOutput(compilationUnit, exceptions);
  }

  private static class ParserOutput {
    @Nullable
    private final CompilationUnit node;
    private final List<Exception> exceptions;

    private ParserOutput(@Nullable CompilationUnit node, List<Exception> exceptions) {
      this.node = node;
      this.exceptions = exceptions;
    }

    private CompilationUnit getOrElseThrowParseException() {
      if (exceptions.isEmpty()) {
        return node;
      }
      Exception firstException = exceptions.get(0);
      throw convertToParseException(firstException);
    }
  }

  static ParseException convertToParseException(Exception exception) {
    TextPointer location = null;
    if (exception instanceof apex.jorje.services.exception.ParseException) {
      location = toTextPointer(((apex.jorje.services.exception.ParseException) exception).getLoc());
    } else if (exception instanceof RecognitionException) {
      RecognitionException recognitionException = (RecognitionException) exception;
      location = new TextPointerImpl(recognitionException.line, recognitionException.charPositionInLine);
    }
    return new ParseException("ParseException: " + exception.getMessage(), location, exception);
  }

  @CheckForNull
  static TextPointer toTextPointer(@Nullable Location location) {
    if (location == null || location.getLine() < 1 || location.getColumn() < 1) {
      return null;
    }
    return new TextPointerImpl(location.getLine(), location.getColumn() - 1);
  }

}
