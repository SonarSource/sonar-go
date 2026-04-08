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
package org.sonar.go.plugin.caching;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.go.plugin.InputFileContext;
import org.sonar.go.plugin.PullRequestAwareVisitor;
import org.sonar.go.plugin.caching.CacheHandler.CacheEntry;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CacheHandlerTest {

  private static final String MODULE_KEY = "moduleKey";
  private static final String FILE_CONTENT = "package main\nfunc main() {}";

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private SensorContextTester sensorContext;
  private InputFile inputFile;
  private InputFileContext inputFileContext;

  @BeforeEach
  void setup(@TempDir File tmpBaseDir) throws NoSuchAlgorithmException {
    sensorContext = SensorContextTester.create(tmpBaseDir);
    sensorContext.setCanSkipUnchangedFiles(true);
    sensorContext.setCacheEnabled(true);

    inputFile = new TestInputFileBuilder(MODULE_KEY, "file1.go")
      .setModuleBaseDir(tmpBaseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage("go")
      .setCharset(StandardCharsets.UTF_8)
      .setContents(FILE_CONTENT)
      .setStatus(InputFile.Status.SAME)
      .build();
    sensorContext.fileSystem().add(inputFile);
    inputFileContext = new InputFileContext(sensorContext, inputFile);

    // Set up caches with hash
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    byte[] md5Hash = md5.digest(FILE_CONTENT.getBytes(StandardCharsets.UTF_8));
    String hashKey = "slang:hash:" + inputFile.key();
    DummyReadCache previousCache = new DummyReadCache();
    previousCache.persisted.put(hashKey, md5Hash);
    sensorContext.setPreviousCache(previousCache);

    DummyWriteCache nextCache = new DummyWriteCache();
    nextCache.bind(previousCache);
    sensorContext.setNextCache(nextCache);
  }

  @Test
  void shouldSkipsFileWhenAllVisitorsReuseSuccessfully() {
    var visitor = spy(new SuccessfulReuseVisitor());
    List<TreeVisitor<InputFileContext>> visitors = List.of(visitor);

    Map<String, CacheEntry> result = CacheHandler.filterOutFilesFromCache(List.of(inputFileContext), visitors);

    assertThat(result).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Checking that previous results can be reused for input file moduleKey:file1.go.",
      "Skipping input file moduleKey:file1.go (status is unchanged).");
  }

  @Test
  void shouldIncludesFileWhenVisitorFailsToReuse() {
    var visitor = spy(new FailingToReuseVisitor());
    List<TreeVisitor<InputFileContext>> visitors = List.of(visitor);

    Map<String, CacheEntry> result = CacheHandler.filterOutFilesFromCache(List.of(inputFileContext), visitors);

    assertThat(result).hasSize(1);
    CacheEntry entry = result.values().iterator().next();
    assertThat(entry.fileContext()).isEqualTo(inputFileContext);
    assertThat(entry.visitorsToSkip()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains(
      "Checking that previous results can be reused for input file moduleKey:file1.go.",
      "Visitor FailingToReuseVisitor failed to reuse previous results for input file moduleKey:file1.go.",
      "Will convert input file moduleKey:file1.go for full analysis.");
  }

  @Test
  void shouldIncludesFileWithEmptyVisitorsToSkipWhenCannotSkipUnchangedFiles() {
    sensorContext.setCanSkipUnchangedFiles(false);
    var visitor = spy(new SuccessfulReuseVisitor());
    List<TreeVisitor<InputFileContext>> visitors = List.of(visitor);

    Map<String, CacheEntry> result = CacheHandler.filterOutFilesFromCache(List.of(inputFileContext), visitors);

    assertThat(result).hasSize(1);
    CacheEntry entry = result.values().iterator().next();
    assertThat(entry.visitorsToSkip()).isEmpty();
  }

  @Test
  void shouldPartialReuseRecordsSuccessfulVisitorAsSkippable() {
    var successful = spy(new SuccessfulReuseVisitor());
    var failing = spy(new FailingToReuseVisitor());
    List<TreeVisitor<InputFileContext>> visitors = List.of(successful, failing);

    Map<String, CacheEntry> result = CacheHandler.filterOutFilesFromCache(List.of(inputFileContext), visitors);

    assertThat(result).hasSize(1);
    CacheEntry entry = result.values().iterator().next();
    assertThat(entry.visitorsToSkip()).containsExactly(successful);
  }

  @Test
  void shouldIncludeNonPullRequestAwareVisitorsWithoutSkipping() {
    TreeVisitor<InputFileContext> regularVisitor = new TreeVisitor<>() {
    };
    List<TreeVisitor<InputFileContext>> visitors = List.of(regularVisitor);

    Map<String, CacheEntry> result = CacheHandler.filterOutFilesFromCache(List.of(inputFileContext), visitors);

    // File has matching hash, no PullRequestAwareVisitors to check, so all "succeed" -> file skipped
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnFilenameAndContent() {
    CacheEntry cacheEntry = new CacheEntry(inputFileContext, List.of());

    Map.Entry<String, String> result = CacheHandler.convertCacheEntryToFilenameAndContent(cacheEntry);

    assertThat(result.getKey()).isEqualTo(inputFile.toString());
    assertThat(result.getValue()).isEqualTo(FILE_CONTENT);
  }

  @Test
  void shouldThrowParseExceptionOnIOError() throws IOException {
    InputFile failingFile = mock(InputFile.class);
    when(failingFile.toString()).thenReturn("failing.go");
    when(failingFile.contents()).thenThrow(new IOException("disk error"));
    InputFileContext failingContext = new InputFileContext(sensorContext, failingFile);
    CacheEntry cacheEntry = new CacheEntry(failingContext, List.of());

    assertThatThrownBy(() -> CacheHandler.convertCacheEntryToFilenameAndContent(cacheEntry))
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Cannot read")
      .hasMessageContaining("failing.go");
  }

  @Test
  void shouldWritesHash() {
    CacheHandler.writeHashToCache(inputFileContext);

    String hashKey = "slang:hash:" + inputFile.key();
    DummyWriteCache nextCache = (DummyWriteCache) sensorContext.nextCache();
    assertThat(nextCache.persisted).containsKey(hashKey);
  }

  static class FailingToReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return false;
    }
  }

  private static class SuccessfulReuseVisitor extends PullRequestAwareVisitor {
    @Override
    public boolean reusePreviousResults(InputFileContext unused) {
      return true;
    }
  }
}
