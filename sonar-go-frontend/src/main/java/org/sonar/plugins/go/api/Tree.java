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
import java.util.stream.Stream;

public interface Tree extends HasTextRange {

  List<Tree> children();

  TreeMetaData metaData();

  @Override
  default TextRange textRange() {
    return metaData().textRange();
  }

  default Stream<Tree> descendants() {
    return children().stream()
      .flatMap(tree -> Stream.concat(Stream.of(tree), tree.descendants()));
  }
}
