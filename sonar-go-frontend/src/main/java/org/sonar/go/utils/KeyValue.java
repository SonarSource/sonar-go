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
package org.sonar.go.utils;

import java.util.List;
import java.util.Optional;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;

public record KeyValue(Tree key, Tree value) {
  public static Optional<KeyValue> of(NativeTree nativeTree) {
    List<Tree> children = nativeTree.children();
    // Expected children: Key, Colon, Value.
    // Anything else should not happen, but we keep the check for defensive programming.
    if (NativeKinds.isKeyValueExpr(nativeTree) && children.size() == 3) {
      return Optional.of(new KeyValue(children.get(0), children.get(2)));
    }
    return Optional.empty();
  }
}
