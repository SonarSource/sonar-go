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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.StringLiteralTree;

import static org.sonar.go.utils.TreeCreationUtils.identifier;

class ImportSpecificationTreeImplTest {

  @Test
  void test() {
    IdentifierTree identifier = identifier("x");
    StringLiteralTree path = new StringLiteralTreeImpl(null, "\"path\"");
    ImportSpecificationTree tree = new ImportSpecificationTreeImpl(null, identifier, path);
    Assertions.assertThat(tree.children()).containsExactly(identifier, path);
    Assertions.assertThat(tree.name()).isEqualTo(identifier);
    Assertions.assertThat(tree.path()).isEqualTo(path);
  }

  @Test
  void test_null_name() {
    StringLiteralTree path = new StringLiteralTreeImpl(null, "\"path\"");
    ImportSpecificationTree tree = new ImportSpecificationTreeImpl(null, null, path);
    Assertions.assertThat(tree.children()).containsExactly(path);
    Assertions.assertThat(tree.name()).isNull();
    Assertions.assertThat(tree.path()).isEqualTo(path);
  }
}
