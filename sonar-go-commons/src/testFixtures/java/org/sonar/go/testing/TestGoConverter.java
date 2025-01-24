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

import java.io.File;
import java.nio.file.Paths;
import org.sonar.go.api.Tree;
import org.sonar.go.converter.GoConverter;

public class TestGoConverter {
  private static final File CONVERTER_DIR = Paths.get("build", "tmp").toFile();
  public static final GoConverter GO_CONVERTER = new GoConverter(CONVERTER_DIR);

  public static Tree parse(String content) {
    return GO_CONVERTER.parse(content);
  }
}
