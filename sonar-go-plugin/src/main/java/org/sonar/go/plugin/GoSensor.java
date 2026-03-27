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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.go.checks.GoCheckList;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.plugin.caching.CacheHandler;
import org.sonar.go.plugin.converter.ASTConverterValidation;
import org.sonar.go.report.GoProgressReport;
import org.sonar.go.utils.NativeKinds;
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
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.GoVersion;

public class GoSensor implements Sensor {

  protected static final Pattern EMPTY_FILE_CONTENT_PATTERN = Pattern.compile("\\s*+");
  protected static final Predicate<Tree> EXECUTABLE_LINE_PREDICATE = t -> !(t instanceof PackageDeclarationTree)
    && !(t instanceof ImportDeclarationTree)
    && !(t instanceof ClassDeclarationTree)
    && !(t instanceof FunctionDeclarationTree)
    && !(t instanceof BlockTree);

  private static final Logger LOG = LoggerFactory.getLogger(GoSensor.class);
  private static final int PROGRESS_REPORT_INTERVAL_SECOND = 10;
  private static final String FAIL_FAST_PROPERTY_NAME = "sonar.internal.analysis.failFast";
  private static final String DEBUG_TYPE_CHECK_PROPERTY_NAME = "sonar.go.internal.debugTypeCheck";
  private final NoSonarFilter noSonarFilter;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final Language language;
  private final GoChecks checks;
  private final ASTConverter goConverter;
  private final InputFileDiscovery inputFileDiscovery;

  protected DurationStatistics durationStatistics;
  protected MemoryMonitor memoryMonitor;

