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
package org.sonar.plugins.go.api;

public interface Type {

  /**
   * @return text representation of the type, e.g.: {@code net/http.Cookie}
   */
  String type();

  /**
   * @return text representation of the package name, e.g.: {@code net/http}
   */
  String packageName();

  /**
   * Checks if the type is equivalent to the basic type from the argument,
   * e.g.: for baseType: {@code net/http.Cookie}, the following types will be {@code true}:
   * <ul>
   *   <li>{@code net/http.Cookie}</li>
   *   <li>{@code &net/http.Cookie}</li>
   *   <li>{@code *net/http.Cookie}</li>
   * </ul>
   */
  boolean isTypeOf(String baseType);
}
