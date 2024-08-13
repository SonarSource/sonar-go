/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.converter.ApexConverterTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;

public class Verifier {

  private static final Path BASE_DIR = Paths.get("src", "test", "resources", "com", "sonarsource", "apex", "checks");
  private static final ASTConverter CONVERTER = ApexConverterTest.converter();

  public static void verify(String fileName, SlangCheck check) {
    org.sonarsource.slang.testing.Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check);
  }

  public static void verifyNoIssue(String fileName, SlangCheck check) {
    org.sonarsource.slang.testing.Verifier.verifyNoIssue(CONVERTER, BASE_DIR.resolve(fileName), check);
  }

}
