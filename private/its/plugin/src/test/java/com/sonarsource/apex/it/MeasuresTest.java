/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import com.sonar.orchestrator.build.SonarScanner;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonarqube.ws.Measures;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasuresTest extends TestBase {

  private static final String BASE_DIRECTORY_MEASURES = "projects/measures/";
  private static final String PROJECT_KEY = "apexMeasures";
  
  public static final String EMPTY_FILE = "empty_file.cls";
  public static final String FILE_1 = "file1.cls";
  public static final String FILE_2 = "file2.cls";
  
  public static final String MEASURE_COMMENT_LINES = "comment_lines";
  public static final String MEASURE_NCLOC = "ncloc";
  public static final String MEASURE_NCLOC_DATA = "ncloc_data";
  public static final String MEASURE_FUNCTIONS = "functions";
  public static final String MEASURE_COGNITIVE_COMPLEXITY = "cognitive_complexity";
  public static final String MEASURE_STATEMENTS = "statements";
  public static final String MEASURE_EXECUTABLE_LINES_DATA = "executable_lines_data";

  @Test
  public void measures_test() {
    SonarScanner sonarScanner = getSonarScanner(PROJECT_KEY, BASE_DIRECTORY_MEASURES);
    sonarScanner.setProperty("sonar.apex.coverage.reportPath", "test-result-codecoverage.json");
    ORCHESTRATOR.executeBuild(sonarScanner);

    String file1 = componentKey(FILE_1);
    String file2 = componentKey(FILE_2);
    String file3 = componentKey("file3.cls");

    assertThat(getMeasureAsInt(PROJECT_KEY, "files")).isEqualTo(4);

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_COMMENT_LINES)).isNull();
    assertThat(getMeasureAsInt(file1, MEASURE_COMMENT_LINES)).isEqualTo(5);
    assertThat(getMeasureAsInt(file2, MEASURE_COMMENT_LINES)).isZero();

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_NCLOC)).isNull();
    assertThat(getMeasureAsInt(file1, MEASURE_NCLOC)).isEqualTo(6);
    assertThat(getMeasureAsInt(file2, MEASURE_NCLOC)).isEqualTo(21);

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_NCLOC_DATA)).isNull();
    assertCoverage(getMeasure(PROJECT_KEY, FILE_1, MEASURE_NCLOC_DATA), "17=1;2=1;6=1;11=1;13=1;15=1");
    assertCoverage(getMeasure(PROJECT_KEY, FILE_2, MEASURE_NCLOC_DATA), "1=1;2=1;4=1;5=1;6=1;7=1;8=1;9=1;10=1;11=1;12=1;14=1;15=1;16=1;17=1;18=1;20=1;21=1;22=1;23=1;24=1");

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_FUNCTIONS)).isNull();
    assertThat(getMeasureAsInt(file1, MEASURE_FUNCTIONS)).isEqualTo(2);
    assertThat(getMeasureAsInt(file2, MEASURE_FUNCTIONS)).isEqualTo(1);
    assertThat(getMeasureAsInt(file3, MEASURE_FUNCTIONS)).isEqualTo(3);
    assertThat(getMeasureAsInt(PROJECT_KEY, MEASURE_FUNCTIONS)).isEqualTo(6);

    assertThat(getMeasureAsInt(file1, MEASURE_COGNITIVE_COMPLEXITY)).isZero();
    assertThat(getMeasureAsInt(file2, MEASURE_COGNITIVE_COMPLEXITY)).isEqualTo(4);
    assertThat(getMeasureAsInt(file3, MEASURE_COGNITIVE_COMPLEXITY)).isEqualTo(7);

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_STATEMENTS)).isNull();
    assertThat(getMeasureAsInt(file1, MEASURE_STATEMENTS)).isZero();
    assertThat(getMeasureAsInt(file2, MEASURE_STATEMENTS)).isEqualTo(8);
    assertThat(getMeasureAsInt(file3, MEASURE_STATEMENTS)).isEqualTo(11);

    assertThat(getMeasure(PROJECT_KEY, EMPTY_FILE, MEASURE_EXECUTABLE_LINES_DATA)).isNull();
    assertCoverage(getMeasure(PROJECT_KEY, FILE_1, MEASURE_EXECUTABLE_LINES_DATA), "2=1");
    assertCoverage(getMeasure(PROJECT_KEY, FILE_2, MEASURE_EXECUTABLE_LINES_DATA), "17=1;1=1;20=1;21=1;5=1;7=1;10=1;14=1;15=1");

    assertThat(getMeasureAsInt(file2, "lines_to_cover")).isEqualTo(10);
    assertThat(getMeasureAsInt(file2, "uncovered_lines")).isEqualTo(2);
    assertThat(getMeasureAsInt(file2, "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt(file2, "uncovered_conditions")).isNull();
  }

  private static String componentKey(String fileName) {
    return PROJECT_KEY + ":" + fileName;
  }

  private static void assertCoverage(Measures.Measure nclocData, String expected) {
    Set<String> nclocDataSet = Arrays.stream(nclocData.getValue().split(";")).collect(Collectors.toSet());
    Set<String> expectedSet = Arrays.stream(expected.split(";")).collect(Collectors.toSet());
    assertThat(nclocDataSet).isEqualTo(expectedSet);
  }


}
