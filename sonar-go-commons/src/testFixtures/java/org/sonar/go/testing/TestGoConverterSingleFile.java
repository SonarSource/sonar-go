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
import java.nio.file.Paths;
import java.util.Map;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.converter.GoServerProcess;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeOrError;

public class TestGoConverterSingleFile {
  public static final File CONVERTER_DIR = Paths.get("build", "test-tmp").toFile();
  /**
   * The Go process of all the tests of the JVM, never closed: it stops when the JVM exits, as its stdin is then closed.
   */
  public static final GoServerProcess GO_PROCESS = started(new GoServerProcess(CONVERTER_DIR));
  public static final GoConverter GO_CONVERTER = new GoConverter(CONVERTER_DIR, GO_PROCESS);
  public static final GoConverter GO_CONVERTER_DEBUG_TYPE_CHECK = withDebugTypeCheck(new GoConverter(CONVERTER_DIR, GO_PROCESS));

  private static GoServerProcess started(GoServerProcess goProcess) {
    goProcess.start();
    return goProcess;
  }

  public static GoConverter withDebugTypeCheck(GoConverter converter) {
    converter.debugTypeCheck();
    return converter;
  }

  public static Tree parse(String content) {
    return parseAndReturnTreeOrError(content).tree();
  }

  public static TreeOrError parseAndReturnTreeOrError(String content) {
    return GO_CONVERTER_DEBUG_TYPE_CHECK.parse(Map.of("foo.go", content), "ModuleNameForTest").get("foo.go");
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
    var root = (TopLevelTree) GO_CONVERTER_DEBUG_TYPE_CHECK.parse(Map.of("foo.go", code), "ModuleNameForTest").get("foo.go").tree();
    var main = (FunctionDeclarationTree) root.declarations().get(1);
    return main.body().statementOrExpressions().get(0).children().get(0);
  }
}
