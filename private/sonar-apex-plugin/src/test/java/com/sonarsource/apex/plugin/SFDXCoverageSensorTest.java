/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class SFDXCoverageSensorTest {

  private static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");
  private static final String MODULE_KEY = "/Absolute/Path/To/";

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_description() {
    SensorDescriptor sensorDescriptor = spy(new DefaultSensorDescriptor());
    new SFDXCoverageSensor().describe(sensorDescriptor);

    // assert that newCoverage method is called only once on ClassB
    verify(sensorDescriptor, times(1)).name("Test coverage Sensor for Apex");
    verify(sensorDescriptor, times(1)).onlyOnLanguage("apex");
  }

  @Test
  void test_coverage_report() throws IOException {
    SensorContextTester context = getSensorContext("test-result-codecoverage.json", "classes/AccountDeletion.trigger", "classes/ClassB.cls");
    new SFDXCoverageSensor().execute(context);

    assertClassBLineHits(context);
  }

  @Test
  void test_absolute_report_path() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("test-result-codecoverage.json");

    SensorContextTester context = getSensorContext(reportPath.toString(), "classes/AccountDeletion.trigger", "classes/ClassB.cls");
    new SFDXCoverageSensor().execute(context);

    assertClassBLineHits(context);
  }

  @Test
  void no_coverage_when_property_not_set() throws IOException {
    SensorContextTester context = getSensorContext(null, "classes/AccountDeletion.trigger", "classes/ClassB.cls");
    new SFDXCoverageSensor().execute(context);

    String fileKey = MODULE_KEY + ":classes/ClassB.cls";
    assertThat(context.lineHits(fileKey, 1)).isNull();
  }

  private void assertClassBLineHits(SensorContextTester context) {
    String fileKey = MODULE_KEY + ":classes/ClassB.cls";
    assertThat(context.lineHits(fileKey, 1)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 2)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 3)).isEqualTo(3);
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 5)).isZero();
    assertThat(context.lineHits(fileKey, 6)).isZero();
    assertThat(context.lineHits(fileKey, 7)).isNull();
    assertThat(context.lineHits(fileKey, 8)).isNull();
    assertThat(context.lineHits(fileKey, 9)).isNull();
    assertThat(context.lineHits(fileKey, 10)).isEqualTo(5);
    assertThat(context.lineHits(fileKey, 11)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 12)).isNull();
    assertThat(context.lineHits(fileKey, 13)).isNull();
    assertThat(context.lineHits(fileKey, 14)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 15)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 16)).isZero();
    assertThat(context.lineHits(fileKey, 17)).isNull();
    assertThat(context.lineHits(fileKey, 18)).isEqualTo(1);
  }

  @Test
  void no_measure_on_files_not_in_context() throws IOException {
    SensorContextTester context = spy(getSensorContext("additional_file_codecoverage.json", "classes/ClassB.cls"));
    new SFDXCoverageSensor().execute(context);

    // assert that newCoverage method is called only once on ClassB
    verify(context, times(1)).newCoverage();
    assertThat(context.lineHits(MODULE_KEY + ":classes/ClassB.cls", 2)).isEqualTo(5);
  }

  @Test
  void log_when_wrong_line_numbers() throws IOException {
    SensorContextTester context = getSensorContext("wrong_lines_codecoverage.json", "classes/AccountDeletion.trigger");
    new SFDXCoverageSensor().execute(context);

    String expectedMessage = "Invalid coverage information on file: 'AccountDeletion'";
    assertThat(logTester.logs()).contains(expectedMessage);
  }

  @Test
  void log_when_invalid_format() throws IOException {
    SensorContextTester context = getSensorContext("invalid_codecoverage.json", "classes/AccountDeletion.trigger");
    new SFDXCoverageSensor().execute(context);

    String expectedMessage = "Cannot read coverage report file, expecting standard SFDX test coverage result in JSON format: 'invalid_codecoverage.json'";
    assertThat(logTester.logs()).contains(expectedMessage);
  }

  @Test
  void log_when_invalid_report_path() throws IOException {
    SensorContextTester context = getSensorContext("noFile.json", "classes/AccountDeletion.trigger");
    new SFDXCoverageSensor().execute(context);

    assertThat(logTester.logs()).contains("SFDX coverage report not found: 'noFile.json'");
  }

  @Test
  void log_when_several_files_with_same_name() throws IOException {
    SensorContextTester context = getSensorContext("test-result-codecoverage.json", "classes/AccountDeletion.trigger", "classes/ClassB.cls", "classes/subfolder/AccountDeletion.cls");
    new SFDXCoverageSensor().execute(context);

    assertThat(logTester.logs()).contains("More than one file found with name: 'AccountDeletion'");
  }

  @Test
  void log_when_io_exception() throws IOException {
    SensorContextTester context = getSensorContext(null, "classes/AccountDeletion.trigger", "classes/ClassB.cls");
    String reportPath = "test-result-codecoverage.json";
    context.settings().setProperty("sonar.apex.coverage.reportPath", reportPath);

    DefaultInputFile coverageFile = createInputFile(reportPath, "");
    coverageFile = spy(coverageFile);
    when(coverageFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(coverageFile);

    new SFDXCoverageSensor().execute(context);

    assertThat(logTester.logs()).contains("Error reading coverage report: 'test-result-codecoverage.json'");
  }

  private SensorContextTester getSensorContext(@Nullable String coverageReportPath, String... fileNames) throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    if (coverageReportPath != null) {
      context.settings().setProperty("sonar.apex.coverage.reportPath", coverageReportPath);
    }

    DefaultFileSystem defaultFileSystem = new DefaultFileSystem(new File(MODULE_KEY));
    createReportFiles(coverageReportPath, baseDir, defaultFileSystem);
    for (String fileName : fileNames) {
      DefaultInputFile inputFile = createInputFile(fileName, fileContent(baseDir, fileName));
      defaultFileSystem.add(inputFile);
    }

    context.setFileSystem(defaultFileSystem);
    return context;
  }

  private void createReportFiles(@Nullable String reportPath, Path baseDir, DefaultFileSystem defaultFileSystem) throws IOException {
    if (reportPath == null) {
      return;
    }

    reportPath = reportPath.trim();
    // if report is relative path we create it under fake filesystem
    if (!Paths.get(reportPath).isAbsolute()) {
      try {
        DefaultInputFile coverageFile = createInputFile(reportPath, fileContent(baseDir, reportPath));
        defaultFileSystem.add(coverageFile);
      } catch (NoSuchFileException e) {
        // tests can simulate non-existing file, this is OK
      }
    }
  }

  private DefaultInputFile createInputFile(String fileName, String content) {
    return TestInputFileBuilder.create(MODULE_KEY, fileName)
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build();
  }

  private String fileContent(Path baseDir, String fileName) throws IOException {
    Path filePath = baseDir.resolve(fileName);
    return new String(Files.readAllBytes(filePath), UTF_8);
  }

}
