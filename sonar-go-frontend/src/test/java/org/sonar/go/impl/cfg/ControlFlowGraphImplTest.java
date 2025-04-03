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
import org.sonar.go.api.cfg.Block;
import org.sonar.go.api.cfg.ControlFlowGraph;

import static org.assertj.core.api.Assertions.assertThat;

class ControlFlowGraphImplTest {
  @Test
  void firstNodeIsEntryBlock() {
    BlockImpl firstBlock = new BlockImpl(List.of());
    List<Block> blocks = List.of(firstBlock, new BlockImpl(List.of()));
    ControlFlowGraph cfg = new ControlFlowGraphImpl(blocks);
    assertThat(cfg.entryBlock()).isEqualTo(firstBlock);
    assertThat(cfg.blocks()).containsExactlyElementsOf(blocks);
  }
}
