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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.go.plugin.caching.HashCacheUtils;
import org.sonar.go.plugin.converter.ASTConverterValidation;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.ImportDeclarationTree;
import org.sonar.plugins.go.api.PackageDeclarationTree;
import org.sonar.plugins.go.api.ParseException;
import org.sonar.plugins.go.api.TextPointer;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeOrError;
import org.sonar.plugins.go.api.checks.GoModFileData;
import org.sonarsource.analyzer.commons.ProgressReport;

public abstract class SlangSensor implements Sensor {
  static final Predicate<Tree> EXECUTABLE_LINE_PREDICATE = t -> !(t instanceof PackageDeclarationTree)
    && !(t instanceof ImportDeclarationTree)
    && !(t instanceof ClassDeclarationTree)
    && !(t instanceof FunctionDeclarationTree)
    && !(t instanceof BlockTree);

  protected static final Pattern EMPTY_FILE_CONTENT_PATTERN = Pattern.compile("\\s*+");
  private static final Logger LOG = LoggerFactory.getLogger(SlangSensor.class);

  private final NoSonarFilter noSonarFilter;
  private final Language language;
  private final FileLinesContextFactory fileLinesContextFactory;

  protected DurationStatistics durationStatistics;

  protected SlangSensor(NoSonarFilter noSonarFilter, FileLinesContextFactory fileLinesContextFactory, Language language) {
    this.noSonarFilter = noSonarFilter;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.language = language;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(language.getKey())
      .name(language.getName() + " Sensor");
  }

  protected abstract ASTConverter astConverter(SensorContext sensorContext);

  protected abstract GoChecks checks();

  protected abstract String repositoryKey();

  protected Predicate<Tree> executableLineOfCodePredicate() {
    return EXECUTABLE_LINE_PREDICATE;
  }

  protected void beforeAnalyzeFile(SensorContext sensorContext, Map<String, List<InputFile>> inputFiles, GoModFileData goModFileData) {
    // the default implementation does nothing
  }

  private boolean analyseFiles(ASTConverter converter,
    SensorContext sensorContext,
    List<InputFile> inputFiles,
    ProgressReport progressReport,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics,
    GoModFileData goModFileData) {
    if (sensorContext.canSkipUnchangedFiles()) {
      LOG.info("The {} analyzer is running in a context where unchanged files can be skipped.", this.language);
    }

    var filesByDirectory = groupFilesByDirectory(inputFiles);

    beforeAnalyzeFile(sensorContext, filesByDirectory, goModFileData);

    for (List<InputFile> inputFilesInDir : filesByDirectory.values()) {
      if (sensorContext.isCancelled()) {
        return false;
      }

      var filesToAnalyse = inputFilesInDir.stream()
        .map(inputFile -> new InputFileContext(sensorContext, inputFile))
        .toList();

      try {
        analyseDirectory(converter, filesToAnalyse, visitors, statistics, sensorContext);
      } catch (ParseException e) {
        LOG.warn("Unable to parse file.", e);
        var inputFilePath = e.getInputFilePath();
        if (inputFilePath != null) {
          filesToAnalyse.stream()
            .filter(inputFileContext -> inputFileContext.inputFile.toString().equals(inputFilePath))
            .findFirst()
            .ifPresent(inputFileContext -> inputFileContext.reportAnalysisParseError(repositoryKey(), e.getMessage()));
          if (GoSensor.isFailFast(sensorContext)) {
            throw new IllegalStateException("Exception when analyzing '" + inputFilePath + "'", e);
          }
        }
      }
      progressReport.nextFile();
    }
    return true;
  }

  static Map<String, List<InputFile>> groupFilesByDirectory(List<InputFile> inputFiles) {
    return inputFiles.stream()
      .collect(Collectors.groupingBy((InputFile inputFile) -> {
        var path = inputFile.uri().getPath();
        int lastSeparatorIndex = path.lastIndexOf("/");
        if (lastSeparatorIndex == -1) {
          return "";
        }
        return path.substring(0, lastSeparatorIndex);
      }));
  }

  static void analyseDirectory(ASTConverter converter,
    List<InputFileContext> inputFileContextList,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics,
    SensorContext sensorContext) {

    Map<String, CacheEntry> filenameToCacheEntry = filterOutFilesFromCache(inputFileContextList, visitors);

    Map<String, String> filenameToContentMap = filenameToCacheEntry.values().stream()
      .map(SlangSensor::convertCacheEntryToFilenameAndContent)
      .filter(entry -> !EMPTY_FILE_CONTENT_PATTERN.matcher(entry.getValue()).matches())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (filenameToContentMap.isEmpty()) {
      return;
    }

    Map<String, TreeOrError> treeOrErrorMap = statistics.time("Parse", () -> converter.parse(filenameToContentMap));

    handleParsingErrors(sensorContext, treeOrErrorMap, filenameToCacheEntry);

    visitTrees(visitors, statistics, treeOrErrorMap, filenameToCacheEntry);
  }

  private static Map.Entry<String, String> convertCacheEntryToFilenameAndContent(CacheEntry cacheEntry) {
    String content;
    String fileName;
    try {
      content = cacheEntry.fileContext().inputFile.contents();
      fileName = cacheEntry.fileContext().inputFile.toString();
      return Map.entry(fileName, content);
    } catch (IOException | RuntimeException e) {
      throw toParseException("read", cacheEntry.fileContext().inputFile, e);
    }
  }

