package org.sonarsource.apex.parser.poc.lexer;

import apex.jorje.data.Locations;
import apex.jorje.parser.impl.ApexLexer;
import apex.jorje.parser.impl.BaseApexLexer;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

public final class Lexer {

  private Lexer() {
    // utility class
  }

  public static Tokens parse(String code) {
    ApexLexer lexer = new InternalApexLexer(new ANTLRStringStream(code));
    return collectTokens(lexer);
  }

  private static Tokens collectTokens(ApexLexer lexer) {
    Tokens tokenList = new Tokens();
    CommonToken token = (CommonToken) lexer.nextToken();
    while (token.getType() != Token.EOF) {
      if (token.getType() == ApexLexer.STRING_LITERAL) {
        // restore simple quotes surrounding the string
        token.setText(token.getInputStream().substring(token.getStartIndex(), token.getStopIndex()));
      }
      boolean isWhiteSpace = token.getType() == ApexLexer.WS;
      if (!isWhiteSpace) {
        tokenList.add(token);
      }
      token = (CommonToken) lexer.nextToken();
    }
    return tokenList;
  }

  private static class InternalApexLexer extends ApexLexer {

    static {
      // Disable ApexLexer log: INFO: Deduped array ApexLexer.DFA22_transition
      java.util.logging.Logger log = java.util.logging.Logger.getLogger(BaseApexLexer.class.getName());
      log.setLevel(java.util.logging.Level.WARNING);
      // Fulfill apex.jorje.data.Location "getStartIndex()" and "getEndIndex()" during parsing
      Locations.useIndexFactory();
    }

    private InternalApexLexer(CharStream input) {
      super(input);
    }

    // Override to prevent "System.err.println"
    @Override
    public void emitErrorMessage(String msg) {
      throw new IllegalStateException("Parse error:" + msg);
    }

  }

}
