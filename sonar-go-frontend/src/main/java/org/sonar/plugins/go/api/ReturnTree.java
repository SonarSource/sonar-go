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

import java.util.List;

public interface ReturnTree extends Tree, HasKeyword {
  /**
   * Provide the list of expressions returned by the statement.
   * In case of empty or named return, the list is empty (not null). E.g. {@code return} will return {@code []}.
   * In case of a single expression, the list contains one element. E.g. {@code return 42} will return {@code [42]}.
   * In case of multiple expression, the list contains the elements without the comma. E.g. {@code return 42, "bob"} will return {@code [42, "bob"]}.
   * @return The list of expressions without separators.
   */
  List<Tree> expressions();

  Token keyword();
}
