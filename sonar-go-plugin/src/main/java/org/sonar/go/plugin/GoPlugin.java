/*
 * SonarQube Go Plugin
 * Copyright (C) 2018-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.go.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;
import org.sonar.go.coverage.GoCoverSensor;
import org.sonar.go.externalreport.GoLintReportSensor;
import org.sonar.go.externalreport.GoMetaLinterReportSensor;
import org.sonar.go.externalreport.GoVetReportSensor;
import org.sonar.go.externalreport.GolangCILintReportSensor;
import org.sonar.go.testreport.GoTestSensor;

public class GoPlugin implements Plugin {

  static final String RESOURCE_FOLDER = "org/sonar/l10n/go/rules/go";

  public static final String EXCLUSIONS_KEY = "sonar.go.exclusions";
  public static final String EXCLUSIONS_DEFAULT_VALUE = "**/vendor/**";

  private static final String GO_CATEGORY = "Go";
  private static final String TEST_COVERAGE_SUBCATEGORY = "Test and Coverage";
  private static final String EXTERNAL_LINTER_SUBCATEGORY = "Popular Rule Engines";
  private static final String GENERAL_SUBCATEGORY = "General";

  @Override
  public void define(Context context) {
    context.addExtensions(
      GoLanguage.class,
      GoSensor.class,
      GoTestSensor.class,
      GoCoverSensor.class,
      GoExclusionsFileFilter.class,
      GoRulesDefinition.class,
      GoProfileDefinition.class,
      GoVetReportSensor.class,
      GoLintReportSensor.class,
      GoMetaLinterReportSensor.class,
      GolangCILintReportSensor.class,

      PropertyDefinition.builder(GoLanguage.FILE_SUFFIXES_KEY)
        .index(10)
        .name("File Suffixes")
        .description("List of suffixes for files to analyze.")
        .category(GO_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(GoLanguage.FILE_SUFFIXES_DEFAULT_VALUE)
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(EXCLUSIONS_KEY)
        .index(11)
        .defaultValue(EXCLUSIONS_DEFAULT_VALUE)
        .name("Go Exclusions")
        .description("List of file path patterns to be excluded from analysis of Go files.")
        .category(GO_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GoTestSensor.REPORT_PATH_KEY)
        .index(19)
        .name("Path to test execution report(s)")
        .description("Path to test execution reports generated by Go with '-json' key, available since go1.10 (e.g.: go test -json > test-report.out).")
        .category(GO_CATEGORY)
        .subCategory(TEST_COVERAGE_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GoCoverSensor.REPORT_PATH_KEY)
        .index(20)
        .name("Path to coverage report(s)")
        .description("Path to coverage reports generated by Go (e.g.: go test -coverprofile=coverage.out), ant patterns relative to project root are supported.")
        .category(GO_CATEGORY)
        .subCategory(TEST_COVERAGE_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GoVetReportSensor.PROPERTY_KEY)
        .index(30)
        .name("\"go vet\" Report Files")
        .description("Paths (absolute or relative) to the files with \"go vet\" issues.")
        .category(GO_CATEGORY)
        .subCategory(EXTERNAL_LINTER_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue("")
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GoLintReportSensor.PROPERTY_KEY)
        .index(31)
        .name("Golint Report Files")
        .description("Paths (absolute or relative) to the files with Golint issues.")
        .category(GO_CATEGORY)
        .subCategory(EXTERNAL_LINTER_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue("")
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GoMetaLinterReportSensor.PROPERTY_KEY)
        .index(32)
        .name("GoMetaLinter Report Files")
        .description("Paths (absolute or relative) to the files with GoMetaLinter issues.")
        .category(GO_CATEGORY)
        .subCategory(EXTERNAL_LINTER_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue("")
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(GolangCILintReportSensor.PROPERTY_KEY)
        .index(33)
        .name("GolangCI-Lint Report Files")
        .description("Paths (absolute or relative) to the files with GolangCI-Lint issues.")
        .category(GO_CATEGORY)
        .subCategory(EXTERNAL_LINTER_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue("")
        .multiValues(true)
        .build());
  }
}
