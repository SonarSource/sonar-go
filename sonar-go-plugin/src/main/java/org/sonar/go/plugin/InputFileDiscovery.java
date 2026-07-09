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
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.resources.Language;
import org.sonarsource.analyzer.commons.appsec.TestFileClassifier;

public class InputFileDiscovery {

  private final Language language;

  public InputFileDiscovery(Language language) {
    this.language = language;
  }

  public List<InputFileContext> findAllInputFiles(SensorContext sensorContext) {
    var fileSystem = sensorContext.fileSystem();
    FilePredicate langPredicate = fileSystem.predicates().hasLanguage(language.getKey());
    var testFileClassifier = TestFileClassifier.of(sensorContext.config(), "**/*_test.go");
    return StreamSupport.stream(fileSystem.inputFiles(langPredicate).spliterator(), false)
      .map(inputFile -> new InputFileContext(sensorContext, inputFile,
        inputFile.type() == InputFile.Type.TEST || testFileClassifier.looksLikeTestFile(inputFile)))
      .toList();
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
