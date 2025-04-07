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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.TreeCreationUtils.integerLiteral;
import static org.sonar.plugins.go.api.IntegerLiteralTree.Base.BINARY;
import static org.sonar.plugins.go.api.IntegerLiteralTree.Base.DECIMAL;
import static org.sonar.plugins.go.api.IntegerLiteralTree.Base.HEXADECIMAL;
import static org.sonar.plugins.go.api.IntegerLiteralTree.Base.OCTAL;

class IntegerLiteralTreeImplTest {

  @Test
  void bases() {
    assertThat(integerLiteral("0123").getBase()).isEqualTo(OCTAL);
    assertThat(integerLiteral("0o123").getBase()).isEqualTo(OCTAL);
    assertThat(integerLiteral("0O123").getBase()).isEqualTo(OCTAL);

    assertThat(integerLiteral("123").getBase()).isEqualTo(DECIMAL);
    assertThat(integerLiteral("0d123").getBase()).isEqualTo(DECIMAL);
    assertThat(integerLiteral("0D123").getBase()).isEqualTo(DECIMAL);

    assertThat(integerLiteral("0x123").getBase()).isEqualTo(HEXADECIMAL);
    assertThat(integerLiteral("0X123").getBase()).isEqualTo(HEXADECIMAL);

    assertThat(integerLiteral("0b101").getBase()).isEqualTo(BINARY);
    assertThat(integerLiteral("0B101").getBase()).isEqualTo(BINARY);
  }

  @Test
  void integerValues() {
    assertThat(integerLiteral("0123").getIntegerValue().intValue()).isEqualTo(83);
    assertThat(integerLiteral("0o123").getIntegerValue().intValue()).isEqualTo(83);
    assertThat(integerLiteral("0O123").getIntegerValue().intValue()).isEqualTo(83);
    assertThat(integerLiteral("0O00123").getIntegerValue().intValue()).isEqualTo(83);

    assertThat(integerLiteral("123").getIntegerValue().intValue()).isEqualTo(123);
    assertThat(integerLiteral("0d123").getIntegerValue().intValue()).isEqualTo(123);
    assertThat(integerLiteral("0D123").getIntegerValue().intValue()).isEqualTo(123);

    assertThat(integerLiteral("0x123").getIntegerValue().intValue()).isEqualTo(291);
    assertThat(integerLiteral("0X123").getIntegerValue().intValue()).isEqualTo(291);

    assertThat(integerLiteral("0b101").getIntegerValue().intValue()).isEqualTo(5);
    assertThat(integerLiteral("0B101").getIntegerValue().intValue()).isEqualTo(5);
  }

  @Test
  void numericPart() {
    assertThat(integerLiteral("0123").getNumericPart()).isEqualTo("123");
    assertThat(integerLiteral("0o123").getNumericPart()).isEqualTo("123");
    assertThat(integerLiteral("0O123").getNumericPart()).isEqualTo("123");

    assertThat(integerLiteral("123").getNumericPart()).isEqualTo("123");
    assertThat(integerLiteral("0d123").getNumericPart()).isEqualTo("123");
    assertThat(integerLiteral("0D123").getNumericPart()).isEqualTo("123");

    assertThat(integerLiteral("0x123").getNumericPart()).isEqualTo("123");
    assertThat(integerLiteral("0X123").getNumericPart()).isEqualTo("123");

    assertThat(integerLiteral("0b101").getNumericPart()).isEqualTo("101");
    assertThat(integerLiteral("0B101").getNumericPart()).isEqualTo("101");
  }

}
