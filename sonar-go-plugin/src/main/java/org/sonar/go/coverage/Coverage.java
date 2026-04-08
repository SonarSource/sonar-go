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
package org.sonar.go.coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Coverage {

  final GoPathContext goContext;
  Map<String, List<CoverageStat>> fileMap = new HashMap<>();

  Coverage(GoPathContext goContext) {
    this.goContext = goContext;
  }

  void add(CoverageStat coverage) {
    fileMap
      .computeIfAbsent(goContext.resolve(coverage.filePath), key -> new ArrayList<>())
      .add(coverage);
  }

}
