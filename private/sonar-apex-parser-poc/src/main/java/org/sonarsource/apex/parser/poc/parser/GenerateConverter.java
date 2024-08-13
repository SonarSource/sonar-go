package org.sonarsource.apex.parser.poc.parser;

import apex.jorje.data.ast.CompilationUnit;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GenerateConverter {

  public static class ClassQueue {

    Deque<Class<?>> classQueue = new LinkedList<>();
    Set<Class<?>> knownClass = new HashSet<>();

    public void pushIfNotExists(Class<?> cls) {
      if (knownClass.add(cls)) {
        classQueue.push(cls);
      }
    }

    public Class<?> pop() {
      return classQueue.pop();
    }

    public boolean isEmpty() {
      return classQueue.isEmpty();
    }
  }

  public static void main(String[] args) throws IOException {

    Map<Class<?>, CaseMethod> caseMethodMap = new HashMap<>();

    ClassQueue classQueue = new ClassQueue();
    classQueue.pushIfNotExists(CompilationUnit.class);
    while (!classQueue.isEmpty()) {
      Class<?> cls = classQueue.pop();
      AstClass astClass = new AstClass(cls);
      astClass.referencedClasses.forEach(classQueue::pushIfNotExists);
      if (astClass.switchMethod != null) {
        astClass.switchMethod.caseMethods.forEach(caseMethod -> caseMethodMap.merge(caseMethod.parameterClass(), caseMethod, (a, b) -> {
          throw new IllegalStateException("Incompatible caseMethod " + a.name() + " and " + b.name() + " on" + cls.getCanonicalName());
        }));
      }
    }

    Path target = Paths.get("src/main/java/org/sonarsource/apex/parser/poc/parser/SimpleConverterGenerated.java");
    StringBuilder code = new StringBuilder();
    code.append("package org.sonarsource.apex.parser.poc.parser;\n");
    code.append("\n");
    code.append("import org.sonarsource.apex.parser.poc.lexer.Tokens;\n");
    code.append("\n");
    code.append("import static java.util.Optional.ofNullable;\n");
    code.append("\n");
    code.append("public class SimpleConverterGenerated extends SimpleConverterBase");
    code.append(classQueue.knownClass.stream()
      .map(AstClass::new)
      .map(cls -> cls.switchMethod)
      .filter(Objects::nonNull)
      .map(method -> method.switchInterfaceTypeString())
      .collect(Collectors.toSet()).stream()
      .sorted()
      .collect(Collectors.joining(",\n  ", " implements\n  ", "")));

    code.append(" {\n");
    code.append("\n");
    code.append("  public SimpleConverterGenerated(Tokens tokens) {\n");
    code.append("    super(tokens);\n");
    code.append("  }\n");
    code.append("\n");

    classQueue.knownClass.stream()
      .sorted(Comparator.comparing(Class::getCanonicalName))
      .forEach(cls -> renderCaseClass(code, new AstClass(cls), caseMethodMap));

    code.append("}\n");
    Files.write(target, code.toString().getBytes(UTF_8));
  }

  private static void renderCaseClass(StringBuilder code, AstClass caseClass, Map<Class<?>, CaseMethod> caseMethodMap) {
    CaseMethod caseMethod = caseMethodMap.get(caseClass.wrapped);
    String methodName = lookupMethodName(caseClass.wrapped, caseMethodMap);
    boolean isPrimitiveVoid = caseMethod == null || rawEqual(caseMethod.returnType(), void.class);
    String returnType = isPrimitiveVoid ? "void" : "Void";
    String returnStatement = isPrimitiveVoid ? "" : "    return null;\n";
    code.append("  public " + returnType + " " + methodName + "(" + caseClass.wrapped.getCanonicalName() + " node) {\n");
    if (caseClass.switchMethod != null) {
      code.append("    node." + caseClass.switchMethod.switchMethodName() + "(this);\n");
    } else {
      String location = caseClass.locationProperty == null ? "null" : "node." + caseClass.locationProperty;
      code.append("    addNode(type(node), " + location + ", () -> {\n");

      for (AstProperty property : caseClass.properties) {
        String propertyCaseMethod = lookupMethodName(underlyingClass(property.type), caseMethodMap);
        if (property.isOptional) {
          if (property.isList) {
            code.append("      node." + property.name + ".ifPresent(list -> list.forEach(this::" + propertyCaseMethod + "));\n");
          } else {
            code.append("      node." + property.name + ".ifPresent(this::" + propertyCaseMethod + ");\n");
          }
        } else {
          if (property.isList) {
            code.append("      ofNullable(node." + property.name + ").ifPresent(list -> list.forEach(this::" + propertyCaseMethod + "));\n");
          } else {
            code.append("      ofNullable(node." + property.name + ").ifPresent(this::" + propertyCaseMethod + ");\n");
          }
        }
      }
      code.append("    });\n");
    }
    code.append(returnStatement);
    code.append("  }\n");
    code.append("\n");
  }

  private static String lookupMethodName(Class<?> cls, Map<Class<?>, CaseMethod> caseMethodMap) {
    CaseMethod caseMethod = caseMethodMap.get(cls);
    return caseMethod == null ? "_case" : caseMethod.name();
  }

  private static class AstClass {

    static final Set<Class<?>> TERMINAL_CLASS_WITH_LOCATION = new HashSet<>(Arrays.asList(
      apex.jorje.data.ast.Expr.SoslExpr.class,
      apex.jorje.data.ast.Expr.SoqlExpr.class));

    static final Set<Class<?>> IGNORED_REFERENCE_CLASSES = new HashSet<>(Arrays.asList(
      apex.jorje.data.ast.VersionRef.class,
      apex.jorje.data.ast.CompilationUnit.InvalidDeclUnit.class));

    static final Set<String> IGNORED_METHODS = new HashSet<>(Arrays.asList(
      "equals", "hashCode", "toString", "negate", "match"));

    final Class<?> wrapped;

    String locationProperty = null;

    final List<AstProperty> properties;

    final Set<Class<?>> referencedClasses;

    SwitchMethod switchMethod;

    private AstClass(Class<?> wrapped) {

      this.wrapped = wrapped;
      properties = new ArrayList<>();
      referencedClasses = new HashSet<>();

      Arrays.stream(wrapped.getFields())
        .filter(AstClass::isPublicAndNotStatic)
        .forEach(field -> addProperty(field.getGenericType(), field.getName()));

      Arrays.stream(wrapped.getMethods())
        .filter(AstClass::isPublicAndNotStatic)
        .filter(AstClass::isGetter)
        .forEach(method -> addProperty(method.getGenericReturnType(), method.getName() + "()"));

      Arrays.stream(wrapped.getMethods())
        .filter(AstClass::isPublicAndNotStatic)
        .filter(AstClass::isNotGetter)
        .forEach(this::addMethod);

      if (switchMethod == null && properties.isEmpty() && locationProperty == null && !IGNORED_REFERENCE_CLASSES.contains(wrapped)) {
        throw new IllegalStateException("Missing switchMethod, properties and location on: " + wrapped.getCanonicalName());
      }

    }

    private void addReferencedType(Type type) {
      if (type instanceof TypeVariable || isPrimitiveOrJavaLang(type)) {
        return;
      }
      Type underlyingType = type;
      while (underlyingType instanceof ParameterizedType) {
        underlyingType = ((ParameterizedType) underlyingType).getRawType();
      }
      if (underlyingType instanceof Class && startWith(underlyingType, "apex.jorje.data.")) {
        Class underlyingClass = (Class) underlyingType;
        if (!underlyingClass.isEnum() && !IGNORED_REFERENCE_CLASSES.contains(underlyingClass)) {
          referencedClasses.add(underlyingClass);
        }
      } else if (underlyingType != void.class) {
        throw new IllegalStateException("Unexpected referenced type:(" + underlyingType.getClass().getCanonicalName() + ")" + type.getTypeName() +
          " on " + wrapped.getCanonicalName());
      }
    }

    private void addMethod(Method method) {
      if (TERMINAL_CLASS_WITH_LOCATION.contains(wrapped) || IGNORED_METHODS.contains(method.getName())) {
        return;
      }
      SwitchMethod switchMethod = SwitchMethod.from(method);
      if (switchMethod != null) {
        if (hasMethod(wrapped.getSuperclass(), method) || Arrays.stream(wrapped.getInterfaces()).anyMatch(i -> hasMethod(i, method))) {
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
        if (innerType.getTypeName().startsWith("java.util.Optional<")) {
          isOptional = true;
          innerType = ((ParameterizedType) innerType).getActualTypeArguments()[0];
        }
        if (innerType.getTypeName().startsWith("java.util.List<")) {
          isList = true;
          innerType = ((ParameterizedType) innerType).getActualTypeArguments()[0];
        }
        if (isEnum(innerType) || TERMINAL_CLASS_WITH_LOCATION.contains(wrapped) || isPrimitiveOrJavaLang(innerType) || IGNORED_REFERENCE_CLASSES.contains(innerType)) {
          return;
        }
        if (!(innerType instanceof Class)) {
          throw new IllegalStateException("Unsupported type: " + innerType.getTypeName());
        }
        String signature = innerType.toString() + " " + name;
        if (!signature.startsWith("class apex.jorje.data.ast.") &&
          !signature.startsWith("class apex.jorje.data.soql.FieldIdentifier") &&
          !signature.startsWith("interface apex.jorje.data.ast.TypeRef ") &&
          !signature.startsWith("interface apex.jorje.data.ast.ParameterRef ") &&
          !signature.startsWith("interface apex.jorje.data.Identifier ")) {
          System.out.println(signature + " on " + wrapped.getCanonicalName());
        }
        addReferencedType(innerType);
        properties.add(new AstProperty(isOptional, isList, innerType, name));
      }
      // TODO LiteralExpr.literal and LiteralExpr.type
    }

    private boolean isPrimitiveOrJavaLang(Type type) {
      return startWith(type, "java.lang.") ||
        (type instanceof Class && ((Class) type).isPrimitive());
    }

    private static boolean isNotGetter(Method method) {
      return !isGetter(method);
    }

    private static boolean isGetter(Method method) {
      return (method.getName().startsWith("get") || method.getName().startsWith("is")) &&
        method.getParameterCount() == 0 && !method.getReturnType().equals(void.class);
    }

    private static boolean isPublicAndNotStatic(Field field) {
      return isPublicAndNotStatic(field.getModifiers());
    }

    private static boolean isPublicAndNotStatic(Method method) {
      return isPublicAndNotStatic(method.getModifiers());
    }

    private static boolean isPublicAndNotStatic(int modifiers) {
      return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

  }

  private static class AstProperty {

    private final boolean isOptional;
    private final boolean isList;
    private final Type type;
    private final String name;

    private AstProperty(boolean isOptional, boolean isList, Type type, String name) {
      this.isOptional = isOptional;
      this.isList = isList;
      this.type = type;
      this.name = name;
    }

  }

  private static class SwitchMethod {

    static final Set<String> SWITCH_METHODS = new HashSet<>(Arrays.asList(
      "_switch", "accept"));

    final Method switchMethod;
    final List<CaseMethod> caseMethods;

    public SwitchMethod(Method method) {
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
        startWith(parameterTypes[0], "apex.jorje.data.")) {
        return new SwitchMethod(method);
      }
      return null;
    }

    Class<?> declaringClass() {
      return switchMethod.getDeclaringClass();
    }

    String switchMethodName() {
      return switchMethod.getName();
    }

    Class<?> switchParameterType() {
      return switchMethod.getParameterTypes()[0];
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

  private static class CaseMethod {
    static final Set<String> CASE_METHODS = new HashSet<>(Arrays.asList(
      "_case", "visit"));

    Method method;
    String compareString;

    public CaseMethod(Method method) {
      this.method = method;
      if (!CASE_METHODS.contains(method.getName()) || method.getParameterCount() != 1) {
        throw new IllegalStateException("Unexpected case method: " + method.toGenericString());
      }
      compareString = name() + "(" + parameterClass().getCanonicalName() + ")";
    }

    public String name() {
      return method.getName();
    }

    public Type returnType() {
      return method.getGenericReturnType();
    }

    public Type parameterType() {
      return method.getParameterTypes()[0];
    }

    public Class<?> parameterClass() {
      Class<?> cls = underlyingClass(parameterType());
      if (cls == null) {
        throw new IllegalStateException("Unexpected case method parameter: " + method.toGenericString());
      }
      return cls;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CaseMethod that = (CaseMethod) o;
      return Objects.equals(compareString, that.compareString);
    }

    @Override
    public int hashCode() {
      return Objects.hash(compareString);
    }
  }

  private static Class<?> underlyingClass(Type type) {
    Type underlyingType = type;
    while (underlyingType instanceof ParameterizedType) {
      underlyingType = ((ParameterizedType) underlyingType).getRawType();
    }
    if (underlyingType instanceof Class) {
      return (Class) underlyingType;
    }
    return null;
  }

  private static boolean hasMethod(Class<?> cls, Method method) {
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

  private static boolean rawEqual(Type a, Type b) {
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

  private static boolean startWith(Type type, String packageName) {
    Class<?> cls = underlyingClass(type);
    if (cls != null) {
      return cls.getCanonicalName().startsWith(packageName);
    }
    return false;
  }

  private static boolean isEnum(Type type) {
    Class<?> cls = underlyingClass(type);
    if (cls != null) {
      return cls.isEnum();
    }
    return false;
  }

}
