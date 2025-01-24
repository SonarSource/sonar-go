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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.go.api.ASTConverter;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.checks.IdenticalBinaryOperandCheck;
import org.sonar.go.checks.StringLiteralDuplicatedCheck;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.plugin.caching.DummyReadCache;
import org.sonar.go.plugin.caching.DummyWriteCache;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.go.testing.TextRangeAssert.assertThat;

class SlangSensorTest extends AbstractSensorTest {

  @Test
  void test_one_rule() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func main() {
        print (1 == 1)
      }""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1764");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Correct one of the identical sub-expressions on both sides this operator");
    assertThat(location.textRange()).hasRange(3, 14, 3, 15);
  }

  @Test
  void test_rule_with_gap() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func f() {
        print("string literal")
        print("string literal")
        print("string literal")
      }""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1192");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1192");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Define a constant instead of duplicating this literal \"string literal\" 3 times.");
    assertThat(location.textRange()).hasRange(3, 8, 3, 24);
    assertThat(issue.gap()).isEqualTo(2.0);
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void test_commented_code() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func main() {
      // func foo() { if (true) {print("string literal");}}
      print (1 == 1);
      print(b);
      // a b c ...
      foo();
      // Coefficients of polynomial
      b = DoubleArray(n); // linear
      c = DoubleArray(n + 1); // quadratic
      d = DoubleArray(n); // cubic
      }""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S125");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Remove this commented out code.");
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void test_nosonar_commented_code() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func main() {
        // func foo() { if (true) {print("string literal");}} NOSONAR
        print (1 == 1);
        print(b);
        // a b c ...
        foo();
        // Coefficients of polynomial
        b = DoubleArray(n); // linear
        c = DoubleArray(n + 1); // quadratic
        d = DoubleArray(n); // cubic
      }""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void simple_file() {
    InputFile inputFile = createInputFile("file1.go",
      """
        package main
        func main(x int) {
          print (1 == 1)
          print("abc")
        }
        type A struct {}""");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    assertThat(context.highlightingTypeAt(inputFile.key(), 2, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 2, 4)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(6);
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero();
    assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    assertThat(context.cpdTokens(inputFile.key()).get(1).getValue()).isEqualTo("funcmain(xint){");
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2);

    assertThat(logTester.logs()).contains("1 source file to be analyzed");
  }

  @Test
  void suppress_issues_in_class() {
    InputFile inputFile = createInputFile("file1.slang", """
      @Suppress("slang:S1764")
      class { fun main() {
      print (1 == 1);} }""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  @Disabled("SONARGO-100 Fix NOSONAR suppression")
  void suppress_issues_in_var() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func bar() {
        b = (1 == 1);  // NOSONAR
        c = (1 == 1);
      }
      """);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    IssueLocation location = issue.primaryLocation();
    assertThat(location.textRange()).hasRange(5, 22, 5, 23);
  }

  @Test
  void test_fail_input() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.slang", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.slang");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.slang",
      """
         class A {
         fun x() {}
         fun y() {}\
        """);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    sensor(checkFactory).execute(context);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S2260");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("A parsing error occurred in this file.");
    assertThat(location.textRange()).isNull();

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.slang");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  void test_fail_parsing_without_parsing_error_rule_activated() {
    InputFile inputFile = createInputFile("file1.slang", "{");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).hasSize(1);
  }

  @Test
  void test_empty_file() {
    InputFile inputFile = createInputFile("empty.slang", "\t\t  \r\n  \n ");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).isEmpty();
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void test_failure_in_check() {
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func f() {}""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = mock(CheckFactory.class);
    var checks = mock(Checks.class);
    GoCheck failingCheck = init -> init.register(TopLevelTree.class, (ctx, tree) -> {
      throw new IllegalStateException("BOUM");
    });
    when(checks.ruleKey(failingCheck)).thenReturn(RuleKey.of(repositoryKey(), "failing"));
    when(checkFactory.create(repositoryKey())).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(failingCheck));
    sensor(checkFactory).execute(context);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(logTester.logs()).contains("Cannot analyse 'file1.slang': BOUM");
  }

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    SlangSensor sensor = sensor(mock(CheckFactory.class));
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.languages()).hasSize(1);
    assertThat(sensorDescriptor.languages()).containsExactly("slang");
    assertThat(sensorDescriptor.name()).isEqualTo("SLang Sensor");
  }

  @Test
  void test_sonarlint_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    SlangSensor sensor = sensor(SonarRuntimeImpl.forSonarLint(Version.create(6, 5)), mock(CheckFactory.class));
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.languages()).hasSize(1);
    assertThat(sensorDescriptor.languages()).containsExactly("slang");
    assertThat(sensorDescriptor.name()).isEqualTo("SLang Sensor");
  }

  @Test
  void test_cancellation() {
    InputFile inputFile = createInputFile("file1.slang",
      "fun main() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    context.setCancelled(true);
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void test_sonarlint_context() {
    SonarRuntime sonarLintRuntime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9));
    SensorContextTester context = SensorContextTester.create(baseDir);
    InputFile inputFile = createInputFile("file1.slang", """
      package main
      func main(x int) {
        print (1 == 1)
        print("abc")
      }
      type A struct {}""");
    context.fileSystem().add(inputFile);
    context.setRuntime(sonarLintRuntime);
    sensor(checkFactory("S1764")).execute(context);

    assertThat(context.allIssues()).hasSize(1);

    // No CPD, highlighting and metrics in SonarLint
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC)).isNull();
    assertThat(context.cpdTokens(inputFile.key())).isNull();

    assertThat(logTester.logs()).contains("1 source file to be analyzed");
  }

  @Test
  void test_sensor_descriptor_does_not_process_files_independently() {
    final SlangSensor sensor = sensor(
      SonarRuntimeImpl.forSonarQube(Version.create(9, 3), SonarQubeSide.SCANNER, SonarEdition.DEVELOPER),
      checkFactory());
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.isProcessesFilesIndependently()).isFalse();
  }

  @Test
  void test_sensor_logs_when_unchanged_files_can_be_skipped() {
    // Enable PR context
    SensorContextTester sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setCanSkipUnchangedFiles(true);
    sensorContext.setCacheEnabled(true);
    // Execute sensor
    SlangSensor sensor = sensor(
      SonarRuntimeImpl.forSonarQube(Version.create(9, 3), SonarQubeSide.SCANNER, SonarEdition.DEVELOPER),
      checkFactory());
    sensor.execute(sensorContext);
    assertThat(logTester.logs(Level.INFO)).contains(
      "The SLANG analyzer is running in a context where unchanged files can be skipped.");
  }

  @Nested
  class PullRequestContext {
    private static final String ORIGINAL_FILE_CONTENT = """
      package main
      func main() {
        print (1 == 1)
      }
      """;

    private SensorContextTester sensorContext;
    private DummyWriteCache nextCache;
    byte[] md5Hash;
    private InputFile inputFile;
    private InputFileContext inputFileContext;
    private GoConverter converter;
    private PullRequestAwareVisitor visitor;
    private String hashKey;

    /**
     * Set up for happy with PR context
     */
    @BeforeEach
    void setup() throws NoSuchAlgorithmException, IOException {
      // Enable PR context
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setCanSkipUnchangedFiles(true);
      sensorContext.setCacheEnabled(true);
      // Add one unchanged file to analyze
      inputFile = createInputFile(
        "file1.slang",
        ORIGINAL_FILE_CONTENT,
        InputFile.Status.SAME);
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

      converter = spy(TestGoConverter.GO_CONVERTER);
      visitor = spy(new SuccessfulReuseVisitor());
    }

    @Test
    void skips_conversion_for_unchanged_file_with_cached_results() {
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, times(1)).reusePreviousResults(inputFileContext);
      verify(converter, never()).parse(any(String.class), any(String.class));
      assertThat(logTester.logs(Level.DEBUG)).contains(
        "Checking that previous results can be reused for input file moduleKey:file1.slang.",
        "Skipping input file moduleKey:file1.slang (status is unchanged).");
      assertThat(nextCache.persisted).containsKey(hashKey);
      verify(nextCache, times(1)).copyFromPrevious(hashKey);
    }

    @Test
    void does_not_skip_conversion_for_unchanged_file_when_cached_results_cannot_be_reused() {
      // Set the only pull request aware visitor to fail reusing previous results
      visitor = spy(new FailingToReuseVisitor());
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, times(1)).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG)).contains(
        "Checking that previous results can be reused for input file moduleKey:file1.slang.",
        "Visitor FailingToReuseVisitor failed to reuse previous results for input file moduleKey:file1.slang.",
        "Will convert input file moduleKey:file1.slang for full analysis.");
      assertThat(nextCache.persisted).containsKey(hashKey);
      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_unchanged_files_cannot_be_skipped() {
      // Disable the skipping of unchanged files
      sensorContext.setCanSkipUnchangedFiles(false);
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG)).doesNotContain(
        "Skipping input file moduleKey:file1.slang (status is unchanged).");
      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_the_file_has_same_contents_but_input_file_status_is_changed() {
      // Create a changed file
      InputFile changedFile = createInputFile(
        "file1.slang",
        ORIGINAL_FILE_CONTENT,
        InputFile.Status.CHANGED);
      inputFileContext = new InputFileContext(sensorContext, changedFile);
      sensorContext.fileSystem().add(changedFile);
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        changedFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG))
        .doesNotContain("Skipping input file moduleKey:file1.slang (status is unchanged).")
        .contains("File moduleKey:file1.slang is considered changed: file status is CHANGED.");

      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_the_file_content_has_changed_but_input_file_status_is_SAME() {
      // Create a changed file
      InputFile changedFile = createInputFile(
        "file1.slang",
        "// This is definitely not the same thing\npackage main",
        InputFile.Status.SAME);
      sensorContext.fileSystem().add(changedFile);
      inputFileContext = new InputFileContext(sensorContext, changedFile);
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        changedFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG)).doesNotContain("Skipping input file moduleKey:file1.slang (status is unchanged).");

      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_the_file_content_is_unchanged_but_cache_is_disabled() {
      // Disable caching
      sensorContext.setCacheEnabled(false);
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG))
        .doesNotContain("Skipping input file moduleKey:file1.slang (status is unchanged).")
        .contains("File moduleKey:file1.slang is considered changed: hash cache is disabled.");

      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, never()).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_the_file_content_is_unchanged_but_no_hash_in_cache() {
      // Set an empty previous cache
      sensorContext.setPreviousCache(new DummyReadCache());
      // Execute analyzeFile
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG))
        .doesNotContain("Skipping input file moduleKey:file1.slang (status is unchanged).")
        .contains("File moduleKey:file1.slang is considered changed: hash could not be found in the cache.");

      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void does_not_skip_conversion_when_the_file_content_is_unchanged_but_failing_to_read_the_cache() {
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
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, never()).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      assertThat(logTester.logs(Level.DEBUG))
        .doesNotContain("Skipping input file moduleKey:file1.slang (status is unchanged).")
        .contains("File moduleKey:file1.slang is considered changed: failed to read hash from the cache.");

      verify(nextCache, never()).copyFromPrevious(hashKey);
      verify(nextCache, times(1)).write(eq(hashKey), any(byte[].class));
    }

    @Test
    void successful_visitor_is_not_called_to_visit_the_ast_after_conversion() {
      FailingToReuseVisitor failing = spy(new FailingToReuseVisitor());
      SlangSensor.analyseFile(
        converter,
        inputFileContext,
        inputFile,
        List.of(visitor, failing),
        new DurationStatistics(sensorContext.config()));
      verify(visitor, times(1)).reusePreviousResults(inputFileContext);
      verify(failing, times(1)).reusePreviousResults(inputFileContext);
      verify(converter, times(1)).parse(any());
      verify(visitor, never()).scan(eq(inputFileContext), any(Tree.class));
      verify(failing, times(1)).scan(eq(inputFileContext), any(Tree.class));
      assertThat(logTester.logs(Level.DEBUG)).doesNotContain(
        "Skipping input file moduleKey:file1.slang (status is unchanged).");
    }
  }

  @Override
  protected String repositoryKey() {
    return "slang";
  }

  @Override
  protected Language language() {
    return SlangLanguage.SLANG;
  }

  private SlangSensor sensor(CheckFactory checkFactory) {
    return sensor(SQ_LTS_RUNTIME, checkFactory);
  }

  private SlangSensor sensor(SonarRuntime sonarRuntime, CheckFactory checkFactory) {
    return new SlangSensor(sonarRuntime, new DefaultNoSonarFilter(), fileLinesContextFactory, SlangLanguage.SLANG) {
      @Override
      protected ASTConverter astConverter(SensorContext sensorContext) {
        return TestGoConverter.GO_CONVERTER;
      }

      @Override
      protected Checks<GoCheck> checks() {
        Checks<GoCheck> checks = checkFactory.create(repositoryKey());
        checks.addAnnotatedChecks(
          StringLiteralDuplicatedCheck.class,
          // TODO SONARGO-100 Fix NOSONAR suppression
          // new CommentedCodeCheck(new SlangCodeVerifier()),
          IdenticalBinaryOperandCheck.class);
        return checks;
      }

      @Override
      protected String repositoryKey() {
        return SlangSensorTest.this.repositoryKey();
      }
    };
  }

  enum SlangLanguage implements Language {
    SLANG;

    @Override
    public String getKey() {
      return "slang";
    }

    @Override
    public String getName() {
      return "SLang";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {".slang"};
    }
  }

  static class SuccessfulReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return true;
    }
  }

  static class FailingToReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return false;
    }
  }
}
