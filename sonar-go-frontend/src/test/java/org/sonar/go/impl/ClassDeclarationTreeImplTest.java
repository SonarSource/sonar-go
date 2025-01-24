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
import org.junit.jupiter.api.Test;
import org.sonar.go.api.ClassDeclarationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class ClassDeclarationTreeImplTest {

  private class ClassNativeKind implements NativeKind {
  }

  @Test
  void test() {
    TreeMetaData meta = null;
    IdentifierTree className = new IdentifierTreeImpl(meta, "MyClass");
    Tree classDecl = new NativeTreeImpl(meta, new ClassNativeKind(), Collections.singletonList(className));
    ClassDeclarationTree tree = new ClassDeclarationTreeImpl(meta, className, classDecl);
    assertThat(tree.children()).hasSize(1);
    assertThat(areEquivalent(tree.children().get(0), classDecl)).isTrue();
    assertThat(areEquivalent(tree.identifier(), className)).isTrue();
  }

}
