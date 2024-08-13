package org.sonarsource.apex.parser.poc.format;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownTableTest {

  @Test
  public void two_columns() {
    MarkdownTable table = new MarkdownTable("A", "B");
    table.add("1", "2");
    table.add("3", "4");
    assertThat(table.toString()).isEqualTo("" +
      "A | B\n" +
      "--|--\n" +
      "1 | 2\n" +
      "3 | 4");
  }

  @Test
  public void three_columns() {
    MarkdownTable table = new MarkdownTable("Long column", "Short Column", "Other");
    table.add("This is a long value", "", "1");
    table.add(null, "This is also long", "2");
    assertThat(table.toString()).isEqualTo("" +
      "Long column          | Short Column      | Other\n" +
      "---------------------|-------------------|------\n" +
      "This is a long value |                   | 1    \n" +
      "null                 | This is also long | 2    ");
  }

  @Test
  public void escape_text() {
    MarkdownTable table = new MarkdownTable("A\\A", "B\nB");
    table.add("1 \t 2", "3 | 4");
    assertThat(table.toString()).isEqualTo("" +
      "A\\\\A   | B\\nB  \n" +
      "-------|-------\n" +
      "1 \\t 2 | 3 \\| 4");
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalid_column_count() {
    MarkdownTable table = new MarkdownTable("A", "B");
    table.add("a", "b", "c");
  }

  @Test
  public void support_null() {
    MarkdownTable table = new MarkdownTable("A", "B");
    table.add(null, new Object(){
      @Override
      public String toString() {
        return null;
      }
    });
    assertThat(table.toString()).isEqualTo("" +
      "A    | B   \n" +
      "-----|-----\n" +
      "null | null");
  }

}
