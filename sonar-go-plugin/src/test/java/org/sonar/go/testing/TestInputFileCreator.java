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
package org.sonar.go.testing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

public class TestInputFileCreator {
  public static InputFile createInputFile(String relativePath, String content, File baseDir) {
    return createInputFile(relativePath, content, baseDir, null, null);
  }

  public static InputFile createInputFile(String relativePath, String content, File baseDir, @Nullable InputFile.Status status, @Nullable InputFile.Type type) {
    TestInputFileBuilder builder = new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(org.sonar.go.plugin.GoLanguage.KEY)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content);
    if (status != null) {
      builder.setStatus(status);
    }
    if (type != null) {
      builder.setType(type);
    }
    return builder.build();
  }
}
