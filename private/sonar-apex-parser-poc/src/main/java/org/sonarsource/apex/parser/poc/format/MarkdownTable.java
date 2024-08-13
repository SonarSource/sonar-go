package org.sonarsource.apex.parser.poc.format;

import java.util.ArrayList;
import java.util.List;

public class MarkdownTable {

  private final int colCount;
  private final String[] columnNames;
  private final int[] colWidths;
  private final List<String[]> rows;

  public MarkdownTable(String... columnNames) {
    colCount = columnNames.length;
    colWidths = new int[colCount];
    this.columnNames = escape(columnNames);
    for (int col = 0; col < colCount; col++) {
      colWidths[col] = this.columnNames[col].length();
    }
    rows = new ArrayList<>();
  }

  public void add(Object... columnValues) {
    if (columnValues.length != colCount) {
      throw new IllegalArgumentException("columnValues.length (" + columnNames.length + ") must be " + colCount);
    }
    String[] row = escape(columnValues);
    for (int col = 0; col < colCount; col++) {
      if (colWidths[col] < row[col].length()) {
        colWidths[col] = row[col].length();
      }
    }
    rows.add(row);
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    for (int col = 0; col < colCount; col++) {
      appendCol(out, columnNames[col], col, ' ');
    }
    out.append('\n');
    for (int col = 0; col < colCount; col++) {
      appendCol(out, "", col, '-');
    }
    for (String[] row : rows) {
      out.append('\n');
      for (int col = 0; col < colCount; col++) {
        appendCol(out, row[col], col, ' ');
      }
    }
    return out.toString();
  }

  private void appendCol(StringBuilder out, String value, int col, char filler) {
    int start = out.length();
    out.append(value);
    fill(out, filler, start + colWidths[col]);
    if (col + 1 < colCount) {
      out.append(filler).append('|').append(filler);
    }
  }

  private static String[] escape(Object... values) {
    String[] newValues = new String[values.length];
    for (int col = 0; col < values.length; col++) {
      String txt = String.valueOf(values[col]);
      if (txt != null) {
        txt = txt.replace("\\", "\\\\");
        txt = txt.replace("\"", "\\\"");
        txt = txt.replace("\n", "\\n");
        txt = txt.replace("\r", "\\r");
        txt = txt.replace("\t", "\\t");
        txt = txt.replace("|", "\\|");
      } else {
        txt = "null";
      }
      newValues[col] = txt;
    }
    return newValues;
  }

  private static void fill(StringBuilder out, char filler, int endExcluded) {
    while (out.length() < endExcluded) {
      out.append(filler);
    }
  }

}
