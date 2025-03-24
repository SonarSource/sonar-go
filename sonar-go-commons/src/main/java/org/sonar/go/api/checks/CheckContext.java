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
package org.sonar.go.api.checks;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.api.HasTextRange;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.Tree;

public interface CheckContext {

  Deque<Tree> ancestors();

  @CheckForNull
  default Tree parent() {
    if (this.ancestors().isEmpty()) {
      return null;
    } else {
      return this.ancestors().peek();
    }
  }

  default <T extends Tree> Optional<T> firstAncestorOfKind(Class<T> type) {
    return ancestors().stream()
      .filter(type::isInstance)
      .map(type::cast)
      .findFirst();
  }

  String filename();

  String fileContent();

  GoVersion goVersion();

  void reportIssue(TextRange textRange, String message);

  void reportIssue(HasTextRange toHighlight, String message);

  void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap);

  void reportFileIssue(String message);

  void reportFileIssue(String message, @Nullable Double gap);
}
