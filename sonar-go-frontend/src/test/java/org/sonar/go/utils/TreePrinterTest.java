/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.utils;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.AssignmentExpressionTree;
import org.sonar.go.api.BinaryExpressionTree;
import org.sonar.go.api.Tree;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.visitors.TreePrinter;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.go.utils.TreeCreationUtils.assignment;
import static org.sonar.go.utils.TreeCreationUtils.binary;
import static org.sonar.go.utils.TreeCreationUtils.identifier;
import static org.sonar.go.utils.TreeCreationUtils.integerLiteral;

class TreePrinterTest {

  @Test
  void test() {
    Tree x1 = TreeCreationUtils.identifier("x1");
    Tree var1 = TreeCreationUtils.identifier("var1");
    Tree literal1 = new LiteralTreeImpl(null, "42");
    Tree binaryExp = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, var1, literal1);
    Tree assignExp = new AssignmentExpressionTreeImpl(null, AssignmentExpressionTree.Operator.EQUAL, x1, binaryExp);
    Tree function = new FunctionDeclarationTreeImpl(null, null, null, null, emptyList(), null, null);
    Assertions.assertThat(TreePrinter.tree2string(Arrays.asList(assignExp, function))).isEqualTo("""
      AssignmentExpressionTreeImpl EQUAL
        IdentifierTreeImpl x1
        BinaryExpressionTreeImpl PLUS
          IdentifierTreeImpl var1
          LiteralTreeImpl 42

      FunctionDeclarationTreeImpl
      """);
  }

  @Test
  void table_test() {
    // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = x-1;
    Tree add = binary(BinaryExpressionTree.Operator.PLUS,
      identifier("x", new TextRangeImpl(1, 42, 1, 43), "x"),
      integerLiteral("1", new TextRangeImpl(1, 44, 1, 45), "1"),
      new TextRangeImpl(1, 42, 1, 45), "x", "1");

    Tree assign = assignment(identifier("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      new TextRangeImpl(1, 8, 1, 39), "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
      add,
      new TextRangeImpl(1, 8, 1, 45), "x", "=", "x", "1");

    String actual = TreePrinter.table(assign);
    TreePrinter.Table expected = new TreePrinter.Table("AST node class", "first…last tokens", "line:col");
    expected.add("AssignmentExpressionTree {", "x … 1", "1:9 … 1:46");
    expected.add("  IdentifierTree", "xxxxxxxxxxx…xxxxxxxxxxx", "1:9 … 1:40");
    expected.add("  BinaryExpressionTree {", "x … 1", "1:43 … 1:46");
    expected.add("    IdentifierTree", "x", "1:43 … 1:44");
    expected.add("    IntegerLiteralTree", "1", "1:45 … 1:46");
    expected.add("  }", "", "");
    expected.add("}", "", "");
    assertEquals(expected.toString(), actual);
  }
}
