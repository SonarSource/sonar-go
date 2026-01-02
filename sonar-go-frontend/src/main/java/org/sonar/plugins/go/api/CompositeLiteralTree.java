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

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;

/**
 * Composite literal in Go.
 * They can be defined with key value pairs or just with values (no mix).
 * <pre>
 * {@code
 * // key value pairs
 * m := map[string]int{"one": 1, "two": 2}
 * p := Point{x: 1, y: 2}
 * // just values
 * s := []int{1, 2, 3}
 * p := Point{1, 2}
 * }
 * </pre>
 */
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

  /**
   * Returns a Stream of KeyValueTree elements. A composite literal in Go either has all elements as KeyValueTree (field initialization),
   * or all elements as non-KeyValueTree (value initialization, when field names are omitted). Mixture of value and field initialization
   * is not allowed.
   */
  Stream<KeyValueTree> getKeyValuesElements();

  boolean hasType(String packageName, String typeName);
}
