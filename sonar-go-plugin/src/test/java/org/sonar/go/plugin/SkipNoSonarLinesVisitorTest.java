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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.plugins.go.api.GoInputFile;

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

  private void testNosonarCommentLines(String content, Set<Integer> expectedNosonarCommentLines) throws IOException {
    var goInputFile = createInputFile(content);

    visitor.scan(createInputFileContext(goInputFile), TestGoConverterSingleFile.parse(content));

    verify(mockNoSonarFilter).noSonarInFile(goInputFile.getDelegate(), expectedNosonarCommentLines);
  }

  private GoInputFile createInputFile(String content) throws IOException {
    File file = File.createTempFile("file", ".tmp", tempFolder);
    var inputFile = new TestInputFileBuilder("moduleKey", file.getName())
      .setContents(content)
      .build();
    return new GoInputFile(inputFile);
  }

  private InputFileContext createInputFileContext(GoInputFile inputFile) {
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder);
    return new InputFileContext(sensorContext, inputFile);
  }
}
