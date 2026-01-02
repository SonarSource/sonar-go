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

import javax.annotation.CheckForNull;

/**
 * An Ellipsis node stands for the "..." type in a parameter list or the "..." length in an array type.
 */
public interface EllipsisTree extends Tree {

  /**
   * Ellipsis element type (parameter lists only); or null
   * @return the element type of the ellipsis, or null if not applicable
   */
  @CheckForNull
  Tree element();

  /**
   * "..." token.
   * @return the token representing the ellipsis
   */
  Token ellipsis();
}
