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
package org.sonar.go.impl.cfg;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.utils.TreeCreationUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BlockImplTest {
  @Test
  void blockIsCreatedWithEmptyListOfSuccessors() {
    BlockImpl block = new BlockImpl(List.of());
    assertThat(block.successors()).isEmpty();
    assertThat(block.nodes()).isEmpty();
  }

  @Test
  void blockKeepNodes() {
    IdentifierTree identifier = TreeCreationUtils.identifier("name");
    BlockImpl block = new BlockImpl(List.of(identifier));
    assertThat(block.nodes()).containsExactly(identifier);
  }

  @Test
  void blockImplCanSetSuccessors() {
    BlockImpl block = new BlockImpl(List.of());
    BlockImpl nextBlock = new BlockImpl(List.of());
    block.setSuccessors(List.of(nextBlock));
    assertThat(block.successors()).containsExactly(nextBlock);
  }
}
