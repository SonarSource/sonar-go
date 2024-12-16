/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonar.go.checks;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.sonar.go.converter.GoConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;

public class GoVerifier {
  private static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");
  private static final ASTConverter CONVERTER = new GoConverter(Paths.get("build", "tmp").toFile());

  public static void verify(String fileName, SlangCheck check) {
    org.sonarsource.slang.testing.Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check);
  }
}
