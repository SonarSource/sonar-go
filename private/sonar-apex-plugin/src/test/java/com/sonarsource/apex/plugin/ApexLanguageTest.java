/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class ApexLanguageTest {

  @Test
  void test_suffixes_default() {
    ApexLanguage apexLanguage = new ApexLanguage(new MapSettings().asConfig());
    assertThat(apexLanguage.getFileSuffixes()).containsExactly(".cls", ".trigger");
  }

  @Test
  void test_suffixes_empty() {
    ApexLanguage apexLanguage = new ApexLanguage(new MapSettings().setProperty(ApexPlugin.APEX_FILE_SUFFIXES_KEY, "").asConfig());
    assertThat(apexLanguage.getFileSuffixes()).containsExactly(".cls", ".trigger");
  }

  @Test
  void test_suffixes_custom() {
    ApexLanguage apexLanguage = new ApexLanguage(new MapSettings().setProperty(ApexPlugin.APEX_FILE_SUFFIXES_KEY, ".custom").asConfig());
    assertThat(apexLanguage.getFileSuffixes()).containsExactly(".custom");
  }
}
