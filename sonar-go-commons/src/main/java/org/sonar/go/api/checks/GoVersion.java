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
package org.sonar.go.api.checks;

import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.utils.Version;

public class GoVersion {
  public static final GoVersion UNKNOWN_VERSION = new GoVersion(null);

  private final Version version;

  /**
   * Parse a Go version from a string using {@link Version#parse(String)}.
   * Does not support release candidate (rc) or beta versions as we don't need this precision.
   */
  public static GoVersion parse(@Nullable String version) {
    if (version == null) {
      return UNKNOWN_VERSION;
    }
    try {
      return new GoVersion(Version.parse(version));
    } catch (NumberFormatException e) {
      return UNKNOWN_VERSION;
    }
  }

  private GoVersion(@Nullable Version version) {
    this.version = version;
  }

  public boolean isUnknownVersion() {
    return version == null;
  }

  public boolean isGreaterThanEqualOrUnknown(GoVersion otherVersion) {
    if (isUnknownVersion() || otherVersion.isUnknownVersion()) {
      return true;
    }

    return this.version.isGreaterThanOrEqual(otherVersion.version);
  }

  @Override
  public String toString() {
    if (version == null) {
      return "unknown";
    }
    return version.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return Objects.equals(version, ((GoVersion) o).version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(version);
  }
}
