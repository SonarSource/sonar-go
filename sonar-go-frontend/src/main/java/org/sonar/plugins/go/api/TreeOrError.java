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

import javax.annotation.Nullable;

/**
 * A container for a {@link Tree} or an error message. It contains either a valid tree or an error message, but not both.
 * @param tree the parsed tree, or null if there was an error
 * @param error the error message, or null if the tree is valid
 */
public record TreeOrError(@Nullable Tree tree, @Nullable String error) {

  /**
   * Creates a new instance of {@link TreeOrError} with a valid tree.
   *
   * @param tree the parsed tree
   * @return a new instance containing the tree
   */
  public static TreeOrError of(Tree tree) {
    return new TreeOrError(tree, null);
  }

  /**
   * Creates a new instance of {@link TreeOrError} with an error message.
   *
   * @param errorMessage the error message
   * @return a new instance containing the error message
   */
  public static TreeOrError of(String errorMessage) {
    return new TreeOrError(null, errorMessage);
  }

  public boolean isError() {
    return error != null;
  }

  public boolean isTree() {
    return tree != null;
  }
}
