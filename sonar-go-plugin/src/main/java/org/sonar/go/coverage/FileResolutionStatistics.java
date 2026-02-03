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
package org.sonar.go.coverage;

public class FileResolutionStatistics {
  private int absolutePath = 0;
  private int relativeNoModuleInGoModDir = 0;
  private int absoluteNoModuleInReportPath = 0;
  private int relativePath = 0;
  private int relativeNoModuleInReportPath = 0;
  private int relativeSubPaths = 0;
  private int unresolved = 0;

  public int absolutePath() {
    return absolutePath;
  }

  public void incrementAbsolutePath() {
    this.absolutePath++;
  }

  public int relativeNoModuleInGoModDir() {
    return relativeNoModuleInGoModDir;
  }

  public void incrementRelativeNoModuleInGoModDir() {
    this.relativeNoModuleInGoModDir++;
  }

  public int absoluteNoModuleInReportPath() {
    return absoluteNoModuleInReportPath;
  }

  public void incrementAbsoluteNoModuleInReportPath() {
    this.absoluteNoModuleInReportPath++;
  }

  public int relativePath() {
    return relativePath;
  }

  public void incrementRelativePath() {
    this.relativePath++;
  }

  public int relativeNoModuleInReportPath() {
    return relativeNoModuleInReportPath;
  }

  public void incrementRelativeNoModuleInReportPath() {
    this.relativeNoModuleInReportPath++;
  }

  public int relativeSubPaths() {
    return relativeSubPaths;
  }

  public void incrementRelativeSubPaths() {
    this.relativeSubPaths++;
  }

  public int unresolved() {
    return unresolved;
  }

  public void incrementUnresolved() {
    this.unresolved++;
  }
}
