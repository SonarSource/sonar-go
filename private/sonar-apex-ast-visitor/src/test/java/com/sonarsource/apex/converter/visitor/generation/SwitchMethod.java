/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SwitchMethod {

  static final Set<String> SWITCH_METHODS = new HashSet<>(Arrays.asList(
    "_switch", "accept"));

  final Method switchMethod;
  final List<CaseMethod> caseMethods;

  SwitchMethod(Method method) {
    this.switchMethod = method;
    Class<?> switchInterface = method.getParameterTypes()[0];
    if (!switchInterface.isInterface()) {
      throw new IllegalStateException("Unexpected switch method: " + method.toGenericString());
    }
    caseMethods = Arrays.stream(switchInterface.getDeclaredMethods())
      .filter(AstClass::isPublicAndNotStatic)
      .map(CaseMethod::new)
      .collect(Collectors.toList());
  }

  static SwitchMethod from(Method method) {
    Type[] parameterTypes = method.getParameterTypes();
    if (SWITCH_METHODS.contains(method.getName()) &&
      parameterTypes.length == 1 &&
      ClassUtils.startWith(parameterTypes[0], "apex.jorje.data.")) {
      return new SwitchMethod(method);
    }
    return null;
  }

  String switchMethodName() {
    return switchMethod.getName();
  }

  String switchInterfaceTypeString() {
    Type type = switchMethod.getGenericParameterTypes()[0];
    String parameterizedSuffix = "";
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      parameterizedSuffix = IntStream.range(0, parameterizedType.getActualTypeArguments().length)
        .mapToObj(i -> "Void")
        .collect(Collectors.joining(",", "<", ">"));
      type = parameterizedType.getRawType();
    }
    if (!(type instanceof Class) || !((Class) type).isInterface()) {
      throw new IllegalStateException("Unsupported switch interface: " + type.getTypeName());
    }
    return ((Class) type).getCanonicalName() + parameterizedSuffix;
  }

}
