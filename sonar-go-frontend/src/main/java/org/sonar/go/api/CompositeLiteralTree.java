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
package org.sonar.go.api;

import java.util.List;
import javax.annotation.CheckForNull;

public interface CompositeLiteralTree extends Tree {

  /**
   * Can return null when the type is set at higher level. (E.g. array of structured objects)
   */
  @CheckForNull
  Tree type();

  /**
   * Elements of this composite literal. A composite literal in Go either has all elements as KeyValueTree (field initialization),
   * or all elements as non-KeyValueTree (value initialization, when field names are omitted). Mixture of value and field initialization
   * is not allowed.
   */
  List<Tree> elements();
}
