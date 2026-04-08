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

/**
 * Represents a map type in Go.
 * <a href="https://pkg.go.dev/go/ast#MapType">Go Ast Map Documentation reference</a>
 */
public interface MapTypeTree extends Tree {

  /**
   * @return the key type of the map
   */
  Tree key();

  /**
   * @return the value type of the map
   */
  Tree value();

  /**
   * @return the {@link org.sonar.plugins.go.api.Type} this Tree is referring to
   */
  Type type();
}
