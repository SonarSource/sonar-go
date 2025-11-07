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

/**
 * A function or method parameter.
 */
public interface ParameterTree extends Tree {

  /**
   * The parameter name represented by {@link IdentifierTree}.
   * @return identifier
   */
  IdentifierTree identifier();

  /**
   * The parameter type represented by the {@link Type}
   * @return type as {@link Type}
   */
  Type type();

  /**
   * The parameter type represented by the original {@link Tree}.
   * @return type tree
   */
  Tree typeTree();
}
