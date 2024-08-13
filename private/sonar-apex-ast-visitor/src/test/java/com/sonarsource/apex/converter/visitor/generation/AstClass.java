/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.sonarsource.apex.converter.visitor.generation.ClassUtils.rawEqual;

public class AstClass {

  static final Set<Class<?>> IGNORED_REFERENCED_CLASSES_WITHOUT_LOCATION = new HashSet<>(Arrays.asList(
    apex.jorje.data.ast.VersionRef.class,
    apex.jorje.data.sosl.SearchWithClauseValue.class));

  static final List<Class<?>> IGNORED_DERIVED_CLASSES_WITHOUT_LOCATION = Arrays.asList(
    apex.jorje.data.ast.CompilationUnit.InvalidDeclUnit.class,
    apex.jorje.data.sosl.FindValue.FindString.class,
    apex.jorje.data.soql.Geolocation.GeolocationLiteral.class);

  static final Map<String, List<String>> IGNORED_PROPERTIES = new HashMap<>();
  static {
    IGNORED_PROPERTIES.put("java.lang.Object",
      Arrays.asList("equals", "hashCode", "toString"));
    // getNames, getHeldType and getTypeArguments contains the same Identifier
    IGNORED_PROPERTIES.put("apex.jorje.data.ast.TypeRefs.ArrayTypeRef",
      Arrays.asList("getNames", "getTypeArguments"));
  }

  public final Class<?> wrappedAstClass;

  public String locationProperty = null;

  public final List<AstProperty> properties;

  public final Set<Class<?>> referencedAstClasses;

  /**
   * The switchMethod is routing to the right method on the visitor
   */
  public SwitchMethod switchMethod;

  public AstClass(Class<?> wrappedAstClass) {

    this.wrappedAstClass = wrappedAstClass;
    properties = new ArrayList<>();
    referencedAstClasses = new HashSet<>();
    Set<String> memberToIgnore = new HashSet<>();

    // collect all properties

    for (Class<?> cls : superToDerivedOrderedInheritanceClasses(wrappedAstClass)) {
      memberToIgnore.addAll(IGNORED_PROPERTIES.getOrDefault(cls.getCanonicalName(), Collections.emptyList()));
    }
    for (Class<?> cls : superToDerivedOrderedInheritanceClasses(wrappedAstClass)) {
      Arrays.stream(cls.getDeclaredFields())
        .filter(AstClass::isPublicAndNotStatic)
        .filter(field -> memberToIgnore.add(field.getName()))
        .forEach(field -> addProperty(field.getGenericType(), field.getName()));

      Arrays.stream(cls.getDeclaredMethods())
        .filter(AstClass::isPublicAndNotStatic)
        .filter(AstClass::isGetter)
        .filter(method -> memberToIgnore.add(method.getName()))
        .forEach(method -> addProperty(method.getGenericReturnType(), method.getName() + "()"));

      Arrays.stream(cls.getDeclaredMethods())
        .filter(AstClass::isPublicAndNotStatic)
        .filter(AstClass::isNotGetter)
        .filter(method -> memberToIgnore.add(method.getName()))
        .forEach(this::addMethod);
    }
    if (switchMethod == null && properties.isEmpty() && locationProperty == null && !isKnownClassWithoutLocation(wrappedAstClass)) {
      throw new IllegalStateException("Missing switchMethod, properties and location on: " + wrappedAstClass.getCanonicalName());
    }

  }

  private static Collection<Class<?>> superToDerivedOrderedInheritanceClasses(Class<?> cls) {
    List<Class<?>> list = new ArrayList<>();
    addSuperToDerivedInheritanceClasses(list, cls);
    return list;
  }

  private static void addSuperToDerivedInheritanceClasses(List<Class<?>> list, Class<?> cls) {
    if (cls == null || cls.equals(Object.class)) {
      return;
    }
    addSuperToDerivedInheritanceClasses(list, cls.getSuperclass());
    for (Class<?> anInterface : cls.getInterfaces()) {
      addSuperToDerivedInheritanceClasses(list, anInterface);
    }
    list.add(cls);
  }

