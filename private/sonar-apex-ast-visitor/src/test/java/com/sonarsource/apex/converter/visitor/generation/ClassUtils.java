/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ClassUtils {

  private ClassUtils() {
    // utlility class
  }


  public static Class<?> underlyingClass(Type type) {
    Type underlyingType = type;
    while (underlyingType instanceof ParameterizedType) {
      underlyingType = ((ParameterizedType) underlyingType).getRawType();
    }
    if (underlyingType instanceof Class) {
      return (Class) underlyingType;
    }
    return null;
  }

  public static boolean isEnum(Type type) {
    Class<?> cls = underlyingClass(type);
    if (cls != null) {
      return cls.isEnum();
    }
    return false;
  }

  public static boolean startWith(Type type, String packageName) {
    Class<?> cls = underlyingClass(type);
    if (cls != null) {
      return cls.getCanonicalName().startsWith(packageName);
    }
    return false;
  }

  public static boolean rawEqual(Type a, Type b) {
    Class<?> clsA = underlyingClass(a);
    Class<?> clsB = underlyingClass(b);
    if (clsA == clsB) {
      return true;
    }
    if (clsA == null || clsB == null) {
      return false;
    }
    return clsA.getCanonicalName().equals(clsB.getCanonicalName());
  }

  public static boolean hasMethod(Class<?> cls, Method method) {
    if (cls == null) {
      return false;
    }
    for (Method declaredMethod : cls.getDeclaredMethods()) {
      if (declaredMethod.getName().equals(method.getName()) &&
        declaredMethod.getParameterCount() == method.getParameterCount()) {
        for (int i = 0; i < declaredMethod.getParameterCount(); i++) {
          if (!rawEqual(declaredMethod.getParameterTypes()[i], method.getParameterTypes()[i])) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }
}
