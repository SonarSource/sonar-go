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
package org.sonar.go.impl;

import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.go.api.PackageDeclarationTree;
import org.sonar.plugins.go.api.Tree;

import static org.sonar.go.utils.TreeCreationUtils.identifier;

class PackageDeclarationTreeImplTest {

  @Test
  void test() {
    Tree identifier = identifier("x");
    PackageDeclarationTree tree = new PackageDeclarationTreeImpl(null, Collections.singletonList(identifier));
    Assertions.assertThat(tree.children()).containsExactly(identifier);
  }

}
