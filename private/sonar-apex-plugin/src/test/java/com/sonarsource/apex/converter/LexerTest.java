/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import org.antlr.runtime.ANTLRStringStream;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.Token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

  @Test
  void parse() {
    Lexer.Tokens tokens = Lexer.parse("" +
      "class A { // c1\n" +
      "  /*\n" +
      "    c2\n" +
      "   */\n" +
      "  String name = 'abc';\n" +
      "}\n");
    assertThat(tokens.tokensWithoutComments).hasSize(9);

    Token t1 = tokens.tokensWithoutComments.get(0);
    assertThat(t1.text()).isEqualTo("class");
    assertThat(t1.type()).isEqualTo(Token.Type.KEYWORD);
    assertThat(t1.textRange()).hasToString("TextRange[1, 0, 1, 5]");

    Token t2 = tokens.tokensWithoutComments.get(1);
    assertThat(t2.text()).isEqualTo("A");
    assertThat(t2.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t2.textRange()).hasToString("TextRange[1, 6, 1, 7]");

    Token t3 = tokens.tokensWithoutComments.get(2);
    assertThat(t3.text()).isEqualTo("{");
    assertThat(t3.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t3.textRange()).hasToString("TextRange[1, 8, 1, 9]");

    Token t4 = tokens.tokensWithoutComments.get(3);
    assertThat(t4.text()).isEqualTo("String");
    assertThat(t4.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t4.textRange()).hasToString("TextRange[5, 2, 5, 8]");

    Token t5 = tokens.tokensWithoutComments.get(4);
    assertThat(t5.text()).isEqualTo("name");
    assertThat(t5.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t5.textRange()).hasToString("TextRange[5, 9, 5, 13]");

    Token t6 = tokens.tokensWithoutComments.get(5);
    assertThat(t6.text()).isEqualTo("=");
    assertThat(t6.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t6.textRange()).hasToString("TextRange[5, 14, 5, 15]");

    Token t7 = tokens.tokensWithoutComments.get(6);
    assertThat(t7.text()).isEqualTo("'abc'");
    assertThat(t7.type()).isEqualTo(Token.Type.STRING_LITERAL);
    assertThat(t7.textRange()).hasToString("TextRange[5, 16, 5, 21]");

    Token t8 = tokens.tokensWithoutComments.get(7);
    assertThat(t8.text()).isEqualTo(";");
    assertThat(t8.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t8.textRange()).hasToString("TextRange[5, 21, 5, 22]");

    Token t9 = tokens.tokensWithoutComments.get(8);
    assertThat(t9.text()).isEqualTo("}");
    assertThat(t9.type()).isEqualTo(Token.Type.OTHER);
    assertThat(t9.textRange()).hasToString("TextRange[6, 0, 6, 1]");

    assertThat(tokens.comments).hasSize(2);
    Comment c1 = tokens.comments.get(0);
    assertThat(c1.text()).isEqualTo("// c1");
    assertThat(c1.textRange()).hasToString("TextRange[1, 10, 1, 15]");
    assertThat(c1.contentText()).isEqualTo(" c1");
    assertThat(c1.contentRange()).hasToString("TextRange[1, 12, 1, 15]");

    Comment c2 = tokens.comments.get(1);
    assertThat(c2.text()).isEqualTo("/*\n    c2\n   */");
    assertThat(c2.textRange()).hasToString("TextRange[2, 2, 4, 5]");
    assertThat(c2.contentText()).isEqualTo("\n    c2\n   ");
    assertThat(c2.contentRange()).hasToString("TextRange[2, 4, 4, 3]");
  }

  @Test
  void parse_error_throws() {
    Lexer.InternalApexLexer apexLexer = new Lexer.InternalApexLexer(new ANTLRStringStream("foo"));
    assertThrows(IllegalStateException.class,
      () -> apexLexer.emitErrorMessage("Something wrong happened"));
  }
}
