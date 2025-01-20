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
package org.sonar.go.checks.utils;

import java.util.function.Predicate;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.persistence.conversion.StringNativeKind;

public class TreeUtils {
  private TreeUtils() {
  }

  public static Predicate<Tree> IS_NOT_SEMICOLON = Predicate.not(tree -> tree instanceof NativeTree nativeTree
    && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
    && stringNativeKind.toString().equals("Semicolon"));
}
