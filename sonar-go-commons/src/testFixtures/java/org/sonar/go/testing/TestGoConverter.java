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
package org.sonar.go.testing;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import org.sonar.go.converter.DefaultCommand;
import org.sonar.go.converter.GoConverter;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;

public class TestGoConverter {
  public static final File CONVERTER_DIR = Paths.get("build", "tmp").toFile();
  public static final GoConverter GO_CONVERTER = new GoConverter(CONVERTER_DIR);
  public static final GoConverter GO_CONVERTER_DEBUG_TYPE_CHECK = new GoConverter(new CommandWithDebugTypeCheck(CONVERTER_DIR));

  public static Tree parse(String content) {
    return GO_CONVERTER.parse(Map.of("foo.go", content)).get("foo.go").tree();
  }

  public static String parseAndReturnError(String content) {
    return GO_CONVERTER.parse(Map.of("foo.go", content)).get("foo.go").error();
  }

  public static <T extends Tree> T parseAndRetrieve(Class<T> clazz, String content) {
    return retrieve(clazz, parse(content));
  }

  private static <T extends Tree> T retrieve(Class<T> clazz, Tree tree) {
    return tree.descendants()
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .findFirst()
      .get();
  }

  public static Tree parseStatement(String content) {
    var code = """
      package main
      func main() {
        %s
      }
      """.formatted(content);
    var root = (TopLevelTree) GO_CONVERTER.parse(Map.of("foo.go", code)).get("foo.go").tree();
    var main = (FunctionDeclarationTree) root.declarations().get(1);
    return main.body().statementOrExpressions().get(0).children().get(0);
  }

  private static class CommandWithDebugTypeCheck extends DefaultCommand {
    public CommandWithDebugTypeCheck(File workDir) {
      super(workDir, "-debug_type_check", "-");
    }
  }
}
