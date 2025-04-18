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
package org.sonar.go.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoExclusionsFileFilterTest {

  @Test
  void should_exclude_vendor_dir() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty(GoPlugin.EXCLUSIONS_KEY, GoPlugin.EXCLUSIONS_DEFAULT_VALUE);
    GoExclusionsFileFilter filter = new GoExclusionsFileFilter(settings.asConfig());
    assertTrue(filter.accept(inputFile("file.go")));
    assertFalse(filter.accept(inputFile("vendor/file.go")));
    assertFalse(filter.accept(inputFile("vendor/someDir/file.go")));
    assertFalse(filter.accept(inputFile("someDir/vendor/file.go")));
  }

  @Test
  void should_exclude_only_go() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty(GoPlugin.EXCLUSIONS_KEY, GoPlugin.EXCLUSIONS_DEFAULT_VALUE);
    GoExclusionsFileFilter filter = new GoExclusionsFileFilter(settings.asConfig());
    assertFalse(filter.accept(inputFile("vendor/file.go")));
    assertTrue(filter.accept(inputFile("vendor/file.json")));
  }

  @Test
  void should_include_vendor_when_property_is_overridden() throws Exception {
    MapSettings settings = new MapSettings();

    settings.setProperty(GoPlugin.EXCLUSIONS_KEY, "");
    GoExclusionsFileFilter filter = new GoExclusionsFileFilter(settings.asConfig());

    assertTrue(filter.accept(inputFile("file.go")));
    assertTrue(filter.accept(inputFile("vendor/file.go")));
    assertTrue(filter.accept(inputFile("vendor/someDir/file.go")));
    assertTrue(filter.accept(inputFile("someDir/vendor/file.go")));
  }

  @Test
  void should_exclude_using_custom_path_regex() throws Exception {
    MapSettings settings = new MapSettings();

    settings.setProperty(GoPlugin.EXCLUSIONS_KEY, "**/lib/**");
    GoExclusionsFileFilter filter = new GoExclusionsFileFilter(settings.asConfig());

    assertTrue(filter.accept(inputFile("file.go")));
    assertTrue(filter.accept(inputFile("vendor/file.go")));
    assertFalse(filter.accept(inputFile("lib/file.go")));
    assertFalse(filter.accept(inputFile("someDir/lib/file.go")));
  }

  @Test
  void should_ignore_empty_path_regex() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty(GoPlugin.EXCLUSIONS_KEY, "," + GoPlugin.EXCLUSIONS_DEFAULT_VALUE + ",");
    GoExclusionsFileFilter filter = new GoExclusionsFileFilter(settings.asConfig());

    assertTrue(filter.accept(inputFile("file.go")));
    assertFalse(filter.accept(inputFile("vendor/file.go")));
  }

  private DefaultInputFile inputFile(String file) {
    return new TestInputFileBuilder("test", "test_vendor/" + file).setLanguage(file.split("\\.")[1]).build();
  }

}
