/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineSetTest {

  @Test
  void new_line() {
    assertThat(pointers("12\n\n45\n", true )).isEqualTo("1,0|1,1|1,2|2,0|3,0|3,1|3,2|4,0|");
    assertThat(pointers("12\n\n45\n", false)).isEqualTo("1,0|1,1|1,2|1,3|2,1|3,1|3,2|3,3|");
  }

  @Test
  void carriage_return() {
    assertThat(pointers("12\r\r45\r", true )).isEqualTo("1,0|1,1|1,2|2,0|3,0|3,1|3,2|4,0|");
    assertThat(pointers("12\r\r45\r", false)).isEqualTo("1,0|1,1|1,2|1,3|2,1|3,1|3,2|3,3|");
  }

  @Test
  void carriage_return_new_line() {
    assertThat(pointers("12\r\n\r\n45\r\n", true )).isEqualTo("1,0|1,1|1,2|1,3|2,0|2,1|3,0|3,1|3,2|3,3|4,0|");
    assertThat(pointers("12\r\n\r\n45\r\n", false)).isEqualTo("1,0|1,1|1,2|1,3|1,4|2,1|2,2|3,1|3,2|3,3|3,4|");
  }

  @Test
  void range() {
    LineSet converter = new LineSet("12\n\n45\n");
    TextRange range = converter.range(1, 4);
    assertThat(range.start().line()).isEqualTo(1);
    assertThat(range.start().lineOffset()).isEqualTo(1);
    assertThat(range.end().line()).isEqualTo(2);
    assertThat(range.end().lineOffset()).isEqualTo(1);
  }

  @Test
  void invalid_offset() {
    LineSet converter = new LineSet("abc");
    assertThatThrownBy(() -> converter.startTextPointer(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> converter.startTextPointer(4)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> converter.endTextPointer(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> converter.endTextPointer(4)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void end_negative_offset() {
    LineSet converter = new LineSet("abc");
    assertThrows(IndexOutOfBoundsException.class,
      () -> converter.endTextPointer(-1));
  }

  private static String pointers(String code, boolean isStart) {
    StringBuilder out = new StringBuilder();
    LineSet converter = new LineSet(code);
    for (int i = 0; i <= code.length(); i++) {
      TextPointer pointer = isStart ? converter.startTextPointer(i) : converter.endTextPointer(i);
      out.append(pointer.line()).append(',').append(pointer.lineOffset()).append('|');
    }
    return out.toString();
  }

}
