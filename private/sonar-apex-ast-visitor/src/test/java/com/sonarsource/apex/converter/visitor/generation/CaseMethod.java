/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CaseMethod {
  private static final Set<String> CASE_METHODS = new HashSet<>(Arrays.asList(
    "_case", "visit"));

  private final Method method;

  CaseMethod(Method method) {
    this.method = method;
    if (!CASE_METHODS.contains(method.getName()) || method.getParameterCount() != 1) {
      throw new IllegalStateException("Unexpected case method: " + method.toGenericString());
    }
  }

  public String name() {
    return method.getName();
  }

  Type returnType() {
    return method.getGenericReturnType();
  }

  Type parameterType() {
    return method.getParameterTypes()[0];
  }

  Class<?> parameterClass() {
    Class<?> cls = ClassUtils.underlyingClass(parameterType());
    if (cls == null) {
      throw new IllegalStateException("Unexpected case method parameter: " + method.toGenericString());
    }
    return cls;
  }
}
