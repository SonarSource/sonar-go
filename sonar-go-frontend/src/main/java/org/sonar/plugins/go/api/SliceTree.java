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
package org.sonar.plugins.go.api;

import javax.annotation.CheckForNull;

/**
 * Represents a slice in Go.
 * <a href="https://pkg.go.dev/go/ast#SliceExpr">Go Ast Slice Documentation reference</a>
 */
public interface SliceTree extends Tree {
  /**
   * @return the expression of the slice
   */
  Tree expression();

  /**
   * @return the low bound of the slice, or null if not specified
   */
  @CheckForNull
  Tree low();

  /**
   * @return the high bound of the slice, or null if not specified
   */
  @CheckForNull
  Tree high();

  /**
   * @return the max bound of the slice, or null if not specified
   */
  @CheckForNull
  Tree max();

  /**
   * @return true if the slice is a 3-argument slice expression
   */
  boolean slice3();
}
