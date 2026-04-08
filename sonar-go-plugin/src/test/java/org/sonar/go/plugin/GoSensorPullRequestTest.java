/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.plugin.caching.DummyReadCache;
import org.sonar.go.plugin.caching.DummyWriteCache;
import org.sonar.go.report.GoProgressReport;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.go.testing.TestInputFileCreator.createInputFile;

class GoSensorPullRequestTest {
  private static final String ORIGINAL_FILE_CONTENT = """
    package main
    func main() {
      print (1 == 1)
    }
    """;

  private SensorContextTester sensorContext;
  private DummyWriteCache nextCache;
  private byte[] md5Hash;
  private InputFile inputFile;
  private InputFileContext inputFileContext;
  private List<GoFolder> goFolders;
  private GoConverter converter;
  private PullRequestAwareVisitor visitor;
  private String hashKey;
  private GoProgressReport goProgressReport;
  private File baseDir;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  /**
   * Set up for happy with PR context
   */
  @BeforeEach
  void setup(@TempDir File tmpBaseDir) throws NoSuchAlgorithmException, IOException {
    baseDir = tmpBaseDir;

    // Enable PR context
    sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setCanSkipUnchangedFiles(true);
    sensorContext.setCacheEnabled(true);
    // Add one unchanged file to analyze
    inputFile = createInputFile(
      "file1.go",
      ORIGINAL_FILE_CONTENT,
      baseDir,
      InputFile.Status.SAME, null);
    sensorContext.fileSystem().add(inputFile);
    inputFileContext = new InputFileContext(sensorContext, inputFile);
    // Add the hash of the file to the cache
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    try (InputStream in = new ByteArrayInputStream(ORIGINAL_FILE_CONTENT.getBytes(StandardCharsets.UTF_8))) {
      md5Hash = md5.digest(in.readAllBytes());
    }
    DummyReadCache previousCache = new DummyReadCache();
    hashKey = "slang:hash:" + inputFile.key();
    previousCache.persisted.put(hashKey, md5Hash);
    sensorContext.setPreviousCache(previousCache);

    // Bind the next cache
    nextCache = spy(new DummyWriteCache());
    nextCache.bind(previousCache);
    sensorContext.setNextCache(nextCache);

    converter = spy(TestGoConverterSingleFile.GO_CONVERTER);
    visitor = spy(new SuccessfulReuseVisitor());
    goProgressReport = new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10));
    goFolders = List.of(new GoFolder("myFolder", List.of(inputFileContext)));
  }

  @Test
  void shouldSkipsConversionForUnchangedFileWithCachedResults() {
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor).reusePreviousResults(inputFileContext);
    verify(converter, never()).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Checking that previous results can be reused for input file moduleKey:file1.go.",
      "Skipping input file moduleKey:file1.go (status is unchanged).");
    assertThat(nextCache.persisted).containsKey(hashKey);
    verify(nextCache).copyFromPrevious(hashKey);
  }

  @Test
  void shouldNotSkipConversionForUnchangedFileWhenCachedResultsCannotBeReused() {
    // Set the only pull request aware visitor to fail reusing previous results
    visitor = spy(new GoSensorTest.FailingToReuseVisitor());
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Checking that previous results can be reused for input file moduleKey:file1.go.",
      "Visitor FailingToReuseVisitor failed to reuse previous results for input file moduleKey:file1.go.",
      "Will convert input file moduleKey:file1.go for full analysis.");
    assertThat(nextCache.persisted).containsKey(hashKey);
    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenUnchangedFilesCannotBeSkipped() {
    // Disable the skipping of unchanged files
    sensorContext.setCanSkipUnchangedFiles(false);
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG)).doesNotContain(
      "Skipping input file moduleKey:file1.go (status is unchanged).");
    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenTheFileHasSameContentsButInputFileStatusIsChanged() {
    // Create a changed file
    InputFile changedFile = createInputFile(
      "file1.go",
      ORIGINAL_FILE_CONTENT,
      baseDir,
      InputFile.Status.CHANGED,
      null);
    inputFileContext = new InputFileContext(sensorContext, changedFile);
    sensorContext.fileSystem().add(changedFile);
    goProgressReport.start(goFolders);
    // Execute analyzeFile
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG))
      .doesNotContain("Skipping input file moduleKey:file1.go (status is unchanged).")
      .contains("File moduleKey:file1.go is considered changed: file status is CHANGED.");

    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenTheFileContentHasChangedButInputFileStatusIsSame() {
    // Create a changed file
    InputFile changedFile = createInputFile(
      "file1.go",
      "// This is definitely not the same thing\npackage main",
      baseDir,
      InputFile.Status.SAME,
      null);
    sensorContext.fileSystem().add(changedFile);
    inputFileContext = new InputFileContext(sensorContext, changedFile);
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG)).doesNotContain("Skipping input file moduleKey:file1.go (status is unchanged).");

    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenTheFileContentIsUnchangedButCacheIsDisabled() {
    // Disable caching
    sensorContext.setCacheEnabled(false);
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG))
      .doesNotContain("Skipping input file moduleKey:file1.go (status is unchanged).")
      .contains("File moduleKey:file1.go is considered changed: hash cache is disabled.");

    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache, never()).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenTheFileContentIsUnchangedButNoHashInCache() {
    // Set an empty previous cache
    sensorContext.setPreviousCache(new DummyReadCache());
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG))
      .doesNotContain("Skipping input file moduleKey:file1.go (status is unchanged).")
      .contains("File moduleKey:file1.go is considered changed: hash could not be found in the cache.");

    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void shouldNotSkipConversionWhenTheFileContentIsUnchangedButFailingToReadTheCache() {
    // Set a previous cache that contains a stream to a hash that will fail to close
    DummyReadCache corruptedCache = spy(new DummyReadCache());
    doReturn(true).when(corruptedCache).contains(hashKey);
    InputStream failingToClose = new ByteArrayInputStream(ORIGINAL_FILE_CONTENT.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public void close() throws IOException {
        throw new IOException("BOOM!");
      }
    };
    doReturn(failingToClose).when(corruptedCache).read(hashKey);
    sensorContext.setPreviousCache(corruptedCache);
    // Execute analyzeFile
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor, never()).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    assertThat(logTester.logs(Level.DEBUG))
      .doesNotContain("Skipping input file moduleKey:file1.go (status is unchanged).")
      .contains("File moduleKey:file1.go is considered changed: failed to read hash from the cache.");

    verify(nextCache, never()).copyFromPrevious(hashKey);
    verify(nextCache).write(eq(hashKey), any(byte[].class));
  }

  @Test
  void visitorShouldNotBeCalledToVisitTheAstAfterConversion() {
    GoSensorTest.FailingToReuseVisitor failing = spy(new GoSensorTest.FailingToReuseVisitor());
    goProgressReport.start(goFolders);
    GoSensor.analyseDirectory(
      converter,
      List.of(inputFileContext),
      List.of(visitor, failing),
      new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10)),
      new DurationStatistics(sensorContext.config()),
      sensorContext,
      "MyModuleName");
    verify(visitor).reusePreviousResults(inputFileContext);
    verify(failing).reusePreviousResults(inputFileContext);
    verify(converter).parse(anyMap(), anyString());
    verify(visitor, never()).scan(eq(inputFileContext), any(Tree.class));
    verify(failing).scan(eq(inputFileContext), any(Tree.class));
    assertThat(logTester.logs(Level.DEBUG)).doesNotContain(
      "Skipping input file moduleKey:file1.go (status is unchanged).");
  }

  private static class SuccessfulReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return true;
    }
  }
}
