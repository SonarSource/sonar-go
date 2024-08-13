/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static org.assertj.core.api.Assertions.assertThat;

class ApexSensorTest extends AbstractSensorTest {

  @Test
  void test_highlighting() {
    InputFile inputFile = createInputFile("file1.cls", "" +
      "public class Foo {\n" +
      "  /**\n" +
      "   * multi line comment\n" +
      "   */\n" +
      "  public Double myValue = 42.00;\n" +
      "\n" +
      "  String companyName = 'SonarSource';\n" +
      "\n" +
      "  public static Double getCalculatedValueViaPrice (Decimal price) {\n" +
      "    // single line comment\n" +
      "    myValue = myValue + price;\n" +
      "    return myValue;\n" +
      "  }\n" +
      "}");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    String key = inputFile.key();
    assertThat(context.highlightingTypeAt(key, 1, 0)).containsExactly(TypeOfText.KEYWORD); // public
    assertThat(context.highlightingTypeAt(key, 1, 8)).containsExactly(TypeOfText.KEYWORD); // class
    assertThat(context.highlightingTypeAt(key, 1, 14)).isEmpty(); // Foo
    assertThat(context.highlightingTypeAt(key, 1, 19)).isEmpty(); // {
    assertThat(context.highlightingTypeAt(key, 2, 3)).containsExactly(TypeOfText.COMMENT); // /**
    assertThat(context.highlightingTypeAt(key, 3, 4)).containsExactly(TypeOfText.COMMENT); // * multi ...
    assertThat(context.highlightingTypeAt(key, 4, 4)).containsExactly(TypeOfText.COMMENT); // */
    assertThat(context.highlightingTypeAt(key, 5, 3)).containsExactly(TypeOfText.KEYWORD); // public
    assertThat(context.highlightingTypeAt(key, 5, 10)).isEmpty(); // Double
    assertThat(context.highlightingTypeAt(key, 5, 17)).isEmpty(); // myValue
    assertThat(context.highlightingTypeAt(key, 5, 25)).isEmpty(); // =
    assertThat(context.highlightingTypeAt(key, 5, 27)).containsExactly(TypeOfText.CONSTANT); // 42.00
    assertThat(context.highlightingTypeAt(key, 7, 3)).isEmpty(); // String
    assertThat(context.highlightingTypeAt(key, 7, 10)).isEmpty(); // companyName
    assertThat(context.highlightingTypeAt(key, 7, 24)).containsExactly(TypeOfText.STRING); // 'SonarSource'
    assertThat(context.highlightingTypeAt(key, 9, 3)).containsExactly(TypeOfText.KEYWORD); // public
    assertThat(context.highlightingTypeAt(key, 9, 10)).containsExactly(TypeOfText.KEYWORD); // static
    assertThat(context.highlightingTypeAt(key, 9, 24)).isEmpty(); // getCalculatedValueViaPrice
    assertThat(context.highlightingTypeAt(key, 9, 51)).isEmpty(); // (
    assertThat(context.highlightingTypeAt(key, 9, 52)).isEmpty(); // Decimal
    assertThat(context.highlightingTypeAt(key, 10, 5)).containsExactly(TypeOfText.COMMENT); // single line comment
    assertThat(context.highlightingTypeAt(key, 12, 5)).containsExactly(TypeOfText.KEYWORD); // return
  }

  @Test
  void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.cls", "invalid apex source code");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("ParsingError");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.cls");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(1);
    assertThat(textPointer.lineOffset()).isEqualTo(8);
    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 1:8", inputFile.uri()));
  }


  @Override
  protected String repositoryKey() {
    return ApexPlugin.APEX_REPOSITORY_KEY;
  }

  @Override
  protected ApexLanguage language() {
    return new ApexLanguage(new MapSettings().asConfig());
  }

  private ApexSensor sensor(CheckFactory checkFactory) {
    return new ApexSensor(SQ_LTS_RUNTIME, checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
  }
}
