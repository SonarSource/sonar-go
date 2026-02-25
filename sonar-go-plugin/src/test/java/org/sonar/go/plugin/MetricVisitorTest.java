/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricVisitorTest {

  private File tempFolder;
  private MetricVisitor visitor;
  private SensorContextTester sensorContext;
  private DefaultInputFile inputFile;

  @BeforeEach
  void setUp(@TempDir File tempFolder) {
    this.tempFolder = tempFolder;
    sensorContext = SensorContextTester.create(tempFolder);
    FileLinesContext mockFileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory mockFileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(mockFileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(mockFileLinesContext);
    visitor = new MetricVisitor(mockFileLinesContextFactory, SlangSensor.EXECUTABLE_LINE_PREDICATE);
  }

  @Test
  @Disabled("SONARGO-99 Unable to parse empty file or with comments only")
  void emptySource() throws Exception {
    scan("");
    assertThat(visitor.linesOfCode()).isEmpty();
    assertThat(visitor.commentLines()).isEmpty();
    assertThat(visitor.numberOfFunctions()).isZero();
  }

  @Test
  void linesOfCode() throws Exception {
    scan("""
      package main
      func main() {
        x + 1;
      }
      // comment
      func function1() { // comment
        x = true || false;
      }""");
    assertThat(visitor.linesOfCode()).containsExactly(1, 2, 3, 4, 6, 7, 8);
  }

  @Test
  void commentLines() throws Exception {
    scan("""
      package main
      func foo() {
        x + 1;
        // comment
      }
      func function1() { // comment
        x = true || false;
      }""");
    assertThat(visitor.commentLines()).containsExactly(4, 6);
  }

  @Test
  void commentBeforeTheFirstTokenCorrespondToTheIgnoredHeader() throws Exception {
    scan("""
      // first line of the header
      // second line of the header
      /*
        this is also part of the header
      */
      package abc; // comment 1
      import "x";

      func function1() { // comment 2
        //
        /**/
      }""");
    assertThat(visitor.commentLines()).containsExactly(1, 2, 4, 6, 9);
  }

  @Test
  @Disabled("SONARGO-99 Unable to parse empty file or with comments only")
  void commentsWithoutDeclarationsAreIgnored() throws Exception {
    scan("""
      // header 1
      /**
       * header 2
       */""");
    assertThat(visitor.commentLines()).isEmpty();
  }

  @Test
  void noSonarCommentsDoNotAccountForTheCommentMetrics() throws Exception {
    scan("""
      package main
      func function1() {
        // comment1
        // NOSONAR comment2
        // comment3
      }""");
    assertThat(visitor.commentLines()).containsExactly(3, 5);
  }

  @Test
  void emptyLinesDoNotAccountForTheCommentMetrics() throws Exception {
    scan("""
      package abc // comment 1
      /*

        comment 2

        comment 3

      */

      func function1() { // comment 4
        /**
         *
         #
         =
         -
         |
         | comment 5
         | どのように
         |
         */
      }""");
    assertThat(visitor.commentLines()).containsExactlyInAnyOrder(1, 4, 6, 10, 17, 18);
  }

  @Test
  void multiLineComment() throws Exception {
    scan("""
      /*start
      x + 1
      end*/
      package main""");
    assertThat(visitor.commentLines()).containsExactly(1, 2, 3);
    assertThat(visitor.linesOfCode()).containsExactly(4);
  }

  @Test
  void functions() throws Exception {
    scan("""
      package main
      type A struct {
        x int
      }""");
    assertThat(visitor.numberOfFunctions()).isZero();

    scan("""
      package main
      // Only functions with implementation bodies are considered for the metric
      func noBodyFunction()
      // It counts
      func main() {
        // Anonymous functions are not considered for function metric computation
        func() {
          x = 1;
        }
      }
      func function1() { // comment
        x = true || false;
      }""");
    assertThat(visitor.numberOfFunctions()).isEqualTo(2);
  }

  @Test
  void classes() throws Exception {
    scan("""
      package main
      func noBodyFunction()""");
    assertThat(visitor.numberOfClasses()).isZero();

    scan("""
      package main
      type C struct {}
      func function() {}
      type D struct { x int }
      type E struct {
      }""");
    assertThat(visitor.numberOfClasses()).isEqualTo(3);
  }

  @Test
  void cognitiveComplexity() throws Exception {
    // +1 for 'if'
    // +1 for 'if'
    // + 2 for nested 'if'
    // +1 for match
    // +2 for nested 'if'
    scan("""
      package main
      func function() int {
        if 1 != 1 {             // +1
          if 1 != 1 {           // +2 (nested)
            return 1
          }
        }
        bar := func(a int) {
          switch a {            // +2 (nested)
            case 1:
              doSomething()
            case 2:
              doSomething()
            default:
              if 1 != 1 {       // +3 (double nested)
                doSomething()
              }
          }
        }
        bar(1)
        return 0
      }
      """);
    assertThat(visitor.cognitiveComplexity()).isEqualTo(8);
  }

  @Test
  void executable_lines() throws Exception {
    scan("""
      package abc
      import "x"

      func foo() {
        statementOnSeveralLines(a,
          b)
      }

      func bar() {
        x = 42
      }""");
    assertThat(visitor.executableLines()).containsExactly(5, 10);
  }

  @Test
  void metricsShouldIgnoreLineDirective() throws IOException {
    scan("""
      package p

      // Use a different line number for each token
      var _ = struct{}{ /*line :6:1*/foo /*line :7:1*/: /*line :8:1*/0 }

      // ERROR "unknown field foo"
      """);

    assertThat(visitor.linesOfCode()).containsExactly(1, 4);
    assertThat(visitor.commentLines()).containsExactly(3, 4, 6);
    assertThat(visitor.executableLines()).containsExactly(4);
  }

  private void scan(String code) throws IOException {
    File tmpFile = File.createTempFile("file", ".tmp", tempFolder);
    inputFile = new TestInputFileBuilder("moduleKey", tmpFile.getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    Tree root = TestGoConverterSingleFile.parse(code);
    visitor.scan(ctx, root);
  }
}
