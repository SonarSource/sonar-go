/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.impl;

import org.junit.jupiter.api.Test;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class MemberSelectTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    IdentifierTree identifierTree = TreeCreationUtils.identifier("y");
    Tree member = TreeCreationUtils.identifier("x");
    MemberSelectTree memberSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    assertThat(memberSelect.children()).containsExactly(member, identifierTree);
    assertThat(memberSelect.expression()).isEqualTo(member);
    assertThat(memberSelect.identifier()).isEqualTo(identifierTree);
  }
}
