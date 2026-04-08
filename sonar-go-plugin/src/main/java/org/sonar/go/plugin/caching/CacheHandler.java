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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.go.plugin.GoSensor;
import org.sonar.go.plugin.InputFileContext;
import org.sonar.go.plugin.PullRequestAwareVisitor;
import org.sonar.go.visitors.TreeVisitor;

public class CacheHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CacheHandler.class);

  private CacheHandler() {
    // utility class
  }

  public static Map<String, CacheEntry> filterOutFilesFromCache(List<InputFileContext> inputFileContexts, List<TreeVisitor<InputFileContext>> visitors) {
    Map<String, CacheEntry> result = new HashMap<>();
    for (InputFileContext inputFileContext : inputFileContexts) {
      if (fileCanBeSkipped(inputFileContext)) {
        String fileKey = inputFileContext.inputFile().key();
        LOG.debug("Checking that previous results can be reused for input file {}.", fileKey);

        Map<PullRequestAwareVisitor, Boolean> successfulCacheReuseByVisitor = visitors.stream()
          .filter(PullRequestAwareVisitor.class::isInstance)
          .map(PullRequestAwareVisitor.class::cast)
          .collect(Collectors.toMap(visitor -> visitor, visitor -> reusePreviousResults(visitor, inputFileContext)));

        boolean allVisitorsSuccessful = successfulCacheReuseByVisitor.values().stream().allMatch(Boolean.TRUE::equals);
        if (allVisitorsSuccessful) {
          LOG.debug("Skipping input file {} (status is unchanged).", fileKey);
          HashCacheUtils.copyFromPrevious(inputFileContext);
          // The file can be skipped completely
          continue;
        }
        LOG.debug("Will convert input file {} for full analysis.", fileKey);
        var visitorsToSkip = successfulCacheReuseByVisitor.entrySet().stream()
          .filter(Map.Entry::getValue)
          .map(Map.Entry::getKey)
          .map(visitor -> (TreeVisitor<InputFileContext>) visitor)
          .toList();
        result.put(inputFileContext.inputFile().toString(), new CacheEntry(inputFileContext, visitorsToSkip));
      } else {
        result.put(inputFileContext.inputFile().toString(), new CacheEntry(inputFileContext, List.of()));
      }
    }
    return result;
  }

  private static boolean fileCanBeSkipped(InputFileContext inputFileContext) {
    SensorContext sensorContext = inputFileContext.sensorContext;
    if (!sensorContext.canSkipUnchangedFiles()) {
      return false;
    }
    return HashCacheUtils.hasSameHashCached(inputFileContext);
  }

  public static Map.Entry<String, String> convertCacheEntryToFilenameAndContent(CacheEntry cacheEntry) {
    String content;
    String fileName;
    try {
      fileName = cacheEntry.fileContext().inputFile().toString();
      content = cacheEntry.fileContext().inputFile().contents();
      return Map.entry(fileName, content);
    } catch (IOException | RuntimeException e) {
      throw GoSensor.toParseException("read", cacheEntry.fileContext().inputFile(), e);
    }
  }

  private static boolean reusePreviousResults(PullRequestAwareVisitor visitor, InputFileContext inputFileContext) {
    boolean success = visitor.reusePreviousResults(inputFileContext);
    if (success) {
      return true;
    }
    String message = String.format(
      "Visitor %s failed to reuse previous results for input file %s.",
      visitor.getClass().getSimpleName(),
      inputFileContext.inputFile().key());
    LOG.debug(message);
    return false;
  }

  public static void writeHashToCache(InputFileContext inputFileContext) {
    HashCacheUtils.writeHashForNextAnalysis(inputFileContext);
  }

  public record CacheEntry(InputFileContext fileContext, List<TreeVisitor<InputFileContext>> visitorsToSkip) {
  }
}
