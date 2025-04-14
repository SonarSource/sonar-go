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
package org.sonar.plugins.go.api;

import java.util.List;

public interface FunctionInvocationTree extends Tree {

  /**
   * The callee of the function invocation. Can be a member select, or a simple identifier, or an anonymous function.
   */
  Tree memberSelect();

  List<Tree> arguments();

  /**
   * Return function invocation signature. This signature may be treated as unique id.
   * @param packageName the packageName of the file where function is invoked
   * @return function invocation signature
   */
  String signature(String packageName);
}
