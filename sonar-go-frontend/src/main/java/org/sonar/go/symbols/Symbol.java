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
package org.sonar.go.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.api.HasTextRange;
import org.sonar.go.api.Tree;
import org.sonar.go.impl.IdentifierTreeImpl;

public class Symbol {
  private final String type;
  private final List<Usage> usages;

  public Symbol(@Nullable String type) {
    this.type = Objects.requireNonNullElse(type, IdentifierTreeImpl.UNKNOWN_TYPE);
    this.usages = new ArrayList<>();
  }

  public String getType() {
    return type;
  }

  public List<Usage> getUsages() {
    return usages;
  }

  /**
   * Return the list of usages that are before the given line.
   */
  public List<Usage> getUsagesBeforeLine(int line) {
    return usages.stream()
      .takeWhile(usage -> usage.identifier().textRange().start().line() < line)
      .toList();
  }

  public List<Usage> getUsagesBefore(HasTextRange hasTextRange) {
    return getUsagesBeforeLine(hasTextRange.textRange().start().line());
  }

  /**
   * Returns the value of the symbol if it is safe to do so.
   * A value is considered safe if either
   * <ol>
   * <li>it is assigned in the declaration and not other value is assigned afterward.</li>
   * <li>there is no assignment in the declaration and a single assignment is done afterward.</li>
   * </ol>
   */
  @CheckForNull
  public Tree getSafeValue() {
    Usage effectivelyFinalUsage = null;
    for (Usage usage : usages) {
      if (usage.type() == Usage.UsageType.DECLARATION) {
        if (effectivelyFinalUsage != null) {
          // An identifier with multiple declarations should never happen, but if it ever does, we don't consider it as effectively final.
          return null;
        } else if (usage.value() != null) {
          // Declaration with an assignment
          effectivelyFinalUsage = usage;
        }
      } else if (usage.type() == Usage.UsageType.ASSIGNMENT && effectivelyFinalUsage == null) {
        // Variable has declared without assignment, so we can consider the first assignment as effectively final
        effectivelyFinalUsage = usage;
      } else if (usage.type() == Usage.UsageType.ASSIGNMENT) {
        // Variable is reassigned, so it is not effectively final
        return null;
      }
    }
    return effectivelyFinalUsage != null ? effectivelyFinalUsage.value() : null;
  }
}
