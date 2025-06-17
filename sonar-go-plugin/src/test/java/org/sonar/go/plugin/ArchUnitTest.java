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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "org.sonar.go", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchUnitTest {
  @ArchTest
  static final ArchRule class_annotated_with_scanner_side_should_be_annotated_with_sonar_lint_side = ArchRuleDefinition.classes()
    .that().areAnnotatedWith("org.sonar.api.scanner.ScannerSide")
    .should().beAnnotatedWith("org.sonarsource.api.sonarlint.SonarLintSide")
    .because("Classes annotated with @ScannerSide must also be annotated with @SonarLintSide to avoid breaking SQIDE connected mode.");
}
