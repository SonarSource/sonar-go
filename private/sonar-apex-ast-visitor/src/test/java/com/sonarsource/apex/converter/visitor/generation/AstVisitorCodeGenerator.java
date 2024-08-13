/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import apex.jorje.data.ast.CompilationUnit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class AstVisitorCodeGenerator {

  private static final Path PROPERTY_ORDER_FILE = Paths.get("src", "test", "java", "com", "sonarsource", "apex", "converter", "visitor", "generation", "PropertyOrder.properties");
  private static final Map<String, Integer> PROPERTY_ORDER = loadPropertiesOrderMap();

  private final String generatedClassPackage;
  private final String generatedClassName;
  private final Set<Class<?>> allAstClasses = new HashSet<>();
  private final Map<Class<?>, CaseMethod> caseMethodMap = new HashMap<>();
  private final StringBuilder code = new StringBuilder();

  public AstVisitorCodeGenerator(String generatedClassPackage, String generatedClassName) {
    this.generatedClassPackage = generatedClassPackage;
    this.generatedClassName = generatedClassName;
    ProcessingClassQueue classQueue = new ProcessingClassQueue(allAstClasses);
    classQueue.pushIfNotExists(CompilationUnit.class);
    while (!classQueue.isEmpty()) {
      Class<?> cls = classQueue.pop();
      AstClass astClass = new AstClass(cls);
      astClass.referencedAstClasses.forEach(classQueue::pushIfNotExists);
      if (astClass.switchMethod != null) {
        astClass.switchMethod.caseMethods.forEach(caseMethod -> caseMethodMap.merge(caseMethod.parameterClass(), caseMethod, (a, b) -> {
          throw new IllegalStateException("Incompatible caseMethod " + a.name() + " and " + b.name() + " on" + cls.getCanonicalName());
        }));
      }
    }
  }

  /**
   * This regenerates the PropertyOrder.properties file that is used to enforce order in the class members when
   * generating the GeneratedApexAstVisitor
   */
  public static void main(String[] args) {
    // This generator use refection to generate the visitor, unfortunately Class#getDeclaredFields
    // and Class#getDeclaredMethods does not guarantee the source code order.
    // This function store the generation order, so the order will be the same during test.
    Properties properties = new Properties();
    AstVisitorCodeGenerator generation = new AstVisitorCodeGenerator("none", "none");
    generation.allAstClasses.stream()
      .map(AstClass::new)
      .filter(cls -> !cls.properties.isEmpty())
      .sorted(Comparator.comparing(cls -> cls.wrappedAstClass.getCanonicalName()))
      .forEach(cls -> {
        String propList = cls.properties.stream().map(prop->prop.name).collect(Collectors.joining(","));
        properties.put(cls.wrappedAstClass.getCanonicalName(), propList);
      });
    try (OutputStream outputStream = Files.newOutputStream(PROPERTY_ORDER_FILE)) {
      properties.save(outputStream, null);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Map<String, Integer> loadPropertiesOrderMap() {
    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(PROPERTY_ORDER_FILE)) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    Map<String, Integer> orderMap = new HashMap<>();
    properties.forEach(
      (className, propertyList) -> {
        String[] propertyNames = ((String) propertyList).split(",");
        for (int index = 0; index < propertyNames.length; index++) {
          orderMap.put(className + "#" + propertyNames[index], index);
        }
      });
    return orderMap;
  }

  private List<AstProperty> sorted(List<AstProperty> properties, Class<?> cls) {
    return properties.stream()
      .sorted(Comparator.comparing(prop -> PROPERTY_ORDER.getOrDefault(cls.getCanonicalName()+"#"+prop.name, Integer.MAX_VALUE)))
      .collect(Collectors.toList());
  }

  private StringBuilder append(Object obj) {
    return code.append(obj);
  }

  public String generate() {
    code.setLength(0);
    append("/*\n");
    append(" * Copyright (C) 2018-2024 SonarSource SA\n");
    append(" * All rights reserved\n");
    append(" * mailto:info AT sonarsource DOT com\n");
    append(" */\n");
    append("package ").append(generatedClassPackage).append(";\n");
    append("\n");
    append("import apex.jorje.data.Location;\n");
    append("import javax.annotation.Nullable;\n");
    append("\n");
    append("import static java.util.Optional.ofNullable;\n");
    append("\n");
    append("@javax.annotation.Generated(\"com.sonarsource.apex.converter.visitor.GeneratedApexAstVisitorTest.main\")\n");
    append("public class ").append(generatedClassName).append(implementedList()).append(" {\n");
    append("\n");
    append("  protected void defaultEnter(Object node, @Nullable Location location) {\n");
    append("    // can be overridden\n");
    append("  }\n");
    append("\n");
    append("  protected void defaultExit(Object node, @Nullable Location location) {\n");
    append("    // can be overridden\n");
    append("  }\n");
    append("\n");
    append("  @Nullable\n");
    append("  protected static Location valid(@Nullable Location location) {\n");
    append("    // Some AST node have a non null location equals to Locations.NONE like apex.jorje.data.soql.Order.OrderAsc\n");
    append("    if (location == null || (location.getStartIndex() == 0 && location.getEndIndex() == 0)) {\n");
    append("      return null;\n");
    append("    }\n");
    append("    return location;\n");
    append("  }\n");
    append("\n");
    append("  protected void prepareChildVisit(Property property) {\n");
    append("    // can be overridden\n");
    append("  }\n");
    append("\n");
    for (Class<?> leafClass : AstClass.IGNORED_DERIVED_CLASSES_WITHOUT_LOCATION) {
      append("  public void _case(" + leafClass.getCanonicalName() + " node) {\n");
      append("    // ignore AST leaf without location\n");
      append("  }\n");
      append("\n");
    }
    allAstClasses.stream()
      .sorted(Comparator.comparing(Class::getCanonicalName))
      .forEach(cls -> renderCaseClass(new AstClass(cls)));
    appendPropertiesHierarchy();
    append("}\n");

    return code.toString();
  }

  private void appendPropertiesHierarchy() {
    append("  public interface Property {\n");
    append("    Class<?> declaringClass();\n");
    append("    String accessor();\n");
    append("    boolean isOptional();\n");
    append("    boolean isList();\n");
    append("\n");

    allAstClasses.stream()
      .sorted(Comparator.comparing(Class::getCanonicalName))
      .forEach(cls -> renderCaseClassProperties(new AstClass(cls)));

    append("  }\n");
    append("\n");
  }

  private void renderCaseClassProperties(AstClass caseClass) {
    if (!caseClass.properties.isEmpty()) {
      append("    enum " + caseClass.wrappedAstClass.getSimpleName() + " implements Property {\n");
      for (AstProperty property : sorted(caseClass.properties, caseClass.wrappedAstClass)) {
        append("      " + property.enumKey() + " {\n");
        append("        public Class<?> declaringClass() { return " + caseClass.wrappedAstClass.getCanonicalName() + ".class; }\n");
        append("        public String accessor()         { return \"" + property.name + "\"; }\n");
        append("        public boolean isOptional()      { return " + property.isOptional + "; }\n");
        append("        public boolean isList()          { return " + property.isList + "; }\n");
        append("      },\n");
      }
      append("    }\n");
      append("\n");
    }
  }

  private String implementedList() {
    return allAstClasses.stream()
      .map(AstClass::new)
      .map(cls -> cls.switchMethod)
      .filter(Objects::nonNull)
      .map(SwitchMethod::switchInterfaceTypeString)
      .collect(Collectors.toSet()).stream()
      .sorted()
      .collect(Collectors.joining(",\n  ", " implements\n  ", ""));
  }

  private void renderCaseClass(AstClass caseClass) {
    CaseMethod caseMethod = caseMethodMap.get(caseClass.wrappedAstClass);
    String methodName = lookupMethodName(caseClass.wrappedAstClass);
    boolean isPrimitiveVoid = caseMethod == null || ClassUtils.rawEqual(caseMethod.returnType(), void.class);
    String returnType = isPrimitiveVoid ? "void" : "Void";
    String returnStatement = isPrimitiveVoid ? "" : "    return null;\n";
    if (caseClass.switchMethod != null) {
      renderCaseClassWithSwitch(returnType, methodName, caseClass, returnStatement);
    } else {
      renderCaseClassWithEnterAndExit(returnType, methodName, caseClass, returnStatement);
    }
  }

  private void renderCaseClassWithSwitch(String returnType, String methodName, AstClass caseClass, String returnStatement) {
    append("  public ").append(returnType).append(" ").append(methodName).append("(").append(caseClass.wrappedAstClass.getCanonicalName()).append(" node) {\n");
    append("    node." + caseClass.switchMethod.switchMethodName() + "(this);\n");
    append(returnStatement);
    append("  }\n");
    append("\n");
  }

  private void renderCaseClassWithEnterAndExit(String returnType, String methodName, AstClass caseClass, String returnStatement) {
    String location = caseClass.locationProperty == null ? "null" : "valid(node." + caseClass.locationProperty + ")";
    append("  public " + returnType + " " + methodName + "(" + caseClass.wrappedAstClass.getCanonicalName() + " node) {\n");
    append("    enter(node);\n");
    if (!caseClass.properties.isEmpty()) {
      append("    visitChildren(node);\n");
    }
    append("    exit(node);\n");
    append(returnStatement);
    append("  }\n");
    append("\n");
    append("  protected void enter(" + caseClass.wrappedAstClass.getCanonicalName() + " node) {\n");
    append("    defaultEnter(node, " + location + ");\n");
    append("  }\n");
    append("\n");
    if (!caseClass.properties.isEmpty()) {
      renderVisitChildren(caseClass);
    }
    append("  protected void exit(" + caseClass.wrappedAstClass.getCanonicalName() + " node) {\n");
    append("    defaultExit(node, " + location + ");\n");
    append("  }\n");
    append("\n");
  }

  private void renderVisitChildren(AstClass caseClass) {
    append("  protected void visitChildren(" + caseClass.wrappedAstClass.getCanonicalName() + " node) {\n");
    for (AstProperty property : sorted(caseClass.properties, caseClass.wrappedAstClass)) {
      append("    prepareChildVisit(Property." + caseClass.wrappedAstClass.getSimpleName() + "." + property.enumKey() + ");\n");
      String propertyCaseMethod = lookupMethodName(ClassUtils.underlyingClass(property.type));
      if (property.isOptional) {
        if (property.isList) {
          append("    node." + property.name + ".ifPresent(list -> list.forEach(this::" + propertyCaseMethod + "));\n");
        } else {
          append("    node." + property.name + ".ifPresent(this::" + propertyCaseMethod + ");\n");
        }
      } else {
        if (property.isList) {
          append("    ofNullable(node." + property.name + ").ifPresent(list -> list.forEach(this::" + propertyCaseMethod + "));\n");
        } else {
          // defensive programming: sometimes ANTLR gives non-optional null elements
          append("    ofNullable(node." + property.name + ").ifPresent(this::" + propertyCaseMethod + ");\n");
        }
      }
    }
    append("  }\n");
    append("\n");
  }

  private String lookupMethodName(Class<?> cls) {
    CaseMethod caseMethod = caseMethodMap.get(cls);
    return caseMethod == null ? "_case" : caseMethod.name();
  }

}
