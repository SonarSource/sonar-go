/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApexCodeVerifierTest {

  private ApexCodeVerifier apexCodeVerifier = new ApexCodeVerifier();
  @Test
  void testContainsCode() {
    assertThat(apexCodeVerifier.containsCode("This is a normal sentence: definitely not code")).isFalse();
    assertThat(apexCodeVerifier.containsCode("this is a normal comment")).isFalse();
    assertThat(apexCodeVerifier.containsCode("this is a normal comment;")).isFalse();
    assertThat(apexCodeVerifier.containsCode("just three words")).isFalse();
    assertThat(apexCodeVerifier.containsCode("SNAPSHOT")).isFalse();
    assertThat(apexCodeVerifier.containsCode("")).isFalse();
    assertThat(apexCodeVerifier.containsCode(" ")).isFalse();
    assertThat(apexCodeVerifier.containsCode(" continue ")).isFalse();
    assertThat(apexCodeVerifier.containsCode("1.0")).isFalse();
    assertThat(apexCodeVerifier.containsCode("* Copyright (c) 2012-2014")).isFalse();

    assertThat(apexCodeVerifier.containsCode("return something very useful")).isFalse();
    assertThat(apexCodeVerifier.containsCode("return something very useful;")).isFalse();
    assertThat(apexCodeVerifier.containsCode("return something")).isFalse();
    assertThat(apexCodeVerifier.containsCode("return something;")).isTrue();

    assertThat(apexCodeVerifier.containsCode("done (almost done)")).isFalse();
    assertThat(apexCodeVerifier.containsCode("done (almost done);")).isFalse();
    assertThat(apexCodeVerifier.containsCode("done (almost)")).isFalse();
    assertThat(apexCodeVerifier.containsCode("done (almost);")).isTrue();

    assertThat(apexCodeVerifier.containsCode("try something different")).isFalse();
    assertThat(apexCodeVerifier.containsCode("try to catch(M e) {}")).isFalse();
    assertThat(apexCodeVerifier.containsCode("try {t = o;} catch(M e) {}")).isTrue();

    assertThat(apexCodeVerifier.containsCode("a = 5 + b")).isFalse();
    assertThat(apexCodeVerifier.containsCode("a = 5 + b;")).isTrue();
  }
}
