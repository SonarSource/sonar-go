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
import java.util.ArrayList;
import java.util.Arrays;
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
import org.sonar.plugins.go.api.checks.GoVersion;
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
  private FileLinesContextFactory fileLinesContextFactory;

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

  protected void beforeAnalyzeFile(SensorContext sensorContext, List<InputFile> inputFiles) {
    // the default implementation does nothing
  }

  private boolean analyseFiles(ASTConverter converter,
    SensorContext sensorContext,
    List<InputFile> inputFiles,
    ProgressReport progressReport,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics) {
    if (sensorContext.canSkipUnchangedFiles()) {
      LOG.info("The {} analyzer is running in a context where unchanged files can be skipped.", this.language);
    }

    beforeAnalyzeFile(sensorContext, inputFiles);

    // TODO SONARGO-618 Parse files in batches, Java part
    for (InputFile inputFile : inputFiles) {
      if (sensorContext.isCancelled()) {
        return false;
      }
      InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
      try {
        analyseFile(converter, inputFileContext, inputFile, visitors, statistics);
      } catch (ParseException e) {
        logParsingError(inputFile, e);
        inputFileContext.reportAnalysisParseError(repositoryKey(), inputFile, e.getPosition());
      }
      progressReport.nextFile();
    }
    return true;
  }

  static void analyseFile(ASTConverter converter,
    InputFileContext inputFileContext,
    InputFile inputFile,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics) {
    List<TreeVisitor<InputFileContext>> canBeSkipped = new ArrayList<>();
    if (fileCanBeSkipped(inputFileContext)) {
      String fileKey = inputFile.key();
      LOG.debug("Checking that previous results can be reused for input file {}.", fileKey);

      Map<PullRequestAwareVisitor, Boolean> successfulCacheReuseByVisitor = visitors.stream()
        .filter(PullRequestAwareVisitor.class::isInstance)
        .map(PullRequestAwareVisitor.class::cast)
        .collect(Collectors.toMap(visitor -> visitor, visitor -> reusePreviousResults(visitor, inputFileContext)));

      boolean allVisitorsSuccessful = successfulCacheReuseByVisitor.values().stream().allMatch(Boolean.TRUE::equals);
      if (allVisitorsSuccessful) {
        LOG.debug("Skipping input file {} (status is unchanged).", fileKey);
        HashCacheUtils.copyFromPrevious(inputFileContext);
        return;
      }
      LOG.debug("Will convert input file {} for full analysis.", fileKey);
      successfulCacheReuseByVisitor.entrySet().stream()
        .filter(Map.Entry::getValue)
        .map(Map.Entry::getKey)
        .forEach(canBeSkipped::add);
    }
    String content;
    String fileName;
    try {
      content = inputFile.contents();
      fileName = inputFile.toString();
    } catch (IOException | RuntimeException e) {
      throw toParseException("read", inputFile, e);
    }

    if (EMPTY_FILE_CONTENT_PATTERN.matcher(content).matches()) {
      return;
    }

    Map<String, Tree> trees = statistics.time("Parse", () -> {
      try {
        return converter.parse(content, fileName);
      } catch (RuntimeException e) {
        throw toParseException("parse", inputFile, e);
      }
    });
    for (TreeVisitor<InputFileContext> visitor : visitors) {
      try {
        if (canBeSkipped.contains(visitor)) {
          continue;
        }
        String visitorId = visitor.getClass().getSimpleName();
        trees.values().forEach(tree -> statistics.time(visitorId, () -> visitor.scan(inputFileContext, tree)));
      } catch (RuntimeException e) {
        inputFileContext.reportAnalysisError(e.getMessage(), null);
        LOG.warn("Cannot analyse '" + inputFile + "': " + e.getMessage(), e);
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
    return new ParseException("Cannot " + action + " '" + inputFile + "': " + cause.getMessage(), position, cause);
  }

  private static void logParsingError(InputFile inputFile, ParseException e) {
    TextPointer position = e.getPosition();
    String positionMessage = "";
    if (position != null) {
      positionMessage = String.format("Parse error at position %s:%s", position.line(), position.lineOffset());
    }
    LOG.warn("Unable to parse file: {}. {}", inputFile.uri(), positionMessage);
    LOG.warn(e.getMessage());
  }

  @Override
  public void execute(SensorContext sensorContext) {
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    FileSystem fileSystem = sensorContext.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasLanguage(language.getKey()),
      fileSystem.predicates().hasType(InputFile.Type.MAIN));
    var goVersion = new GoVersionAnalyzer(sensorContext).analyzeGoVersion();
    List<InputFile> inputFiles = StreamSupport.stream(fileSystem.inputFiles(mainFilePredicate).spliterator(), false)
      .toList();
    List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    ProgressReport progressReport = new ProgressReport("Progress of the " + language.getName() + " analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean success = false;
    ASTConverter converter = ASTConverterValidation.wrap(astConverter(sensorContext), sensorContext.config());
    try {
      success = analyseFiles(converter, sensorContext, inputFiles, progressReport, visitors(sensorContext, statistics, goVersion), statistics);
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      converter.terminate();
    }
    statistics.log();
  }

  private List<TreeVisitor<InputFileContext>> visitors(SensorContext sensorContext, DurationStatistics statistics, GoVersion goVersion) {
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goVersion));
    } else {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new MetricVisitor(fileLinesContextFactory, executableLineOfCodePredicate()),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goVersion),
        new CpdVisitor(),
        new SyntaxHighlighter());
    }
  }
}