  private static void handleParsingErrors(SensorContext sensorContext, Map<String, TreeOrError> treeOrErrorMap, Map<String, CacheEntry> filenameToCacheResult) {
    var isAnyError = false;
    for (Map.Entry<String, TreeOrError> filenameToTree : treeOrErrorMap.entrySet()) {
      var treeOrError = filenameToTree.getValue();
      if (treeOrError.isError()) {
        isAnyError = true;
        String fileName = filenameToTree.getKey();
        LOG.warn("Unable to parse file: {}. {}", fileName, treeOrError.error());
        filenameToCacheResult.get(fileName).fileContext().reportAnalysisParseError(GoRulesDefinition.REPOSITORY_KEY, treeOrError.error());
      }
    }
    if (isAnyError && GoSensor.isFailFast(sensorContext)) {
      throw new IllegalStateException("Exception when analyzing files. See logs above for details.");
    }
  }

  private static Map<String, CacheEntry> filterOutFilesFromCache(List<InputFileContext> inputFileContexts, List<TreeVisitor<InputFileContext>> visitors) {
    Map<String, CacheEntry> result = new HashMap<>();
    for (InputFileContext inputFileContext : inputFileContexts) {
      if (fileCanBeSkipped(inputFileContext)) {
        String fileKey = inputFileContext.inputFile.key();
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
        result.put(inputFileContext.inputFile.toString(), new CacheEntry(inputFileContext, visitorsToSkip));
      } else {
        result.put(inputFileContext.inputFile.toString(), new CacheEntry(inputFileContext, List.of()));
      }
    }
    return result;
  }

  private static void visitTrees(List<TreeVisitor<InputFileContext>> visitors, DurationStatistics statistics, Map<String, TreeOrError> treeOrErrorMap,
    Map<String, CacheEntry> filenameToCacheEntry) {
    for (Map.Entry<String, TreeOrError> filenameToTree : treeOrErrorMap.entrySet()) {
      var treeOrError = filenameToTree.getValue();
      if (treeOrError.isTree()) {
        var filename = filenameToTree.getKey();
        var cacheResult = filenameToCacheEntry.get(filename);
        visitTree(visitors, statistics, cacheResult, treeOrError.tree());
      }
    }
  }

  private static void visitTree(List<TreeVisitor<InputFileContext>> visitors, DurationStatistics statistics, CacheEntry cacheResult, Tree tree) {
    var visitorsToSkip = cacheResult.visitorsToSkip();
    var inputFileContext = cacheResult.fileContext();

    for (TreeVisitor<InputFileContext> visitor : visitors) {
      try {
        if (visitorsToSkip.contains(visitor)) {
          continue;
        }
        String visitorId = visitor.getClass().getSimpleName();
        statistics.time(visitorId, () -> visitor.scan(inputFileContext, tree));
      } catch (RuntimeException e) {
        inputFileContext.reportAnalysisError(e.getMessage(), null);
        var message = "Cannot analyse '" + inputFileContext.inputFile + "': " + e.getMessage();
        LOG.warn(message, e);
      }
    }
    writeHashToCache(inputFileContext);
  }

  private static boolean fileCanBeSkipped(InputFileContext inputFileContext) {
    SensorContext sensorContext = inputFileContext.sensorContext;
    if (!sensorContext.canSkipUnchangedFiles()) {
      return false;
    }
    return HashCacheUtils.hasSameHashCached(inputFileContext);
  }

  private static void writeHashToCache(InputFileContext inputFileContext) {
    HashCacheUtils.writeHashForNextAnalysis(inputFileContext);
  }

  private static boolean reusePreviousResults(PullRequestAwareVisitor visitor, InputFileContext inputFileContext) {
    boolean success = visitor.reusePreviousResults(inputFileContext);
    if (success) {
      return true;
    }
    String message = String.format(
      "Visitor %s failed to reuse previous results for input file %s.",
      visitor.getClass().getSimpleName(),
      inputFileContext.inputFile.key());
    LOG.debug(message);
    return false;
  }

  protected static ParseException toParseException(String action, InputFile inputFile, Exception cause) {
    TextPointer position = cause instanceof ParseException actual ? actual.getPosition() : null;
    return new ParseException("Cannot " + action + " '" + inputFile + "': " + cause.getMessage(), position, cause, inputFile.toString());
  }

  @Override
  public void execute(SensorContext sensorContext) {
    initialize(sensorContext);

    FileSystem fileSystem = sensorContext.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasLanguage(language.getKey()),
      fileSystem.predicates().hasType(InputFile.Type.MAIN));
    List<InputFile> inputFiles = StreamSupport.stream(fileSystem.inputFiles(mainFilePredicate).spliterator(), false)
      .toList();
    List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    ProgressReport progressReport = new ProgressReport("Progress of the " + language.getName() + " analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean success = false;
    ASTConverter converter = ASTConverterValidation.wrap(astConverter(sensorContext), sensorContext.config());
    var goModFileData = new GoModFileAnalyzer(sensorContext).analyzeGoModFile();
    try {
      var visitors = visitors(sensorContext, durationStatistics, goModFileData);
      success = analyseFiles(converter, sensorContext, inputFiles, progressReport, visitors, durationStatistics, goModFileData);
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      converter.terminate();
    }

    processMetrics();
    cleanUp();
  }

  protected void initialize(SensorContext sensorContext) {
    durationStatistics = new DurationStatistics(sensorContext.config());
  }

  protected void processMetrics() {
    durationStatistics.log();
  }

  protected void cleanUp() {
    durationStatistics = null;
  }

  private List<TreeVisitor<InputFileContext>> visitors(SensorContext sensorContext, DurationStatistics statistics, GoModFileData goModFileData) {
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goModFileData));
    } else {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new MetricVisitor(fileLinesContextFactory, executableLineOfCodePredicate()),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goModFileData),
        new CpdVisitor(),
        new SyntaxHighlighter());
    }
  }

  record CacheEntry(InputFileContext fileContext, List<TreeVisitor<InputFileContext>> visitorsToSkip) {
  }
}
