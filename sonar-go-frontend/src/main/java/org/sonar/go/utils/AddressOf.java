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

/**
 * Match "address-of" unary operator: `&sonar`
 */
public record AddressOf(Tree operand) {

  public static Optional<AddressOf> of(NativeTree nativeTree) {
    List<Tree> children = nativeTree.children();
    if (nativeTree.nativeKind().toString().contains("UnaryExpr")
      && children.size() == 2
      && children.get(0) instanceof NativeTree operator
      && operator.nativeKind().toString().contains("Op")) {
      return Optional.of(new AddressOf(children.get(1)));
    }
    return Optional.empty();
  }
}
