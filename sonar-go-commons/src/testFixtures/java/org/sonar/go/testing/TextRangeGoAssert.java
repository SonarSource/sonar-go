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
package org.sonar.go.testing;

import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;
import org.sonar.plugins.go.api.TextRange;

public class TextRangeGoAssert extends AbstractAssert<TextRangeGoAssert, TextRange> {

  private TextRangeGoAssert(@Nullable TextRange actual) {
    super(actual, TextRangeGoAssert.class);
  }

  public static TextRangeGoAssert assertThat(@Nullable TextRange actual) {
    return new TextRangeGoAssert(actual);
  }

  public TextRangeGoAssert hasRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    isNotNull();
    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(actual.start().line()).as("startLine mismatch").isEqualTo(startLine);
      softly.assertThat(actual.start().lineOffset()).as("startLineOffset mismatch").isEqualTo(startLineOffset);
      softly.assertThat(actual.end().line()).as("endLine mismatch").isEqualTo(endLine);
      softly.assertThat(actual.end().lineOffset()).as("endLineOffset mismatch").isEqualTo(endLineOffset);
    });
    return this;
  }
}
