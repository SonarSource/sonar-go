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

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class GoLanguage extends AbstractLanguage {

  public static final String KEY = "go";
  public static final String FILE_SUFFIXES_KEY = "sonar.go.file.suffixes";
  public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".go";

  private final Configuration configuration;

  public GoLanguage(Configuration configuration) {
    super(KEY, "Go");
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = configuration.getStringArray(FILE_SUFFIXES_KEY);
    if (suffixes.length == 0) {
      suffixes = new String[] {FILE_SUFFIXES_DEFAULT_VALUE};
    }
    return suffixes;
  }
}
