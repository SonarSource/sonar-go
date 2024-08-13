/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;

public class LineSet {

  public final String code;
  private final int maxOffset;
  /**
   * The index represents the index of the line and the value represents the global offset of the first character in the line.
   */
  private final int[] lineStarts;

  LineSet(String code) {
    this.code = code;
    char[] chars = code.toCharArray();
    maxOffset = chars.length;
    List<Integer> starts = new ArrayList<>();
    starts.add(Integer.MIN_VALUE);
    starts.add(0);
    char last = 0;
    for (int i = 0; i < chars.length; i++) {
      char ch = chars[i];
      if (last == '\r') {
        if (ch == '\n') {
          starts.add(i + 1);
        } else {
          starts.add(i);
        }
      } else if (ch == '\n') {
        starts.add(i + 1);
      }
      last = ch;
    }
    if (last == '\r') {
      starts.add(chars.length);
    }
    starts.add(Integer.MAX_VALUE);
    this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
  }

  /**
   * @param startOffset start at 0, inclusive
   */
  TextPointer startTextPointer(int startOffset) {
    if (startOffset < 0 || startOffset > maxOffset) {
      throw new IndexOutOfBoundsException("startOffset: " + startOffset);
    }
    int lineIndex = Arrays.binarySearch(lineStarts, startOffset);
    return textPointer(lineIndex, startOffset, 0);
  }

  /**
   * @param endOffset start at 0, exclusive
   */
  TextPointer endTextPointer(int endOffset) {
    if (endOffset < 0 || endOffset > maxOffset) {
      throw new IndexOutOfBoundsException("startOffset: " + endOffset);
    }
    if (endOffset == 0) {
      return new TextPointerImpl(1, 0);
    }
    int lineIndex = Arrays.binarySearch(lineStarts, endOffset - 1);
    return textPointer(lineIndex, endOffset, 1);
  }

  /**
   * @param startOffset start at 0, inclusive
   * @param endOffset start at 0, exclusive
   */
  TextRange range(int startOffset, int endOffset) {
    return new TextRangeImpl(startTextPointer(startOffset), endTextPointer(endOffset));
  }

  /**
   * @param startOffset start at 0, inclusive
   * @param endOffset start at 0, exclusive
   */
  String substring(int startOffset, int endOffset) {
    return code.substring(startOffset, endOffset);
  }

  private TextPointer textPointer(int lineIndex, int offset, int defaultLineOffset) {
    if (lineIndex < 0) {
      int positiveLineIndex = (-lineIndex) - 2;
      return new TextPointerImpl(positiveLineIndex, offset - lineStarts[positiveLineIndex]);
    }
    return new TextPointerImpl(lineIndex, defaultLineOffset);
  }

}
