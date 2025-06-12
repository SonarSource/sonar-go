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

import java.util.List;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;

public final class GoModFileFinder {
  private GoModFileFinder() {
    // Utility class
  }

  public static List<InputFile> findGoModFiles(SensorContext sensorContext) {
    FilePredicates predicates = sensorContext.fileSystem().predicates();
    var goModFilePredicate = predicates.matchesPathPattern("**/go.mod");
    return StreamSupport.stream(
      sensorContext.fileSystem().inputFiles(goModFilePredicate).spliterator(), false)
      .toList();
  }
}
