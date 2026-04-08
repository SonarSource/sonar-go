/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.TreeCreationUtils.identifier;

class PackageDeclarationTreeImplTest {

  @Test
  void test() {
    var identifier = identifier("x");
    var tree = new PackageDeclarationTreeImpl(null, Collections.singletonList(identifier));
    assertThat(tree.children()).containsExactly(identifier);
    assertThat(tree.packageName()).isEqualTo("x");
  }
}
