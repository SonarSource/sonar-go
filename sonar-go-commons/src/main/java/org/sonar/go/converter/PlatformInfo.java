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
package org.sonar.go.converter;

/**
 * Abstraction for platform information to avoid direct dependency on System properties.
 * This enables proper testing without modifying global JVM state.
 */
public interface PlatformInfo {
  /**
   * Returns the operating system name.
   * @return the OS name (e.g., "Linux", "Mac OS X", "Windows 10")
   */
  String osName();

  /**
   * Returns the system architecture.
   * @return the architecture (e.g., "x86_64", "aarch64", "amd64")
   */
  String osArch();
}
