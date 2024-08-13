package org.sonarsource.apex.parser.poc.lexer;

import apex.jorje.parser.impl.ApexLexer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

public class Tokens extends ArrayList<CommonToken> {

  private static final Map<Integer, String> TOKEN_TYPE_MAP = Stream.of(Token.class, ApexLexer.class)
    .flatMap(cls -> Arrays.stream(cls.getFields()))
    .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
    .collect(Collectors.toMap(Tokens::intFieldValue, java.lang.reflect.Field::getName, (x1, x2) -> x2));

  private static Integer intFieldValue(java.lang.reflect.Field field) {
    try {
      return (Integer) field.get(null);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private int startLookup = 0;
  private int endLookup = 0;

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    for (CommonToken token : this) {
      out.append(token.getText());
      out.append(' ');
    }
    return out.toString();
  }

  public static String type(CommonToken token) {
    return TOKEN_TYPE_MAP.getOrDefault(token.getType(), "???");
  }

  /**
   * @param startIndex zero based character index in the source code
   * @param endIndex zero based character index excluding the character at endIndex
   * @return
   */
  public Tokens lookup(int startIndex, int endIndex) {
    // token.getStartIndex() zero based character index in the source code
    // token.getStopIndex() zero based character index including the character at getStopIndex()
    Tokens result = new Tokens();
    if (isEmpty()) {
      return result;
    }
    startLookup = boundToSize(startLookup);
    endLookup = boundToSize(endLookup);

    while (startLookup > 0 && get(startLookup).getStartIndex() > startIndex) {
      startLookup--;
    }
    while (startLookup < size() && get(startLookup).getStartIndex() < startIndex) {
      startLookup++;
    }

    while (endLookup < (size() - 1) && (get(endLookup).getStopIndex() + 1) < endIndex) {
      endLookup++;
    }
    while (endLookup >= 0 && (get(endLookup).getStopIndex() + 1) > endIndex) {
      endLookup--;
    }

    for (int i = startLookup; i <= endLookup; i++) {
      result.add(get(i));
    }
    return result;
  }

  private int boundToSize(int index) {
    if (index < 0) {
      return 0;
    } else if (index >= size()) {
      return index - 1;
    } else {
      return index;
    }
  }
}
