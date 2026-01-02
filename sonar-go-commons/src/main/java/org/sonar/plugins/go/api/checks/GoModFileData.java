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
package org.sonar.plugins.go.api.checks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Data extracted from the go.mod file.
 * @param moduleName the name of the module
 * @param goVersion the Go version used in the module
 * @param replacedModules a map of replaced modules, where the key is the original module and the value is the replacement module.
 */
public record GoModFileData(String moduleName, GoVersion goVersion, List<Map.Entry<ModuleSpec, ModuleSpec>> replacedModules, String goModFilePath) {

  public static final GoModFileData UNKNOWN_DATA = new GoModFileData("", GoVersion.UNKNOWN_VERSION, Collections.emptyList(), "");

  /**
   * Represents a module specification with its name and an optional version.
   * @param moduleName the name of the module
   * @param version the version of the module, can be null if not specified
   */
  public record ModuleSpec(String moduleName, @Nullable String version) {
  }
}
