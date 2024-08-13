/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor;

import com.sonarsource.apex.converter.visitor.generation.AstVisitorCodeGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class GeneratedApexAstVisitorTest {

  private static final String GENERATED_CLASS_PACKAGE = "com.sonarsource.apex.converter.visitor";
  private static final String GENERATED_CLASS_NAME = "GeneratedApexAstVisitor";

  private static final Path GENERATED_CLASS_PACKAGE_PATH = Paths.get("src", "main", "java")
    .resolve(GENERATED_CLASS_PACKAGE.replace('.', File.separatorChar));
  private static final Path GENERATED_CLASS_PATH = GENERATED_CLASS_PACKAGE_PATH.resolve(GENERATED_CLASS_NAME + ".java");

  @Test
  void validate_generated_class() throws IOException {
    String expectedImplementation = generateAstVisitorCode();
    String actualImplementation = new String(Files.readAllBytes(GENERATED_CLASS_PATH), UTF_8);
    assertThat(expectedImplementation)
      .describedAs("Unexpected Implementation, use GeneratedApexAstVisitorTest#main to regenerate.")
      .isEqualTo(actualImplementation);
  }

  /**
   * Should only be ran to regenerate the Visitor.
   * It can be useful in case the 3rd party Apex Jar has been updated
   */
  public static void main(String[] args) throws IOException {
    updateGeneratedClass();
  }

  private static void updateGeneratedClass() throws IOException {
    Files.write(GENERATED_CLASS_PATH, generateAstVisitorCode().getBytes(UTF_8));
  }

  private static String generateAstVisitorCode() {
    AstVisitorCodeGenerator generation = new AstVisitorCodeGenerator(GENERATED_CLASS_PACKAGE, GENERATED_CLASS_NAME);
    return generation.generate();
  }

}
