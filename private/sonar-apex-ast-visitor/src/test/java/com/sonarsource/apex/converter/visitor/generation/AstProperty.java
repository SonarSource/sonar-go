/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.lang.reflect.Type;
import java.util.Locale;

class AstProperty {

  final boolean isOptional;
  final boolean isList;
  final Type type;
  final String name;

  AstProperty(boolean isOptional, boolean isList, Type type, String name) {
    this.isOptional = isOptional;
    this.isList = isList;
    this.type = type;
    this.name = name;
  }

  String enumKey() {
    return name.replace("()", "").toUpperCase(Locale.ROOT);
  }

}