  public GoSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory,
    NoSonarFilter noSonarFilter, GoLanguage language, GoConverter goConverter) {
    this(initializeChecks(checkFactory), fileLinesContextFactory, noSonarFilter, language, goConverter);
  }

  public GoSensor(GoChecks checks, FileLinesContextFactory fileLinesContextFactory,
    NoSonarFilter noSonarFilter, GoLanguage language, GoConverter goConverter) {
    this.noSonarFilter = noSonarFilter;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.language = language;
    this.checks = checks;
    this.goConverter = goConverter;
    this.inputFileDiscovery = new InputFileDiscovery(language);
  }

  private static GoChecks initializeChecks(CheckFactory checkFactory) {
    return new GoChecks(checkFactory)
      .addChecks(GoRulesDefinition.REPOSITORY_KEY, GoCheckList.checks());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(GoLanguage.KEY)
      .name("Code Quality and Security for Go");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    if (!isActive(sensorContext)) {
      return;
    }
    try {
      if (!goConverter.isInitialized() && sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
        LOG.info("Skipping the Go analysis, parsing is not possible with uninitialized Go converter.");
        return;
      }

      executeLogic(sensorContext);
    } catch (RuntimeException e) {
      if (GoSensor.isFailFast(sensorContext)) {
        throw e;
      } else {
        LOG.error("An error occurred during the analysis of the Go language:", e);
      }
    }
  }

  private void executeLogic(SensorContext sensorContext) {
    initialize(sensorContext);

    List<InputFileContext> inputFileContexts = inputFileDiscovery.findAllInputFiles(sensorContext);
    var goProgressReport = new GoProgressReport("Progress of the " + language.getName() + " analysis", TimeUnit.SECONDS.toMillis(PROGRESS_REPORT_INTERVAL_SECOND));
    boolean success = false;
    var converter = ASTConverterValidation.wrap(goConverter, sensorContext.config());
    var goModFileDataStore = new GoModFileAnalyzer(sensorContext).analyzeGoModFiles();
    collectTelemetry(sensorContext, goModFileDataStore);
    try {
      var visitors = visitors(sensorContext, durationStatistics, goModFileDataStore);
      success = analyseFiles(converter, sensorContext, inputFileContexts, goProgressReport, visitors, durationStatistics, goModFileDataStore);
    } finally {
      if (success) {
        goProgressReport.stop();
      } else {
        goProgressReport.cancel();
      }
      converter.terminate();
    }

    processMetrics();
    cleanUp();
  }

  protected void initialize(SensorContext sensorContext) {
    durationStatistics = new DurationStatistics(sensorContext.config());
    memoryMonitor = new MemoryMonitor(sensorContext.config());
    if (debugTypeCheck(sensorContext)) {
      goConverter.debugTypeCheck();
    }
  }

  private List<TreeVisitor<InputFileContext>> visitors(SensorContext sensorContext, DurationStatistics statistics, GoModFileDataStore goModFileDataStore) {
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goModFileDataStore));
    } else {
      return Arrays.asList(
        new IssueSuppressionVisitor(),
        new MetricVisitor(fileLinesContextFactory, executableLineOfCodePredicate()),
        new SkipNoSonarLinesVisitor(noSonarFilter),
        new SymbolVisitor<>(),
        new ChecksVisitor(checks(), statistics, goModFileDataStore),
        new CpdVisitor(),
        new SyntaxHighlighter());
    }
  }

  protected boolean analyseFiles(ASTConverter converter,
    SensorContext sensorContext,
    List<InputFileContext> inputFileContexts,
    GoProgressReport goProgressReport,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics,
    GoModFileDataStore goModFileDataStore) {
    if (sensorContext.canSkipUnchangedFiles()) {
      LOG.info("The {} analyzer is running in a context where unchanged files can be skipped.", this.language);
    }
    var filesByDirectory = InputFileDiscovery.groupFilesByDirectory(inputFileContexts);
    goProgressReport.start(filesByDirectory);
    beforeAnalyzeFile(sensorContext, filesByDirectory, goModFileDataStore);

    for (var goFolder : filesByDirectory) {
      if (sensorContext.isCancelled()) {
        return false;
      }

      var filesToAnalyse = goFolder.files();

      var moduleName = goModFileDataStore.retrieveClosestGoModFileData(goFolder.name()).moduleName();
      LOG.debug("Parse directory '{}', number of files: {}, nodule name: '{}'", goFolder.name(), filesToAnalyse.size(), moduleName);

      try {
        analyseDirectory(converter, filesToAnalyse, visitors, goProgressReport, statistics, sensorContext, moduleName);
      } catch (RuntimeException e) {
        LOG.warn("Unable to parse directory '{}'.", goFolder.name(), e);
        reportParseException(e, filesToAnalyse);
        if (GoSensor.isFailFast(sensorContext)) {
          throw e;
        }
      }
      goProgressReport.nextFolder();
    }
    return true;
  }

  private static void collectTelemetry(SensorContext sensorContext, GoModFileDataStore goModFileDataStore) {
    var goVersions = goModFileDataStore.collectGoVersions();
    String usedVersion;
    if (goVersions.isEmpty()) {
      usedVersion = "noGoModFile";
    } else {
      usedVersion = goVersions.stream()
        .sorted()
        .map(GoVersion::toString)
        .distinct()
        .collect(Collectors.joining(";"));
    }
    sensorContext.addTelemetryProperty("go.used_version", usedVersion);
  }

  protected void beforeAnalyzeFile(SensorContext sensorContext, List<GoFolder> inputFilesByFolder, GoModFileDataStore goModFileDataStore) {
    // the default implementation does nothing
  }

  // visible for tests
  static void analyseDirectory(ASTConverter converter,
    List<InputFileContext> inputFileContextList,
    List<TreeVisitor<InputFileContext>> visitors,
    GoProgressReport goProgressReport,
    DurationStatistics statistics,
    SensorContext sensorContext,
    String moduleName) {

    goProgressReport.setStep(GoProgressReport.Step.CACHING);
    Map<String, CacheHandler.CacheEntry> filenameToCacheEntry = CacheHandler.filterOutFilesFromCache(inputFileContextList, visitors);

    Map<String, String> filenameToContentMap = filenameToCacheEntry.values().stream()
      .map(CacheHandler::convertCacheEntryToFilenameAndContent)
      .filter(entry -> !EMPTY_FILE_CONTENT_PATTERN.matcher(entry.getValue()).matches())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (filenameToContentMap.isEmpty()) {
      return;
    }

    goProgressReport.setStep(GoProgressReport.Step.PARSING);
    Map<String, TreeOrError> treeOrErrorMap = statistics.time("Parse", () -> converter.parse(filenameToContentMap, moduleName));

    goProgressReport.setStep(GoProgressReport.Step.HANDLING_PARSE_ERRORS);
    handleParsingErrors(sensorContext, treeOrErrorMap, filenameToCacheEntry);

    goProgressReport.setStep(GoProgressReport.Step.ANALYZING);
    visitTrees(visitors, statistics, treeOrErrorMap, filenameToCacheEntry);
  }

  private static void handleParsingErrors(SensorContext sensorContext, Map<String, TreeOrError> treeOrErrorMap, Map<String, CacheHandler.CacheEntry> filenameToCacheResult) {
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

  private static void visitTrees(List<TreeVisitor<InputFileContext>> visitors, DurationStatistics statistics, Map<String, TreeOrError> treeOrErrorMap,
    Map<String, CacheHandler.CacheEntry> filenameToCacheEntry) {
    for (Map.Entry<String, TreeOrError> filenameToTree : treeOrErrorMap.entrySet()) {
      var treeOrError = filenameToTree.getValue();
      if (treeOrError.isTree()) {
        var filename = filenameToTree.getKey();
        var cacheResult = filenameToCacheEntry.get(filename);
        visitTree(visitors, statistics, cacheResult, treeOrError.tree());
      }
    }
  }

  private static void visitTree(List<TreeVisitor<InputFileContext>> visitors, DurationStatistics statistics, CacheHandler.CacheEntry cacheResult, Tree tree) {
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
        var message = "Cannot analyse '" + inputFileContext.inputFile() + "': " + e.getMessage();
        LOG.warn(message, e);
      }
    }
    CacheHandler.writeHashToCache(inputFileContext);
  }

  public static ParseException toParseException(String action, InputFile inputFile, Exception cause) {
    TextPointer position = cause instanceof ParseException actual ? actual.getPosition() : null;
    return new ParseException("Cannot " + action + " '" + inputFile + "': " + cause.getMessage(), position, cause, inputFile.toString());
  }

  private void reportParseException(RuntimeException e, List<InputFileContext> filesToAnalyse) {
    if (e instanceof ParseException parseException) {
      var inputFilePath = parseException.getInputFilePath();
      if (inputFilePath != null) {
        filesToAnalyse.stream()
          .filter(inputFileContext -> inputFileContext.inputFile().toString().equals(inputFilePath))
          .findFirst()
          .ifPresent(inputFileContext -> inputFileContext.reportAnalysisParseError(repositoryKey(), parseException.getMessage()));
      }
    }
  }

  protected void processMetrics() {
    durationStatistics.log();
    memoryMonitor.addRecord("End of the sensor");
    memoryMonitor.logMemory();
  }

  protected void cleanUp() {
    durationStatistics = null;
    memoryMonitor = null;
  }

  private static boolean isActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(GoPlugin.ACTIVATION_KEY).orElse(true);
  }

  public GoChecks checks() {
    return checks;
  }

  protected String repositoryKey() {
    return GoRulesDefinition.REPOSITORY_KEY;
  }

  protected Predicate<Tree> executableLineOfCodePredicate() {
    return EXECUTABLE_LINE_PREDICATE.and(t -> !(t instanceof VariableDeclarationTree)
      && !isGenericDeclaration(t));
  }

  private static boolean isGenericDeclaration(Tree tree) {
    return NativeKinds.isStringNativeKind(tree, str -> str.contains("GenDecl"));
  }

  public static boolean isFailFast(SensorContext context) {
    return context.config().getBoolean(FAIL_FAST_PROPERTY_NAME).orElse(false);
  }

  public static boolean debugTypeCheck(SensorContext context) {
    return context.config().getBoolean(DEBUG_TYPE_CHECK_PROPERTY_NAME).orElse(false);
  }
}
