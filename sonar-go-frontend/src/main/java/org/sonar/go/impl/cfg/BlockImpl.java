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
import org.sonar.go.api.Tree;
import org.sonar.go.api.cfg.Block;

public class BlockImpl implements Block {

  private final List<Tree> nodes;
  private List<Block> successors = Collections.emptyList();

  public BlockImpl(List<Tree> nodes) {
    this.nodes = Collections.unmodifiableList(nodes);
  }

  @Override
  public List<Tree> nodes() {
    return nodes;
  }

  @Override
  public List<Block> successors() {
    return successors;
  }

  public void setSuccessors(List<Block> successors) {
    this.successors = Collections.unmodifiableList(successors);
  }
}
