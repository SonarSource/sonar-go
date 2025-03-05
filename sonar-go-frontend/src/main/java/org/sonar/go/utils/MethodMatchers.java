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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;

import static org.sonar.go.utils.TreeUtils.retrieveFirstIdentifier;

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
 *      <li> {@link ParametersBuilder#withNumberOfParameters(int)} </li>
 *      <li> {@link ParametersBuilder#withParameterAtIndexMatching(int, Predicate)} </li>
 *    </ul>
 *  </li>
 * </ul>
 * Optional methods:
 * <ul>
 *   <li> {@link NameBuilder#withReceiver()} - by default no receiver is expected </li>
 *   <li> {@link VariableMatcherBuilder#withVariableTypeIn(String...)} - allow to specify the type of variable expected </li>
 *   <li> {@link VariableMatcherBuilder#withVariableResultFromMethodIn(String...)} - allow to specify the method call to look for in the symbol variable usage </li>
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
 *     randIntMatcher.validateTypeInContext(topLevelTree);
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
 *     fooBarMatcher.validateTypeInContext(topLevelTree);
 *     // when withReceiver() is called then setReceiverName() needs to be called before matches()
 *     fooBarMatcher.setReceiverName("foo");
 *     var matchedMethodOrEmpty = randIntMatcher.matches(tree);
 *   </pre>
 * <p>
 * <p>
 *   Example 4. Match method bar() called on object of type "example.foo" or result of method call "example.baz()" from "com/example" package
 *   <pre>
 *     {@code
 *     var fooBarMatcher = MethodMatchers.create()
 *       .ofType("com/example")
 *       .withVariableTypeIn("example.foo")
 *       .withVariableResultFromMethodIn("example.baz")
 *       .withName("bar")
 *       .withAnyParameters()
 *       .build();
 *     }
 *     TopLevelTree topLevelTree = ...
 *     FunctionInvocationTree tree = ...
 *     fooBarMatcher.validateTypeInContext(topLevelTree);
 *     var matchedMethodOrEmpty = randIntMatcher.matches(tree);
 *   </pre>
 * <p>
 */
public class MethodMatchers {
  private final List<String> types;
  private final boolean withReceiver;
  private final Predicate<String> namePredicate;
  private final Predicate<List<String>> parametersTypePredicate;
  private final Map<Integer, Predicate<Tree>> parametersTreePredicate;
  private final Predicate<String> variableTypePredicate;
  private final Predicate<String> variableMethodResultPredicate;

  @Nullable
  private String methodReceiverName;
  private boolean validateTypeInTree = false;

  private MethodMatchers(List<String> types, boolean withReceiver, Predicate<String> namePredicate, Predicate<List<String>> parametersTypePredicate,
    Map<Integer, Predicate<Tree>> parametersTreePredicate, Predicate<String> variableTypePredicate, Predicate<String> variableMethodResultPredicate) {
    this.types = types;
    this.withReceiver = withReceiver;
    this.namePredicate = namePredicate;
    this.parametersTypePredicate = parametersTypePredicate;
    this.parametersTreePredicate = parametersTreePredicate;
    this.variableTypePredicate = variableTypePredicate;
    this.variableMethodResultPredicate = variableMethodResultPredicate;
  }

  public static TypeBuilder create() {
    return new MethodMatchersBuilder();
  }

  /**
   * Make sure the type provided to the Method Matcher is present in the (top level) tree the matcher will be used.
   * This is typically expected to be called at runtime, at the beginning of the analysis of each file.
   * If the type is not present, none of the following call to {@link #matches(Tree)} will match.
   * If the type is present, no further validation will be done for the type.
   * It can be called multiple times to update the validation status.
   * This is obviously imprecise, as we are not testing the actual type of the given invocation.
   * This is an approximation, as we don't have proper types resolution for now.
   */
  public void validateTypeInTree(TopLevelTree topLevelTree) {
    this.validateTypeInTree = types.stream().anyMatch(topLevelTree::doesImportType);
  }

  /**
   * Set the name of receiver. It needs to be called when {@code MethodMatchers} was created using
   * {@link NameBuilder#withReceiver()} method. Otherwise, the method will be not resolved.
   * <p>
   * When receiver is not available anymore (e.g. outside the function where it is declared) the
   * receiver name should be set to {@code null} to "reset" the value and not match anymore.
   *
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
    if (validateTypeInTree
      && tree instanceof FunctionInvocationTree functionInvocation
      && matchesFunctionInvocation(functionInvocation)
      && parametersTypePredicate.test(extractArgTypes(functionInvocation))
      && matchParametersTreePredicate(functionInvocation)) {

      return Optional.of(functionInvocation.memberSelect())
        .filter(MemberSelectTree.class::isInstance)
        .map(MemberSelectTree.class::cast)
        .map(MemberSelectTree::identifier);
    }
    return Optional.empty();
  }

  private boolean matchParametersTreePredicate(FunctionInvocationTree functionInvocation) {
    for (var entry : parametersTreePredicate.entrySet()) {
      var arg = getArg(functionInvocation, entry.getKey());
      if (arg == null || !entry.getValue().test(arg)) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public static Tree getArg(FunctionInvocationTree tree, int index) {
    var args = tree.arguments();
    if (args.size() > index) {
      return args.get(index);
    }
    return null;
  }

  private boolean matchesFunctionInvocation(FunctionInvocationTree functionInvocation) {
    Tree functionNameTree = functionInvocation.memberSelect();

    if (functionNameTree instanceof MemberSelectTree memberSelectTree) {
      var firstIdentifier = retrieveFirstIdentifier(memberSelectTree);
      if (firstIdentifier.isPresent()) {
        if (withReceiver) {
          return firstIdentifier.get().name().equals(methodReceiverName) && namePredicate.test(subMethodName(memberSelectTree));
        } else if (matchVariable(firstIdentifier.get())) {
          return namePredicate.test(subMethodName(memberSelectTree));
        }
      }
    }

    return namePredicate.test(TreeUtils.treeToString(functionNameTree));
  }

  private static String subMethodName(MemberSelectTree memberSelectTree) {
    var listNames = new LinkedList<String>();
    Tree currentTree = memberSelectTree;
    while (currentTree instanceof MemberSelectTree memberSelect) {
      listNames.addFirst(memberSelect.identifier().name());
      currentTree = memberSelect.expression();
    }
    return String.join(".", listNames);
  }

  private boolean matchVariable(IdentifierTree identifier) {
    var symbol = identifier.symbol();
    return symbol != null && (variableTypePredicate.test(identifier.type()) ||
      variableTypePredicate.test(symbol.getType()) ||
      variableMethodResultPredicate.test(SymbolHelper.getLastAssignedMethodCall(symbol).orElse("")));
  }

  private static List<String> extractArgTypes(FunctionInvocationTree functionInvocation) {
    // TODO We don't have any type information at this point.
    return functionInvocation.arguments().stream().map(arg -> "UNKNOWN").toList();
  }

  public interface TypeBuilder {
    NameBuilder ofType(String type);

    NameBuilder ofTypes(Collection<String> types);
  }

  public interface NameBuilder extends VariableMatcherBuilder {
    ParametersBuilder withNames(Collection<String> names);

    ParametersBuilder withNames(String... names);

    ParametersBuilder withPrefixAndNames(String commonPrefix, String... names);

    ParametersBuilder withNamesMatching(Predicate<String> namePredicate);

    NameBuilder withReceiver();
  }

  public interface ParametersBuilder {
    ParametersBuilder withAnyParameters();

    /**
     * Here, the String are referring to the types of the parameters, not the actual String representation of the arguments.
     * Multiples method adding parameters matcher can be called to match multiples signatures (perform an "Or").
     */
    ParametersBuilder withParameters(Predicate<List<String>> parametersPredicate);

    ParametersBuilder withNumberOfParameters(int numberOfParameters);

    ParametersBuilder withParameterAtIndexMatching(int indexParameter, Predicate<Tree> predicate);

    MethodMatchers build();
  }

  public interface VariableMatcherBuilder {
    NameBuilder withVariableTypeIn(String... types);

    NameBuilder withVariableResultFromMethodIn(String... methodNames);
  }

  public static class MethodMatchersBuilder implements TypeBuilder, NameBuilder, ParametersBuilder {
    private List<String> types;
    private Predicate<String> namePredicate;
    private boolean methodReceiver = false;
    private Predicate<List<String>> parametersTypesPredicate;
    private Map<Integer, Predicate<Tree>> parametersTreePredicate = new HashMap<>();
    private boolean withVariable = false;
    private Predicate<String> variableTypePredicate = v -> false;
    private Predicate<String> variableMethodResultPredicate = v -> false;

    @Override
    public NameBuilder ofType(String type) {
      this.types = List.of(type);
      return this;
    }

    @Override
    public NameBuilder ofTypes(Collection<String> types) {
      this.types = List.copyOf(types);
      return this;
    }

    @Override
    public ParametersBuilder withNames(Collection<String> names) {
      validateNames(names);
      this.namePredicate = names::contains;
      return this;
    }

    private void validateNames(Collection<String> names) {
      if (!methodReceiver && !withVariable) {
        var nameWithoutDot = names.stream()
          .filter(s -> !s.contains("."))
          .findAny();
        if (nameWithoutDot.isPresent()) {
          var message = "The method resolution \"%s\" doesn't contain a dot. Detection on local method is not supported yet.".formatted(nameWithoutDot.get());
          throw new IllegalArgumentException(message);
        }
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
    public ParametersBuilder withNamesMatching(Predicate<String> namePredicate) {
      this.namePredicate = namePredicate;
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
      if (this.parametersTypesPredicate != null) {
        this.parametersTypesPredicate = this.parametersTypesPredicate.or(parametersPredicate);
      } else {
        this.parametersTypesPredicate = parametersPredicate;
      }
      return this;
    }

    @Override
    public ParametersBuilder withNumberOfParameters(int numberOfParameters) {
      if (this.parametersTypesPredicate != null) {
        this.parametersTypesPredicate = this.parametersTypesPredicate.or(p -> p.size() == numberOfParameters);
      } else {
        this.parametersTypesPredicate = p -> p.size() == numberOfParameters;
      }
      return this;
    }

    @Override
    public ParametersBuilder withParameterAtIndexMatching(int indexParameter, Predicate<Tree> predicate) {
      parametersTreePredicate.put(indexParameter, predicate);
      return this;
    }

    /**
     * Allow to detect the method call on a variable of a specific type.
     * This will match the specific type (e.g. {@code sql.DB}) but also a pointer to the type (e.g. {@code *sql.DB}).
     */
    @Override
    public NameBuilder withVariableTypeIn(String... types) {
      withVariable = true;
      var typesWithStar = Arrays.stream(types).map(t -> "*" + t);
      variableTypePredicate = Stream.concat(Arrays.stream(types), typesWithStar).collect(Collectors.toSet())::contains;
      return this;
    }

    @Override
    public NameBuilder withVariableResultFromMethodIn(String... methodNames) {
      withVariable = true;
      variableMethodResultPredicate = Set.of(methodNames)::contains;
      return this;
    }

    @Override
    public MethodMatchers build() {
      if (parametersTypesPredicate == null) {
        // This can happen if only parametersTreePredicate is set
        parametersTypesPredicate = p -> true;
      }
      return new MethodMatchers(types, methodReceiver, namePredicate, parametersTypesPredicate, parametersTreePredicate,
        variableTypePredicate, variableMethodResultPredicate);
    }
  }
}
