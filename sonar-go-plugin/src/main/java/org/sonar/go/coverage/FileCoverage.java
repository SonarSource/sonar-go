/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

class FileCoverage {
  Map<Integer, LineCoverage> lineMap = new HashMap<>();
  List<String> lines;

  public FileCoverage(List<CoverageStat> coverageStats, @Nullable List<String> lines) {
    this.lines = lines;
    coverageStats.forEach(this::add);
  }

  private void add(CoverageStat coverage) {
    int startLine = findStartIgnoringBrace(coverage);
    int endLine = findEndIgnoringBrace(coverage, startLine);
    for (int line = startLine; line <= endLine; line++) {
      if (!isEmpty(line - 1)) {
        lineMap.computeIfAbsent(line, key -> new LineCoverage())
          .add(coverage);
      }
    }
  }

  private boolean isEmpty(int line) {
    return lines != null &&
      lines.get(line).trim().isEmpty();
  }

  int findStartIgnoringBrace(CoverageStat coverage) {
    int line = coverage.startLine;
    int column = coverage.startCol;
    while (shouldIgnore(line, column)) {
      column++;
      if (column > lines.get(line - 1).length()) {
        line++;
        column = 1;
      }
    }
    return line;
  }

  int findEndIgnoringBrace(CoverageStat coverage, int startLine) {
    int line = coverage.endLine;
    int column = coverage.endCol - 1;
    if (lines != null && line > lines.size()) {
      line = lines.size();
      column = lines.get(line - 1).length();
    }
    while (line > startLine && shouldIgnore(line, column)) {
      column--;
      if (column == 0) {
        line--;
        column = lines.get(line - 1).length();
      }
    }
    return line;
  }

  boolean shouldIgnore(int line, int column) {
    if (lines != null && line > 0 && line <= lines.size() && column > 0) {
      String currentLine = lines.get(line - 1);
      if (column > currentLine.length()) {
        // Ignore end of line
        return true;
      }
      int ch = currentLine.charAt(column - 1);
      return ch < ' ' || ch == '{' || ch == '}';
    }
    return false;
  }
}
