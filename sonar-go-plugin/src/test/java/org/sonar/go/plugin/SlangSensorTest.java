/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
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
import org.sonar.go.checks.IdenticalBinaryOperandCheck;
import org.sonar.go.checks.StringLiteralDuplicatedCheck;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.plugin.caching.DummyReadCache;
import org.sonar.go.plugin.caching.DummyWriteCache;
import org.sonar.go.report.GoProgressReport;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.GoCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.go.testing.TextRangeAssert.assertThat;

class SlangSensorTest extends AbstractSensorTest {

  @Test
  void testOneRule() {
    InputFile inputFile = createInputFile("file1.go", """
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
    assertThat(location.message()).isEqualTo("Correct one of the identical sub-expressions on both sides of this operator.");
    assertThat(location.textRange()).hasRange(3, 14, 3, 15);
  }

  @Test
  void testRuleWithGap() {
    InputFile inputFile = createInputFile("file1.go", """
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
  void testCommentedCode() {
    InputFile inputFile = createInputFile("file1.go", """
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
  void testNosonarCommentedCode() {
    InputFile inputFile = createInputFile("file1.go", """
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
  void testSimpleFile() {
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

    assertThat(logTester.logs()).contains("1 folder (1 file) to be analyzed");
  }

  @Test
  void testSuppressIssuesInClass() {
    InputFile inputFile = createInputFile("file1.go", """
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
  void testSuppressIssuesInVar() {
    InputFile inputFile = createInputFile("file1.go", """
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
  void testFailInput() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.go", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.go");
    assertThat(analysisError.location()).isEqualTo(new DefaultTextPointer(1, 0));

    assertThat(logTester.logs()).isNotEmpty().anyMatch(l -> l.startsWith("Unable to parse directory '"));
  }

  @Test
  void testFailInputWithFailFast() throws IOException {
    context.settings().setProperty(GoSensor.FAIL_FAST_PROPERTY_NAME, true);
    InputFile inputFile = createInputFile("fakeFile.go", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    SlangSensor sensor = sensor(checkFactory);
    assertThatThrownBy(() -> sensor.execute(context))
      .hasMessage("Cannot read 'fakeFile.go': null")
      .isInstanceOf(ParseException.class);
  }

  @Test
  void testFailParsing() {
    InputFile inputFile = createInputFile("file1.go",
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
    assertThat(location.textRange()).hasRange(1, 0, 1, 10);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.go");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isEqualTo(new DefaultTextPointer(1, 2));

    assertThat(logTester.logs()).contains("Unable to parse file: file1.go. file1.go:1:2: expected 'package', found class");
  }

  @Test
  void testFailParsingShouldThrowAnExceptionWhenFailFastIsEnabled() {
    InputFile inputFile = createInputFile("file1.go",
      """
         class A {
         fun x() {}
         fun y() {}\
        """);
    context.settings().setProperty(GoSensor.FAIL_FAST_PROPERTY_NAME, true);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    var sensor = sensor(checkFactory);
    assertThatThrownBy(() -> sensor.execute(context))
      .hasMessage("Exception when analyzing files. See logs above for details.")
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testFailParsingShouldNotThrowAnExceptionWhenFailFastIsDisable() {
    InputFile inputFile = createInputFile("file1.go",
      """
         class A {
         fun x() {}
         fun y() {}\
        """);
    context.settings().setProperty(GoSensor.FAIL_FAST_PROPERTY_NAME, false);
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S2260");
    var sensor = sensor(checkFactory);
    sensor.execute(context);
    assertThat(logTester.logs(Level.WARN)).contains("Unable to parse file: file1.go. file1.go:1:2: expected 'package', found class");
  }

  @Test
  void shouldOnlyLogErrorWithHardFailureWithoutFailFast() {
    SlangSensor sensor = sensor(mock(CheckFactory.class));
    context.setFileSystem(null);
    sensor.execute(context);

    assertThat(logTester.logs(Level.ERROR)).contains("An error occurred during the analysis of the Go language:");
  }

  @Test
  void testFailParsingWithoutParsingErrorRuleActivated() {
    InputFile inputFile = createInputFile("file1.go", "{");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).hasSize(1);
  }

  @Test
  void testEmptyFile() {
    InputFile inputFile = createInputFile("empty.go", "\t\t  \r\n  \n ");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).isEmpty();
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void testFailureInCheck() {
    InputFile inputFile = createInputFile("file1.go", """
      package main
      func f() {}""");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = mock(CheckFactory.class);
    var checks = mock(Checks.class);
    GoCheck failingCheck = init -> init.register(TopLevelTree.class, (ctx, tree) -> {
      throw new IllegalStateException("BOUM");
    });
    when(checks.ruleKey(failingCheck)).thenReturn(RuleKey.of(GoRulesDefinition.REPOSITORY_KEY, "failing"));
    // The two following calls are called by "GoChecks".
    when(checkFactory.create(GoRulesDefinition.REPOSITORY_KEY)).thenReturn(checks);
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(failingCheck));
    sensor(checkFactory).execute(context);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(logTester.logs()).contains("Cannot analyse 'file1.go': BOUM");
  }

  @Test
  void testDescriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    SlangSensor sensor = sensor(mock(CheckFactory.class));
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.languages()).hasSize(1);
    assertThat(sensorDescriptor.languages()).containsExactly("go");
    assertThat(sensorDescriptor.name()).isEqualTo("GO Sensor");
  }

  @Test
  void testCancellation() {
    InputFile inputFile = createInputFile("file1.go",
      "fun main() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    context.setCancelled(true);
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void testSonarlintContext() {
    SonarRuntime sonarLintRuntime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9));
    SensorContextTester context = SensorContextTester.create(baseDir);
    InputFile inputFile = createInputFile("file1.go", """
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

    assertThat(logTester.logs()).contains("1 folder (1 file) to be analyzed");
  }

  @Test
  void testSensorDescriptorDoesNotProcessFilesIndependently() {
    final SlangSensor sensor = sensor(checkFactory());
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.isProcessesFilesIndependently()).isFalse();
  }

  @Test
  void testSensorLogsWhenUnchangedFilesCanBeSkipped() {
    // Enable PR context
    SensorContextTester sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setCanSkipUnchangedFiles(true);
    sensorContext.setCacheEnabled(true);
    // Execute sensor
    SlangSensor sensor = sensor(checkFactory());
    sensor.execute(sensorContext);
    assertThat(logTester.logs(Level.INFO)).contains(
      "The GO analyzer is running in a context where unchanged files can be skipped.");
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
    private List<GoFolder> goFolders;
    private GoConverter converter;
    private PullRequestAwareVisitor visitor;
    private String hashKey;
    private GoProgressReport goProgressReport;

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
        "file1.go",
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

      converter = spy(TestGoConverterSingleFile.GO_CONVERTER);
      visitor = spy(new SuccessfulReuseVisitor());
      goProgressReport = new GoProgressReport("Analysis progress", TimeUnit.SECONDS.toMillis(10));
      goFolders = List.of(new GoFolder("myFolder", List.of(inputFile)));
    }

    @Test
    void shouldSkipsConversionForUnchangedFileWithCachedResults() {
      // Execute analyzeFile
      goProgressReport.start(goFolders);
      SlangSensor.analyseDirectory(
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
      visitor = spy(new FailingToReuseVisitor());
      // Execute analyzeFile
      goProgressReport.start(goFolders);
      SlangSensor.analyseDirectory(
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
      SlangSensor.analyseDirectory(
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
        InputFile.Status.CHANGED);
      inputFileContext = new InputFileContext(sensorContext, changedFile);
      sensorContext.fileSystem().add(changedFile);
      goProgressReport.start(goFolders);
      // Execute analyzeFile
      SlangSensor.analyseDirectory(
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
        InputFile.Status.SAME);
      sensorContext.fileSystem().add(changedFile);
      inputFileContext = new InputFileContext(sensorContext, changedFile);
      // Execute analyzeFile
      goProgressReport.start(goFolders);
      SlangSensor.analyseDirectory(
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
      SlangSensor.analyseDirectory(
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
      SlangSensor.analyseDirectory(
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
      SlangSensor.analyseDirectory(
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
      FailingToReuseVisitor failing = spy(new FailingToReuseVisitor());
      goProgressReport.start(goFolders);
      SlangSensor.analyseDirectory(
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
  }

  @Test
  void shouldGroupFilesByDirectory() {
    InputFile file1 = mockInputFile("dir1/file1.go");
    InputFile file2 = mockInputFile("dir1/file2.go");
    InputFile file3 = mockInputFile("dir2/file3.go");

    var goFolders = SlangSensor.groupFilesByDirectory(List.of(file1, file2, file3));

    assertThat(goFolders).containsExactlyInAnyOrder(
      new GoFolder(new File("dir1").toURI().getPath(), List.of(file1, file2)),
      new GoFolder(new File("dir2").toURI().getPath(), List.of(file3)));
  }

  @Test
  void shouldSkipExecutionIfGoConverterNotInitialized() {
    var sensor = new SlangSensor(new DefaultNoSonarFilter(), fileLinesContextFactory, GoLanguage.GO) {
      @Override
      protected ASTConverter astConverter() {
        var mock = mock(GoConverter.class);
        when(mock.isInitialized()).thenReturn(false);
        return mock;
      }

      @Override
      protected GoChecks checks() {
        return new GoChecks(mock(CheckFactory.class));
      }

      @Override
      protected String repositoryKey() {
        return GoRulesDefinition.REPOSITORY_KEY;
      }
    };

    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(13, 0)));
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(Level.INFO)).contains("Skipping the Go analysis, parsing is not possible with uninitialized Go converter.");
  }

  InputFile mockInputFile(String path) {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.uri()).thenReturn(new File(path).toURI());
    return inputFile;
  }

  private SlangSensor sensor(CheckFactory checkFactory) {
    return new SlangSensor(new DefaultNoSonarFilter(), fileLinesContextFactory, GoLanguage.GO) {
      @Override
      protected ASTConverter astConverter() {
        return TestGoConverterSingleFile.GO_CONVERTER;
      }

      @Override
      protected GoChecks checks() {
        GoChecks checks = new GoChecks(checkFactory);
        checks.addChecks(repositoryKey(), List.of(
          StringLiteralDuplicatedCheck.class,
          // TODO SONARGO-100 Fix NOSONAR suppression
          // new CommentedCodeCheck(new SlangCodeVerifier()),
          IdenticalBinaryOperandCheck.class));
        return checks;
      }

      @Override
      protected String repositoryKey() {
        return GoRulesDefinition.REPOSITORY_KEY;
      }
    };
  }

  enum GoLanguage implements Language {
    GO;

    @Override
    public String getKey() {
      return "go";
    }

    @Override
    public String getName() {
      return "GO";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {".go"};
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
