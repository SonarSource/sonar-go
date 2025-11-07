/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class GoLanguageTest {

  @Test
  void should_have_correct_file_extensions() {
    MapSettings mapSettings = new MapSettings();
    GoLanguage typeScriptLanguage = new GoLanguage(mapSettings.asConfig());
    assertThat(typeScriptLanguage.getFileSuffixes()).containsExactly(".go");
  }

  @Test
  void can_override_file_extensions() {
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty("sonar.go.file.suffixes", ".go1,.go2");
    GoLanguage typeScriptLanguage = new GoLanguage(mapSettings.asConfig());
    assertThat(typeScriptLanguage.getFileSuffixes()).containsExactly(".go1", ".go2");
  }
}
