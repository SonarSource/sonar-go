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

/**
 * Represents a 'go' statement in Go, which is used to invoke a function call.
 * Example:
 * <pre>
 *   go func() { ch <- 1 }()
 * </pre>
 */
public interface GoStatementTree extends Tree {

  /**
   * @return the token representing the 'go' keyword in the statement.
   */
  Token goToken();

  /**
   * @return the function invocation that is being called in this 'go' statement.
   */
  FunctionInvocationTree functionInvocation();
}
