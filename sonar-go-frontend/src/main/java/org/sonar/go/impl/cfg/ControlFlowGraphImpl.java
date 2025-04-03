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

import java.util.Collections;
import java.util.List;
import org.sonar.go.api.cfg.Block;
import org.sonar.go.api.cfg.ControlFlowGraph;

public class ControlFlowGraphImpl implements ControlFlowGraph {
  private final Block entryBlock;
  private final List<Block> blocks;

  public ControlFlowGraphImpl(List<Block> blocks) {
    this.entryBlock = blocks.get(0);
    this.blocks = Collections.unmodifiableList(blocks);
  }

  @Override
  public Block entryBlock() {
    return entryBlock;
  }

  @Override
  public List<Block> blocks() {
    return blocks;
  }
}
