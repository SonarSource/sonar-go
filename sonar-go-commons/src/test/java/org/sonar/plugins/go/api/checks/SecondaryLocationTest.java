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
package org.sonar.plugins.go.api.checks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SecondaryLocationTest {

  private static final TextRangeImpl RANGE = new TextRangeImpl(1, 0, 1, 3);
  private static final Tree IDENTIFIER = new IdentifierTreeImpl(
    new TreeMetaDataProvider(List.of(), List.of(new TokenImpl(RANGE, "foo", Token.Type.OTHER))).metaData(RANGE),
    "foo", "UNKNOWN", "UNKNOWN", 0);

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
