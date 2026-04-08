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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.resources.Language;

public class InputFileDiscovery {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileDiscovery.class);
  private static final String SONAR_TESTS_PROPERTY = "sonar.tests";
  private static final String SONAR_TEST_INCLUSIONS_PROPERTY = "sonar.test.inclusions";
  private static final String TEST_PROPERTIES_NOT_SET_MESSAGE = """
    The properties "%s" and "%s" are not set. To improve the analysis accuracy, we categorize a file as a test file when the filename has suffix: "_test.go"
      It is highly recommended to set those properties, e.g.: for the Go projects it is usually: "%1$s=." and "%2$s=**/*_test.go\""""
    .formatted(SONAR_TESTS_PROPERTY, SONAR_TEST_INCLUSIONS_PROPERTY);
  private static final String TEST_PROPERTIES_SET_MESSAGE = "The properties \"%s\" and \"%s\" are set: \"%1$s=%3$s\" and \"%2$s=%4$s\"";

  private final Language language;

  public InputFileDiscovery(Language language) {
    this.language = language;
  }

  public List<InputFileContext> findAllInputFiles(SensorContext sensorContext) {
    var fileSystem = sensorContext.fileSystem();
    var predicates = fileSystem.predicates();
    FilePredicate langPredicate = predicates.hasLanguage(language.getKey());

    var sonarTests = sensorContext.config().getStringArray(SONAR_TESTS_PROPERTY);
    var sonarTestInclusions = sensorContext.config().getStringArray(SONAR_TEST_INCLUSIONS_PROPERTY);

    if (sonarTests.length == 0 && sonarTestInclusions.length == 0) {
      LOG.info(TEST_PROPERTIES_NOT_SET_MESSAGE);

      var allFiles = fileSystem.inputFiles(langPredicate);
      return StreamSupport.stream(allFiles.spliterator(), false)
        .map(inputFile -> new InputFileContext(sensorContext, inputFile, inputFile.filename().endsWith("_test.go")))
        .toList();
    } else {
      var message = String.format(
        TEST_PROPERTIES_SET_MESSAGE,
        SONAR_TESTS_PROPERTY, SONAR_TEST_INCLUSIONS_PROPERTY,
        String.join(",", sonarTests), String.join(",", sonarTestInclusions));
      LOG.debug(message);

      return StreamSupport.stream(fileSystem.inputFiles(langPredicate).spliterator(), false)
        .map(inputFile -> new InputFileContext(sensorContext, inputFile))
        .toList();
    }
  }

  static List<GoFolder> groupFilesByDirectory(List<InputFileContext> inputFileContexts) {
    Map<String, List<InputFileContext>> filesByDirectory = inputFileContexts.stream()
      .collect(Collectors.groupingBy((InputFileContext ctx) -> {
        var path = ctx.inputFile().uri().getPath();
        int lastSeparatorIndex = path.lastIndexOf("/");
        if (lastSeparatorIndex == -1) {
          return "";
        }
        return path.substring(0, lastSeparatorIndex);
      }));

    return filesByDirectory.entrySet().stream()
      .map(entry -> new GoFolder(entry.getKey(), entry.getValue()))
      .toList();
  }
}
