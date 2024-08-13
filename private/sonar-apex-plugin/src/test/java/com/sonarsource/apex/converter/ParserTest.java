/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.Identifier;
import apex.jorje.data.Locations;
import apex.jorje.data.ast.CompilationUnit;
import apex.jorje.parser.impl.ApexLexer;
import apex.jorje.parser.impl.CaseInsensitiveReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.UnwantedTokenException;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserTest {

  @Test
  void parser_error() {
    assertThatThrownBy(() -> Parser.parse("class A {"))
      .isInstanceOf(ParseException.class)
      .hasMessage("ParseException: Syntax(error = UnexpectedToken(loc = (1, 1, 0, 5), token = 'class'))");
  }

  @Test
  void parser_success() {
    CompilationUnit compilationUnit = Parser.parse("class A {}");
    CompilationUnit.ClassDeclUnit classDeclUnit = (CompilationUnit.ClassDeclUnit) compilationUnit;
    Identifier name = classDeclUnit.body.name;
    assertThat(name.getValue()).isEqualTo("A");
  }

  @Test
  void convert_to_parse_exception() {
    CharStream stream = CaseInsensitiveReaderStream.create("if a");
    stream.consume();
    stream.consume();
    ParseException exception = Parser.convertToParseException(new UnwantedTokenException(ApexLexer.LPAREN, stream));
    assertThat(exception.getPosition().line()).isEqualTo(1);
    assertThat(exception.getPosition().lineOffset()).isEqualTo(2);

    exception = Parser.convertToParseException(new IllegalStateException());
    assertThat(exception.getPosition()).isNull();
  }

  @Test
  void to_text_pointer() {
    TextPointer pointer = Parser.toTextPointer(Locations.loc(0, 5, 42, 3));
    assertThat(pointer.line()).isEqualTo(42);
    assertThat(pointer.lineOffset()).isEqualTo(3 - 1);

    assertThat(Parser.toTextPointer(null)).isNull();
    assertThat(Parser.toTextPointer(Locations.loc(0, 5, 42, 0))).isNull();
    assertThat(Parser.toTextPointer(Locations.loc(0, 0, 0, 0))).isNull();
  }

  @Test
  void check_reserved_words() {
    for (String word : Lexer.RESERVED_WORDS) {
      boolean isIdentifier = isValidCode("Integer " + word + " = 2;") ||
        isValidCode("void foo(" + word + " arg) {}") ||
        isValidCode("void foo(" + word + "<String> arg) {}") ||
        isValidCode("void foo(" + word + "<String, String> arg) {}");
      if (isIdentifier) {
        assertThat(Lexer.IDENTIFIERS.contains(word) || Lexer.IDENTIFIERS_OR_KEYWORDS.contains(word))
          .describedAs("Missing identifier: " + word).isTrue();
        assertThat(Lexer.KEYWORDS).doesNotContain(word);
      } else {
        assertThat(Lexer.IDENTIFIERS.contains(word) || Lexer.IDENTIFIERS_OR_KEYWORDS.contains(word))
          .describedAs("Unexpected keyword: " + word).isFalse();
        assertThat(Lexer.KEYWORDS).contains(word);
      }
    }
    assertThat(Lexer.IDENTIFIERS_OR_KEYWORDS).containsExactly("after", "before", "switch", "when");
  }

  private static boolean isValidCode(String source) {
    try {
      Parser.parse(source);
      return true;
    } catch (ParseException ex) {
      return false;
    }
  }

}
