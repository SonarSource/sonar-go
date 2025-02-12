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
package org.sonar.go.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;

/**
 * Helps identify a method with given Type, Receiver, Name and Parameters.
 * <p>
 * The starting point to define a MethodMatchers is {@link #create()}.
 * <p>
 * It is required to provide the following:
 * <ul>
 *  <li> a type definition (import):
 *    <ul>
 *      <li> {@link TypeBuilder#ofType(String)} </li>
 *    </ul>
 *  </li>
 *  <li> a method name
 *    <ul>
 *      <li> {@link NameBuilder#withNames(String...)} </li>
 *      <li> {@link NameBuilder#withNames(Collection)} </li>
 *      <li> {@link NameBuilder#withPrefixAndNames(String, String...)} </li>
 *    </ul>
 *  </li>
 *  <li> and parameters:
 *    <ul>
 *      <li> {@link ParametersBuilder#withAnyParameters()} </li>
 *      <li> {@link ParametersBuilder#withParameters(Predicate)} </li>
 *    </ul>
 *  </li>
 * </ul>
 * Optional methods:
 * <ul>
 *   <li> {@link NameBuilder#withReceiver()} - by default no receiver is expected </li>
 * </ul>
 * <p>
 * Examples:
 * <p>
 * Example 1. Match method rand.Int() from "math/rand" package
 * <p>
 *   <pre>
 *     {@code
 *     var randIntMatcher = MethodMatchers.create()
 *       .ofType("math/rand")
 *       .withName("rand.Int")
 *       .withAnyParameters()
 *       .build();
 *     TopLevelTree topLevelTree = ...
 *     FunctionInvocationTree tree = ...
 *     randIntMatcher.addImports(topLevelTree);
 *     var matchedMethodOrEmpty = randIntMatcher.matches(tree);
 *     }
 *   </pre>
 * <p>
 *   Example 2. Match method Int() or Int31() from "math/rand" package
 * <p>
 *   <pre>
 *     {@code
 *     MethodMatchers.create()
 *       .ofType("math/rand")
 *       .withPrefixAndNames("rand", "Int", "Int31")
 *       .withAnyParameters()
 *       .build();
 *     }
 *   </pre>
 * <p>
 *   Example 3. Match method bar() called on object "foo" from "com/example" package
 *   <pre>
 *     {@code
 *     var fooBarMatcher = MethodMatchers.create()
 *       .ofType("com/example")
 *       .withReceiver()
 *       .withName("bar")
 *       .withAnyParameters()
 *       .build();
 *     }
 *     TopLevelTree topLevelTree = ...
 *     FunctionInvocationTree tree = ...
 *     fooBarMatcher.addImports(topLevelTree);
 *     // when withReceiver() is called then setReceiverName() needs to be called before matches()
 *     fooBarMatcher.setReceiverName("foo");
 *     var matchedMethodOrEmpty = randIntMatcher.matches(tree);
 *   </pre>
 * <p>
 */
public class MethodMatchers {
  private final String type;
  private final boolean withReceiver;
  private final Predicate<String> namePredicate;
  private final Predicate<List<String>> parametersPredicate;

  private final Set<String> imports = new HashSet<>();
  @Nullable
  private String methodReceiverName;

  private MethodMatchers(String type, boolean withReceiver, Predicate<String> namePredicate, Predicate<List<String>> parametersPredicate) {
    this.type = type;
    this.withReceiver = withReceiver;
    this.namePredicate = namePredicate;
    this.parametersPredicate = parametersPredicate;
  }

  public static TypeBuilder create() {
    return new MethodMatchersBuilder();
  }

  public void addImports(TopLevelTree topLevelTree) {
    addImports(TreeUtils.getImportsAsStrings(topLevelTree));
  }

  public void addImports(Set<String> importStrings) {
    imports.clear();
    imports.addAll(importStrings);
  }

  /**
   * Set the name of receiver. It needs to be called when {@code MethodMatchers} was created using
   * {@link NameBuilder#withReceiver()} method. Otherwise, the method will be not resolved.
   * <p>
   * When receiver is not available anymore (e.g. outside the function where it is declared) the
   * receiver name should be set to {@code null} to "reset" the value and not match anymore.
   * @throws IllegalArgumentException when the receiver is not expected in {@code MethodMatcher}.
   */
  public void setReceiverName(@Nullable String methodReceiverName) {
    if (!withReceiver) {
      var message = "Setting receiver name, when MethodMatcher is not configured to expect receiver, doesn't make sense.";
      throw new IllegalArgumentException(message);
    }
    this.methodReceiverName = methodReceiverName;
  }

