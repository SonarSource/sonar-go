package org.sonarsource.apex.parser.poc.parser;

import apex.jorje.data.ast.CompilationUnit;
import apex.jorje.parser.impl.ApexLexer;
import apex.jorje.parser.impl.ApexParser;
import apex.jorje.parser.impl.CaseInsensitiveReaderStream;
import apex.jorje.parser.impl.HiddenTokenDecorator;
import apex.jorje.parser.impl.TokenSourceDecorator;
import apex.jorje.services.Version;
import java.util.List;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.sonarsource.apex.parser.poc.lexer.Lexer;
import org.sonarsource.apex.parser.poc.lexer.Tokens;

public class Parser {

  private Parser() {
    // utility class
  }

  public static SimpleAstNode parse(String source) throws RecognitionException {
    Tokens tokens = Lexer.parse(source);

    CharStream stream = CaseInsensitiveReaderStream.create(source);
    ApexLexer lexer = new ApexLexer(stream);
    throwFirstIfNotEmpty(lexer.getInternalErrors());
    throwFirstIfNotEmpty(lexer.getParseErrors());
    TokenSourceDecorator tokenSourceDecorator = new HiddenTokenDecorator(lexer);
    TokenStream tokenStream = new CommonTokenStream(tokenSourceDecorator);
    ApexParser parser = new ApexParser(tokenStream);
    parser.setVersion(Version.CURRENT);
    CompilationUnit compilationUnit = parser.compilationUnit();
    if (!parser.getParseErrors().isEmpty()) {
      compilationUnit = parser.anonymousBlockUnit();
    }
    throwFirstIfNotEmpty(parser.getInternalErrors());
    throwFirstIfNotEmpty(parser.getParseErrors());

    SimpleConverterGenerated converter = new SimpleConverterGenerated(tokens);
    SimpleAstNode astNode = converter.convert(compilationUnit);
    astNode.tokens = tokens;
    return astNode;
  }

  private static <T extends Exception> void throwFirstIfNotEmpty(List<T> exceptionList) throws T {
    if (!exceptionList.isEmpty()) {
      throw exceptionList.get(0);
    }
  }

}
