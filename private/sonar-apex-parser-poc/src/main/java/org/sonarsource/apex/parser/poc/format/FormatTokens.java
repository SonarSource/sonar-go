package org.sonarsource.apex.parser.poc.format;

import java.util.Collection;
import org.antlr.runtime.CommonToken;
import org.sonarsource.apex.parser.poc.lexer.Tokens;

public class FormatTokens {

  public static String table(Collection<CommonToken> tokens) {
    MarkdownTable table = new MarkdownTable("text", "type", "line", "col", "start", "stop");
    for (CommonToken token : tokens) {
      table.add(
        token.getText(),
        Tokens.type(token),
        token.getLine(),
        token.getCharPositionInLine() + 1,
        token.getStartIndex(),
        token.getStopIndex());
    }
    return table.toString();
  }

}