  public Optional<IdentifierTree> matches(@Nullable Tree tree) {
    if (imports.contains(type)
      && tree instanceof FunctionInvocationTree functionInvocation
      && matchesFunctionInvocation(functionInvocation)
      && parametersPredicate.test(extractArgTypes(functionInvocation))) {

      return Optional.of(functionInvocation.memberSelect())
        .filter(MemberSelectTree.class::isInstance)
        .map(MemberSelectTree.class::cast)
        .map(MemberSelectTree::identifier);
    }
    return Optional.empty();
  }

  private boolean matchesFunctionInvocation(FunctionInvocationTree functionInvocation) {
    if (!withReceiver) {
      return MethodCall.of(functionInvocation).is(namePredicate);
    }
    if (methodReceiverName == null) {
      return false;
    }
    var methodFqn = MethodCall.of(functionInvocation).methodFqn();
    if (methodFqn.contains(".")) {
      var firstPart = methodFqn.substring(0, methodFqn.indexOf("."));
      var secondPart = methodFqn.substring(Math.min(methodFqn.indexOf(".") + 1, methodFqn.length() - 1));
      return firstPart.equals(methodReceiverName) && namePredicate.test(secondPart);
    }
    return false;
  }

  private static List<String> extractArgTypes(FunctionInvocationTree functionInvocation) {
    // We don't have any type information at this point.
    return functionInvocation.arguments().stream().map(arg -> "UNKNOWN").toList();
  }

  public interface TypeBuilder {
    NameBuilder ofType(String type);
  }

  public interface NameBuilder {
    ParametersBuilder withNames(Collection<String> names);

    ParametersBuilder withNames(String... names);

    ParametersBuilder withPrefixAndNames(String commonPrefix, String... names);

    NameBuilder withReceiver();
  }

  public interface ParametersBuilder {
    ParametersBuilder withAnyParameters();

    /**
     * Here, the String are referring to the types of the parameters, not the actual String representation of the arguments.
     * Multiples method adding parameters matcher can be called to match multiples signatures (perform an "Or").
     */
    ParametersBuilder withParameters(Predicate<List<String>> parametersPredicate);

    MethodMatchers build();
  }

  public static class MethodMatchersBuilder implements TypeBuilder, NameBuilder, ParametersBuilder {
    private String type;
    private Predicate<String> namePredicate;
    private boolean methodReceiver = false;
    private Predicate<List<String>> parametersPredicate;

    @Override
    public NameBuilder ofType(String type) {
      this.type = type;
      return this;
    }

    @Override
    public ParametersBuilder withNames(Collection<String> names) {
      validateNames(names);
      this.namePredicate = names::contains;
      return this;
    }

    private static void validateNames(Collection<String> names) {
      var nameWithoutDot = names.stream()
        .filter(s -> !s.contains("."))
        .findAny();
      if (nameWithoutDot.isPresent()) {
        var message = "The method resolution \"%s\" doesn't contain a dot. Detection on local method is not supported yet.".formatted(nameWithoutDot.get());
        throw new IllegalArgumentException(message);
      }
    }

    @Override
    public ParametersBuilder withNames(String... names) {
      return withNames(Arrays.asList(names));
    }

    @Override
    public ParametersBuilder withPrefixAndNames(String commonPrefix, String... names) {
      var namesWithPrefix = Arrays.stream(names).map(name -> commonPrefix + "." + name).collect(Collectors.toSet());
      this.namePredicate = namesWithPrefix::contains;
      return this;
    }

    @Override
    public NameBuilder withReceiver() {
      methodReceiver = true;
      return this;
    }

    @Override
    public ParametersBuilder withAnyParameters() {
      return withParameters(s -> true);
    }

    @Override
    public ParametersBuilder withParameters(Predicate<List<String>> parametersPredicate) {
      if (this.parametersPredicate != null) {
        this.parametersPredicate = this.parametersPredicate.or(parametersPredicate);
      } else {
        this.parametersPredicate = parametersPredicate;
      }
      return this;
    }

    @Override
    public MethodMatchers build() {
      return new MethodMatchers(type, methodReceiver, namePredicate, parametersPredicate);
    }
  }
}
