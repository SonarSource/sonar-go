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
package org.sonar.go.impl;

import org.sonar.plugins.go.api.Type;

public class TypeImpl implements Type {

  private final String type;
  private final String packageName;

  public TypeImpl(String type, String packageName) {
    this.type = type;
    this.packageName = packageName;
  }

  public static TypeImpl createFromType(String text) {
    var index = text.lastIndexOf(".");
    var packageName = "";
    if (index != -1) {
      packageName = text.substring(0, index);
    }
    if (packageName.startsWith("&") || packageName.startsWith("*")) {
      packageName = packageName.substring(1);
    }
    return new TypeImpl(text, packageName);
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String packageName() {
    return packageName;
  }

  @Override
  public boolean isTypeOf(String baseType) {
    return baseType.equals(extractBaseType());
  }

  private String extractBaseType() {
    if (type.startsWith("&") || type.startsWith("*")) {
      return type.substring(1);
    }
    return type;
  }
}
