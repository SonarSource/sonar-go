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
package org.sonar.plugins.go.api;

public interface AssignmentExpressionTree extends Tree {

  public enum Operator {
    EQUAL, // =
    PLUS_EQUAL, // +=
    SUB_ASSIGN, // -=
    TIMES_ASSIGN, // *=
    DIVIDED_BY_ASSIGN, // /=
    MODULO_ASSIGN, // %=
    BITWISE_AND_ASSIGN, // &=
    BITWISE_OR_ASSIGN, // |=
    BITWISE_XOR_ASSIGN, // ^=
    BITWISE_SHL_ASSIGN, // <<=
    BITWISE_SHR_ASSIGN, // >>=
    BITWISE_AND_NOT_ASSIGN // &^=
  }

  Operator operator();

  Tree leftHandSide();

  Tree statementOrExpression();

}
