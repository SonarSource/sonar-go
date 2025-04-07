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
package org.sonar.plugins.go.api.checks;

import javax.annotation.Nullable;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Tree;

public class SecondaryLocation {

  public final TextRange textRange;

  @Nullable
  public final String message;

  public SecondaryLocation(Tree tree) {
    this(tree, null);
  }

  public SecondaryLocation(Tree tree, @Nullable String message) {
    this(tree.metaData().textRange(), message);
  }

  public SecondaryLocation(TextRange textRange, @Nullable String message) {
    this.textRange = textRange;
    this.message = message;
  }

}
