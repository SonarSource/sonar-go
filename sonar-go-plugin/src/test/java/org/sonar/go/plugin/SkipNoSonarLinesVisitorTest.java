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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.go.testing.TestGoConverterSingleFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SkipNoSonarLinesVisitorTest {

  private File tempFolder;

  private NoSonarFilter mockNoSonarFilter;
  private SkipNoSonarLinesVisitor visitor;

  @BeforeEach
  void setUp(@TempDir File tempFolder) {
    this.tempFolder = tempFolder;

    mockNoSonarFilter = mock(NoSonarFilter.class);
    visitor = new SkipNoSonarLinesVisitor(mockNoSonarFilter);
  }

  @Test
  void testNoDeclarations() throws Exception {
    testNosonarCommentLines("// NOSONAR comment\npackage main", Set.of(1));
  }

  @Test
  void testSingleNosonarComment() throws Exception {
    testNosonarCommentLines("""
      package main
      import "something"
      // NOSONAR comment
      func function1() { // comment
      x = true || false; }""",
      Set.of(3));
  }

  @Test
  void testBareNolintIsTreatedAsNosonar() throws IOException {
    testNosonarCommentLines("""
      package main
      func foo() {
      bar() //nolint
      }""",
      Set.of(3));
  }

  @Test
  void testNolintAllIsTreatedAsNosonar() throws IOException {
    testNosonarCommentLines("""
      package main
      func foo() {
      bar() //nolint:all
      }""",
      Set.of(3));
  }

  @Test
  void testScopedNolintIsTreatedAsNosonar() throws IOException {
    testNosonarCommentLines("""
      package main
      func foo() {
      bar() //nolint:gosec
      }""",
      Set.of(3));
  }

  @Test
  void testNolintWithLeadingSpaceIsTreatedAsNosonar() throws IOException {
    testNosonarCommentLines("""
      package main
      func foo() {
      bar() // nolint
      }""",
      Set.of(3));
  }

  @Test
  void testMultipleNosonarComments() throws IOException {
    testNosonarCommentLines("""
      /* File Header */
      package main
      import "something"
      func foo() { // NOSONAR
        // comment
      }

      func bar() {
        // nosonar
        foo();
      }""",
      Set.of(4, 9));
  }

  // Pins fixture lines tagged `FP` or `FN`: each must behave per its `Compliant` / `Noncompliant` verdict today.
  @ParameterizedTest
  @ValueSource(strings = {
    "src/test/resources/nolint/NoLintAllSuppression.go",
    "src/test/resources/nolint/NoLintGosecSuppression.go"
  })
  void nolintFixturePinsFalsePositiveAndFalseNegativeCases(String fixturePath) throws IOException {
    Pattern fpFnMarker = Pattern.compile("// (Compliant|Noncompliant) \\(S\\d+\\)\\s*(?:-|//)\\s*(?:FP|FN)\\b");
    String content = Files.readString(Path.of(fixturePath));
    Set<Integer> compliantFN = new HashSet<>();
    Set<Integer> noncompliantFP = new HashSet<>();
    String[] lines = content.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      Matcher m = fpFnMarker.matcher(lines[i]);
      if (m.find()) {
        ("Compliant".equals(m.group(1)) ? compliantFN : noncompliantFP).add(i + 1);
      }
    }
    assertThat(compliantFN.size() + noncompliantFP.size())
      .as("fixture %s should document at least one FP or FN case", fixturePath)
      .isPositive();

    InputFile inputFile = createInputFile(content);
    visitor.scan(createInputFileContext(inputFile), TestGoConverterSingleFile.parse(content));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<Integer>> captor = ArgumentCaptor.forClass(Set.class);
    verify(mockNoSonarFilter).noSonarInFile(eq(inputFile), captor.capture());

    assertThat(captor.getValue())
      .containsAll(compliantFN)
      .doesNotContainAnyElementsOf(noncompliantFP);
  }

  private void testNosonarCommentLines(String content, Set<Integer> expectedNosonarCommentLines) throws IOException {
    InputFile inputFile = createInputFile(content);

    visitor.scan(createInputFileContext(inputFile), TestGoConverterSingleFile.parse(content));

    verify(mockNoSonarFilter).noSonarInFile(inputFile, expectedNosonarCommentLines);
  }

  private InputFile createInputFile(String content) throws IOException {
    File file = File.createTempFile("file", ".tmp", tempFolder);
    return new TestInputFileBuilder("moduleKey", file.getName())
      .setContents(content)
      .build();
  }

  private InputFileContext createInputFileContext(InputFile inputFile) {
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder);
    return new InputFileContext(sensorContext, inputFile);
  }
}
