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
package org.sonar.plugins.go.api.checks;

import org.junit.jupiter.api.Test;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.persistence.JsonTree;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SecondaryLocationTest {

  private static final Tree IDENTIFIER = JsonTree.fromJson("""
    {
      "treeMetaData": {"tokens": [{"textRange": "1:0:1:3", "text": "foo", "type": "OTHER"}]},
      "tree": {"@type": "Identifier", "metaData": "1:0:1:3", "name": "foo", "type": "UNKNOWN", "package": "UNKNOWN", "id": 0}
    }""");

  @Test
  void constructor_with_tree() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER);
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isNull();
  }

  @Test
  void constructor_with_tree_and_message() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER, "because");
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isEqualTo("because");
  }

  @Test
  void constructor_with_text_range_and_message() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER.textRange(), "because");
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isEqualTo("because");
  }

}
