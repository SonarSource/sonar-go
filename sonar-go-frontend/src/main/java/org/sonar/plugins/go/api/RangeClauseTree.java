/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import javax.annotation.CheckForNull;

/**
 * The clause of a {@code range} loop, the {@link LoopTree#condition()} of such a loop. Both the key and the value are
 * optional: {@code for range x} has neither, {@code for k := range x} has no value.
 */
public interface RangeClauseTree extends Tree {

  @CheckForNull
  Tree key();

  @CheckForNull
  Tree value();

  /**
   * The expression the loop ranges over.
   */
  Tree rangedExpression();

  /**
   * Whether the clause declares its key and its value, as {@code for k, v := range x} does, rather than assigning to
   * already declared variables, as {@code for k, v = range x} does.
   */
  boolean isDeclaration();
}