  private void addReferencedType(Type type) {
    if (type instanceof TypeVariable || isPrimitiveOrJavaLangOrJavaTime(type)) {
      return;
    }
    Type underlyingType = type;
    while (underlyingType instanceof ParameterizedType) {
      underlyingType = ((ParameterizedType) underlyingType).getRawType();
    }
    if (underlyingType instanceof Class && ClassUtils.startWith(underlyingType, "apex.jorje.data.")) {
      Class underlyingClass = (Class) underlyingType;
      if (!underlyingClass.isEnum() && !isKnownClassWithoutLocation(underlyingClass)) {
        referencedAstClasses.add(underlyingClass);
      }
    } else if (underlyingType != void.class) {
      throw new IllegalStateException("Unexpected referenced type:(" + underlyingType.getClass().getCanonicalName() + ")" + type.getTypeName() +
        " on " + wrappedAstClass.getCanonicalName());
    }
  }

  private void addMethod(Method method) {
    SwitchMethod switchMethod = SwitchMethod.from(method);
    if (switchMethod != null) {
      if (ClassUtils.hasMethod(wrappedAstClass.getSuperclass(), method)
        || Arrays.stream(wrappedAstClass.getInterfaces()).anyMatch(i -> ClassUtils.hasMethod(i, method))) {
        return;
      }
      if (this.switchMethod != null && !this.switchMethod.switchMethod.equals(switchMethod.switchMethod)) {
        throw new IllegalStateException("Second switch, first: " + this.switchMethod.switchMethod.toGenericString() + ", second: " + method.toGenericString());
      }
      this.switchMethod = switchMethod;
      switchMethod.caseMethods.forEach(caseMethod -> addReferencedType(caseMethod.parameterClass()));
      return;
    }
    addReferencedType(method.getGenericReturnType());
  }

  private void addProperty(Type type, String name) {
    if (rawEqual(type, apex.jorje.data.Location.class)) {
      locationProperty = name;
    } else {
      boolean isOptional = false;
      boolean isList = false;
      Type innerType = type;
      if (rawEqual(innerType, Optional.class)) {
        isOptional = true;
        innerType = ((ParameterizedType) innerType).getActualTypeArguments()[0];
      }
      if (rawEqual(innerType, List.class)) {
        isList = true;
        innerType = ((ParameterizedType) innerType).getActualTypeArguments()[0];
      }
      if (ClassUtils.isEnum(innerType) || isPrimitiveOrJavaLangOrJavaTime(innerType)) {
        return;
      }
      if (!(innerType instanceof Class)) {
        throw new IllegalStateException("Unsupported type: " + innerType.getTypeName());
      }
      if (!IGNORED_REFERENCED_CLASSES_WITHOUT_LOCATION.contains(innerType)) {
        addReferencedType(innerType);
        properties.add(new AstProperty(isOptional, isList, innerType, name));
      }
    }
  }

  private boolean isPrimitiveOrJavaLangOrJavaTime(Type type) {
    return ClassUtils.startWith(type, "java.lang.") ||
      ClassUtils.startWith(type, "java.time.") ||
      (type instanceof Class && ((Class) type).isPrimitive());
  }

  private static boolean isNotGetter(Method method) {
    return !isGetter(method);
  }

  public static boolean isGetter(Method method) {
    return (method.getName().startsWith("get") || method.getName().startsWith("is")) &&
      method.getParameterCount() == 0 && !method.getReturnType().equals(void.class);
  }

  public static boolean isPublicAndNotStatic(Field field) {
    return isPublicAndNotStatic(field.getModifiers());
  }

  public static boolean isPublicAndNotStatic(Method method) {
    return isPublicAndNotStatic(method.getModifiers());
  }

  public static boolean isPublicAndNotStatic(int modifiers) {
    return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
  }

  public static boolean isKnownClassWithoutLocation(Class<?> cls) {
    return IGNORED_REFERENCED_CLASSES_WITHOUT_LOCATION.contains(cls) ||
      IGNORED_DERIVED_CLASSES_WITHOUT_LOCATION.contains(cls);
  }
}
