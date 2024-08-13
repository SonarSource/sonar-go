package org.sonarsource.apex.parser.poc.format;

import apex.jorje.parser.impl.ApexLexer;
import java.util.Collections;
import java.util.List;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FormatTokensTest {

  @Test
  public void table() {
    ANTLRStringStream charStream = new ANTLRStringStream("\n\n  A,B");
    CommonToken token = new CommonToken(charStream, ApexLexer.COMMA, ApexLexer.DEFAULT_TOKEN_CHANNEL, 5, 5);
    token.setLine(3);
    token.setCharPositionInLine(3);
    List<CommonToken> tokens = Collections.singletonList(token);
    assertThat(FormatTokens.table(tokens)).isEqualTo("" +
      "text | type  | line | col | start | stop\n" +
      "-----|-------|------|-----|-------|-----\n" +
      ",    | COMMA | 3    | 4   | 5     | 5   ");
  }
}
