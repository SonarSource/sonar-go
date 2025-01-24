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
package org.sonar.go.persistence.conversion;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.TextRangeImpl;
import org.sonar.go.persistence.JsonTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeConverterTest extends JsonTestHelper {

  @Test
  void format() {
    TextRange initialRange = new TextRangeImpl(3, 7, 4, 12);
    String actual = RangeConverter.format(initialRange);
    assertThat(actual).isEqualTo("3:7:4:12");

    assertThat(RangeConverter.format(null)).isNull();
  }

  @Test
  void parse() {
    TextRange range = RangeConverter.parse("3:7:4:12");
    assertThat(range.start().line()).isEqualTo(3);
    assertThat(range.start().lineOffset()).isEqualTo(7);
    assertThat(range.end().line()).isEqualTo(4);
    assertThat(range.end().lineOffset()).isEqualTo(12);
  }

  @Test
  void parse_null_string() {
    assertThat(RangeConverter.parse(null)).isNull();
  }

  @Test
  void parse_invalid_string() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> RangeConverter.parse("12345"));
    assertThat(e).hasMessage("Invalid TextRange '12345'");
  }

  @Test
  void token_reference() {
    Token token = otherToken(3, 7, "foo");
    assertThat(RangeConverter.tokenReference(token)).isEqualTo("3:7::10");
    assertThat(RangeConverter.tokenReference(null)).isNull();
  }

  @Test
  void resolve_token() {
    Token token = otherToken(1, 0, "foo");
    Token actual = RangeConverter.resolveToken(metaDataProvider, "1:0:1:3");
    assertThat(actual).isSameAs(token);
    assertThat(RangeConverter.resolveToken(metaDataProvider, null)).isNull();
  }

  @Test
  void resolve_invalid_token() {
    otherToken(1, 0, "foo");
    NoSuchElementException e = assertThrows(NoSuchElementException.class,
      () -> RangeConverter.resolveToken(metaDataProvider, "2:0:2:3"));
    assertThat(e).hasMessage("Token not found: 2:0:2:3");
  }

  @Test
  void metadata_reference() {
    Token token = otherToken(1, 0, "true");
    Tree tree = new LiteralTreeImpl(metaData(token), token.text());
    assertThat(RangeConverter.metaDataReference(tree)).isEqualTo("1:0::4");
  }

  @Test
  void resolve_metadata() {
    Token token = otherToken(3, 5, "true");
    TreeMetaData metaData = RangeConverter.resolveMetaData(metaDataProvider, "3:5:3:9");
    assertThat(metaData).isNotNull();
    assertThat(metaData.textRange()).isEqualTo(token.textRange());
  }

}
