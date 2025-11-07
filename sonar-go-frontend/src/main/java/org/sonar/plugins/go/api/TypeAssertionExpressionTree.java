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
 * Represents a type assertion expression in Go Ast.
 * <a href="https://pkg.go.dev/go/ast#TypeAssertExpr">Go Ast Type Expr Documentation reference</a>
 */
public interface TypeAssertionExpressionTree extends Tree {

  /**
   * @return the expression being asserted.
   */
  Tree expression();

  /**
   * @return asserted type; null means type "switch X.(type)"
   */
  @CheckForNull
  Tree type();
}
