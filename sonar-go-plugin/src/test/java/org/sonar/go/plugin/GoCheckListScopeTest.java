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

import com.eclipsesource.json.Json;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.check.Rule;
import org.sonar.go.checks.GoCheckList;

/**
 * This test would be better suited in GoCheckListTest.
 * We don't have access to the static rspec resource files there
 */
class GoCheckListScopeTest {

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource
  void checksShouldBeOrderedByScope(String testName, List<Class<?>> classes, String expectedScope) throws IOException {
    var softly = new SoftAssertions();
    for (Class<?> clazz : classes) {
      Rule annotation = clazz.getAnnotation(Rule.class);
      String key = annotation.key();
      File file = new File("src/main/resources/org/sonar/l10n/go/rules/go/%s.json".formatted(key));
      String content = Files.readString(file.toPath());
      String scope = Json.parse(content).asObject().getString("scope", null);
      softly.assertThat(scope)
        .withFailMessage("Expected scope to be '%s', but was '%s', for '%s'. Move the class to another list in GoCheckList.".formatted(expectedScope, scope, key))
        .isEqualTo(expectedScope);
    }
    softly.assertAll();
  }

  static Stream<Arguments> checksShouldBeOrderedByScope() {
    return Stream.of(
      Arguments.of("main checks", GoCheckList.mainChecks(), "Main"),
      Arguments.of("main and test checks", GoCheckList.mainAndTestChecks(), "All"));
  }
}
