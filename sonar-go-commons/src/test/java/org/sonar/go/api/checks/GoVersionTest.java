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
package org.sonar.go.api.checks;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class GoVersionTest {

  @ParameterizedTest
  @CsvSource(
    value = {
      "1.21,1.21",
      "1.21.0,1.21",
      "1.21.1,1.21.1",
      "1.21.10,1.21.10",
      "2.0,2.0",
      "0.1,0.1",
      "fail,unknown",
      ",unknown",
      "1.a,unknown",
      "1.1.a,unknown",
      // GoVersion itself does not support beta/rc versions, we make sure the analyzed go version does not include them
      "1.21.0rc1,unknown",
      "1.21.0rc1,unknown",
    })
  void test(String version, String expected) {
    GoVersion goVersion = GoVersion.parse(version);
    assertThat(goVersion.toString()).isNotNull();
    assertThat(goVersion).hasToString(expected);
  }

  @MethodSource
  @ParameterizedTest
  void shouldCorrectlyIdentifyVersionSuperiority(GoVersion version1, GoVersion version2, boolean expected) {
    assertThat(version1.isGreaterThanEqualOrUnknown(version2)).isEqualTo(expected);
  }

  static Stream<Arguments> shouldCorrectlyIdentifyVersionSuperiority() {
    return Stream.of(
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.21"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.21.0"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.20"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.20.0"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("0.1"), true),
      Arguments.of(GoVersion.parse("1.21.0"), GoVersion.parse("1.21"), true),
      Arguments.of(GoVersion.parse("1.21.1"), GoVersion.parse("1.21.1"), true),

      Arguments.of(GoVersion.UNKNOWN_VERSION, GoVersion.parse("1.21"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.UNKNOWN_VERSION, true),
      Arguments.of(GoVersion.UNKNOWN_VERSION, GoVersion.UNKNOWN_VERSION, true),

      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.21.1"), false),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.22"), false),
      Arguments.of(GoVersion.parse("1.21.0"), GoVersion.parse("1.21.1"), false),
      Arguments.of(GoVersion.parse("1.21.0"), GoVersion.parse("1.22"), false),
      Arguments.of(GoVersion.parse("1.21.0"), GoVersion.parse("1.22"), false));
  }

  @MethodSource("goVersionsSupplierForEqualsAndHashCode")
  @ParameterizedTest
  void equalsShouldReturnCorrectly(GoVersion goVersion, Object objectToCompareAgainst, boolean expected) {
    assertThat(goVersion.equals(objectToCompareAgainst)).isEqualTo(expected);
  }

  @MethodSource("goVersionsSupplierForEqualsAndHashCode")
  @ParameterizedTest
  void hashCodeShouldReturnCorrectly(GoVersion goVersion, Object objectToCompareAgainst, boolean expected) {
    if (objectToCompareAgainst == null) {
      assertThat(goVersion.hashCode()).isNotZero();
      return;
    }
    if (expected) {
      assertThat(goVersion).hasSameHashCodeAs(objectToCompareAgainst);
    } else {
      assertThat(goVersion.hashCode()).isNotEqualTo(objectToCompareAgainst.hashCode());
    }
  }

  static Stream<Arguments> goVersionsSupplierForEqualsAndHashCode() {
    return Stream.of(
      Arguments.of(GoVersion.UNKNOWN_VERSION, GoVersion.UNKNOWN_VERSION, true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.21"), true),
      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.21.0"), true),

      Arguments.of(GoVersion.parse("1.21"), GoVersion.parse("1.20"), false),
      Arguments.of(GoVersion.parse("1.21"), null, false),
      Arguments.of(GoVersion.parse("1.21"), "some string", false));
  }

}
