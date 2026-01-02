/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.coverage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CoverageStat {
  static final Pattern COVERAGE_LINE_REGEXP = Pattern.compile("^(.+):(\\d+)\\.(\\d+),(\\d+)\\.(\\d+) (\\d+) (\\d+)$");

  final String filePath;
  final int startLine;
  final int startCol;
  final int endLine;
  final int endCol;
  final int count;

  CoverageStat(String filePath, int startLine, int startCol, int endLine, int endCol, int count) {
    this.filePath = filePath;
    this.startLine = startLine;
    this.startCol = startCol;
    this.endLine = endLine;
    this.endCol = endCol;
    this.count = count;
  }

  static CoverageStat parseLine(int lineNumber, String line) {
    Matcher matcher = COVERAGE_LINE_REGEXP.matcher(line);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid go coverage at line " + lineNumber);
    }
    String filePath = matcher.group(1);
    int startLine = Integer.parseInt(matcher.group(2));
    int startCol = Integer.parseInt(matcher.group(3));
    int endLine = Integer.parseInt(matcher.group(4));
    int endCol = Integer.parseInt(matcher.group(5));
    // No need to parse numStmt as it is never used.
    int count = parseIntWithOverflow(matcher.group(7));

    return new CoverageStat(filePath, startLine, startCol, endLine, endCol, count);
  }

  private static int parseIntWithOverflow(String s) {
    int result;
    try {
      result = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      // Thanks to the regex, we know that the input can only contain digits, the only possible Exception is therefore coming from overflow.
      return Integer.MAX_VALUE;
    }
    return result;
  }
}
