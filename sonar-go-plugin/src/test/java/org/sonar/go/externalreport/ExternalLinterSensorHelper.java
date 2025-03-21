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
package org.sonar.go.externalreport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.go.plugin.GoLanguage;

public class ExternalLinterSensorHelper {

  static final Path REPORT_BASE_PATH = Paths.get("src", "test", "resources", "externalreport").toAbsolutePath();

  static List<ExternalIssue> executeSensor(Sensor sensor, SensorContextTester context) {
    sensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  static SensorContextTester createContext() throws IOException {
    Path workDir = Files.createTempDirectory("gotemp");
    workDir.toFile().deleteOnExit();
    Path projectDir = Files.createTempDirectory("goproject");
    projectDir.toFile().deleteOnExit();
    SensorContextTester context = SensorContextTester.create(workDir);
    context.fileSystem().setWorkDir(workDir);
    Path filePath = projectDir.resolve("main.go");
    InputFile mainInputFile = TestInputFileBuilder.create("module", projectDir.toFile(), filePath.toFile())
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage(GoLanguage.KEY)
      .setContents("package main\n" +
        "import \"fmt\"\n" +
        "func main() {\n" +
        "  fmt.Println(\"Hello\")\n" +
        "}\n")
      .setType(InputFile.Type.MAIN)
      .build();
    context.fileSystem().add(mainInputFile);
    return context;
  }

}
