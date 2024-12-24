/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonar.go.plugin.caching;

import java.io.File;
import java.nio.charset.StandardCharsets;
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

import static org.assertj.core.api.Assertions.assertThat;

class HashCacheUtilsTest {
  private static final String CONTENTS = "// Hello, world!";
  private static final String EXPECTED_HASH = "180dd7ee70f338197b90e0635cad1131";
  private static final String MODULE_KEY = "moduleKey";
  private static final String FILENAME = "file1.slang";
  private static final String CACHE_KEY = "slang:hash:%s:%s".formatted(MODULE_KEY, FILENAME);

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private SensorContextTester sensorContext;
  private DummyReadCache previousCache;
  private DummyWriteCache nextCache;
  private InputFileContext inputFileContext;

  @BeforeEach
  void setup(@TempDir File tmpBaseDir) {
    sensorContext = SensorContextTester.create(tmpBaseDir);
    previousCache = new DummyReadCache();
    nextCache = new DummyWriteCache();
    nextCache.bind(previousCache);

    sensorContext.setCacheEnabled(true);
    sensorContext.setPreviousCache(previousCache);
    sensorContext.setNextCache(nextCache);

    InputFile inputFile = new TestInputFileBuilder(MODULE_KEY, FILENAME)
      .setModuleBaseDir(tmpBaseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage("slang")
      .setCharset(StandardCharsets.UTF_8)
      .setContents(CONTENTS)
      .build();
    previousCache.persisted.put(CACHE_KEY, EXPECTED_HASH.getBytes(StandardCharsets.UTF_8));

    sensorContext.fileSystem().add(inputFile);
    inputFileContext = new InputFileContext(sensorContext, inputFile);
  }

  @Test
  void copyFromPrevious_fails_to_copy_when_called_a_second_time() {
    // Succeed on first try
    assertThat(HashCacheUtils.copyFromPrevious(inputFileContext)).isTrue();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(nextCache.persisted)
      .containsOnlyKeys("slang:hash:moduleKey:file1.slang");

    // Fail on second try
    assertThat(HashCacheUtils.copyFromPrevious(inputFileContext)).isFalse();
    assertThat(logTester.logs(Level.WARN))
      .containsOnly("Failed to copy hash from previous analysis for moduleKey:file1.slang.");
    assertThat(nextCache.persisted)
      .containsOnlyKeys("slang:hash:moduleKey:file1.slang");
  }

  @Test
  void copyFromPrevious_fails_to_copy_when_entry_does_not_exist_in_previous_cache() {
    // Set an empty previous cache and try to copy from it
    previousCache = new DummyReadCache();
    nextCache = new DummyWriteCache();
    nextCache.bind(previousCache);
    sensorContext.setPreviousCache(previousCache);
    sensorContext.setNextCache(nextCache);

    // Try and fail to copy
    assertThat(HashCacheUtils.copyFromPrevious(inputFileContext)).isFalse();
    assertThat(nextCache.persisted).isEmpty();
    assertThat(logTester.logs(Level.WARN))
      .containsOnly("Failed to copy hash from previous analysis for moduleKey:file1.slang.");
  }

  @Test
  void copyFromPrevious_does_not_attempt_to_copy_when_the_cache_is_disabled() {
    // Disable caching
    sensorContext.setCacheEnabled(false);

    // Try and fail to copy
    assertThat(HashCacheUtils.copyFromPrevious(inputFileContext)).isFalse();
    assertThat(nextCache.persisted).isEmpty();
  }

  @Test
  void writeHashForNextAnalysis_writes_the_md5_sum_of_the_file_to_the_cache() {
    assertThat(HashCacheUtils.writeHashForNextAnalysis(inputFileContext)).isTrue();

    assertThat(nextCache.persisted).containsOnlyKeys("slang:hash:moduleKey:file1.slang");
    byte[] written = nextCache.persisted.get("slang:hash:moduleKey:file1.slang");
    assertThat(written)
      .as("Hash should be written in hexadecimal form.")
      .hasSize(32);
    String actual = new String(written, StandardCharsets.UTF_8);
    assertThat(actual).isEqualTo(EXPECTED_HASH);
  }

  @Test
  void writeHashForNextAnalysis_fails_to_write_the_hash_of_the_same_file_a_second_time_to() {
    // Succeed on first attempt
    assertThat(HashCacheUtils.writeHashForNextAnalysis(inputFileContext)).isTrue();
    assertThat(nextCache.persisted).containsOnlyKeys("slang:hash:moduleKey:file1.slang");

    // Fail on second attempt
    assertThat(HashCacheUtils.writeHashForNextAnalysis(inputFileContext)).isFalse();
    assertThat(nextCache.persisted).containsOnlyKeys("slang:hash:moduleKey:file1.slang");
    assertThat(logTester.logs(Level.WARN))
      .containsOnly("Failed to write hash for moduleKey:file1.slang to cache.");
  }

  @Test
  void writeHashForNextAnalysis_fails_when_the_cache_is_disabled() {
    // Disable the cache
    sensorContext.setCacheEnabled(false);

    assertThat(HashCacheUtils.writeHashForNextAnalysis(inputFileContext)).isFalse();
    assertThat(nextCache.persisted).isEmpty();
  }
}
