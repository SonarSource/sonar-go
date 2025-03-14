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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.go.api.FunctionInvocationTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
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
 *       .withName("Int")
 *       .withAnyParameters()
 *       .build();
 *     TopLevelTree topLevelTree = ...
 *     FunctionInvocationTree tree = ...
 *     randIntMatcher.validateTypeInContext(topLevelTree);
 *     var matchedMethodOrEmpty = randIntMatcher.matches(tree);
 *     }
 *   </pre>
 * <p>
 *   Example 2. Match method rand.Int() or rand.Int31() from "math/rand" package
 * <p>
 *   <pre>
 *     {@code
 *     MethodMatchers.create()
 *       .ofType("math/rand")
 *       .withNames("Int", "Int31")
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
 *   Example 4. Match method bar() called on object of type "example.foo" from "com/example" package
 *   <pre>
 *     {@code
 *     var fooBarMatcher = MethodMatchers.create()
 *       .ofType("com/example")
 *       .withVariableTypeIn("foo")
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
  private final Collection<String> types;
  private final boolean withReceiver;
  private final Predicate<String> namePredicate;
  private final Predicate<List<String>> parametersTypePredicate;
  private final Map<Integer, Predicate<Tree>> parametersTreePredicate;
  private final Predicate<String> variableTypePredicate;

  @Nullable
  private String methodReceiverName;

  private MethodMatchers(Collection<String> types, boolean withReceiver, Predicate<String> namePredicate, Predicate<List<String>> parametersTypePredicate,
    Map<Integer, Predicate<Tree>> parametersTreePredicate, Predicate<String> variableTypePredicate) {
    this.types = types;
    this.withReceiver = withReceiver;
    this.namePredicate = namePredicate;
    this.parametersTypePredicate = parametersTypePredicate;
    this.parametersTreePredicate = parametersTreePredicate;
    this.variableTypePredicate = variableTypePredicate;
  }

  public static TypeBuilder create() {
    return new MethodMatchersBuilder();
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
    if (tree instanceof FunctionInvocationTree functionInvocation
      && matchesFunctionInvocation(functionInvocation)
      && parametersTypePredicate.test(extractArgTypes(functionInvocation))
      && matchParametersTreePredicate(functionInvocation)) {

      return retrieveLastIdentifier(functionInvocation.memberSelect());
    }
    return Optional.empty();
  }

  private static Optional<IdentifierTree> retrieveLastIdentifier(Tree tree) {
    if (tree instanceof MemberSelectTree memberSelectTree) {
      return Optional.of(memberSelectTree.identifier());
    } else if (tree instanceof IdentifierTree identifierTree) {
      return Optional.of(identifierTree);
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
      var optFirstIdentifier = retrieveFirstIdentifier(memberSelectTree);
      if (optFirstIdentifier.isPresent()) {
        var firstIdentifier = optFirstIdentifier.get();
        return matchesFunctionMemberSelectTree(memberSelectTree, firstIdentifier);
      }
    } else if (functionNameTree instanceof IdentifierTree identifierTree
      && types.contains(identifierTree.packageName())
      && !withReceiver) {
        // spotless:off
      // spotless indent this line by 2 spaces what cause java:S1120
      return namePredicate.test(identifierTree.name());
      // spotless:on
      }

    return false;
  }

  private boolean matchesFunctionMemberSelectTree(MemberSelectTree memberSelectTree, IdentifierTree firstIdentifier) {
    var subMethodName = subMethodName(memberSelectTree);
    if (withReceiver) {
      return firstIdentifier.name().equals(methodReceiverName) && namePredicate.test(subMethodName);
    } else if (matchVariable(firstIdentifier)) {
      return namePredicate.test(subMethodName);
    }

    if (types.contains(firstIdentifier.packageName())) {
      // Testing both the method name without the first identifier (normal or import package with an alias) and the method name with the first
      // identifier (package imported with a dot)
      return namePredicate.test(subMethodName) || namePredicate.test(firstIdentifier.name() + "." + subMethodName);
    }
    return false;
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
    return symbol != null && variableTypePredicate.test(identifier.type());
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

    ParametersBuilder withNamesMatching(Predicate<String> namePredicate);

    /**
     * This method can be replaced by {@link #withVariableTypeIn(String...)} and the replacement should be preferred.
     * There are cases where usage of {@link #withReceiver()} is necessary, e.g.:
     * <pre>
     *   {@code
     *   func (ctrl *MainController) sensitive_beego1() {
     *     ctrl.Ctx.SetCookie("name1", "value1", 200, "/", "example.com", false, false)
     *   }
     *   }
     * </pre>
     * Here the {@code ctrl} may be defined in another file and types from {@link #withVariableTypeIn(String...)}
     * need match first identifier type.
     */
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
  }

  public static class MethodMatchersBuilder implements TypeBuilder, NameBuilder, ParametersBuilder {
    private List<String> types;
    private Predicate<String> namePredicate;
    private boolean methodReceiver = false;
    private Predicate<List<String>> parametersTypesPredicate;
    private Map<Integer, Predicate<Tree>> parametersTreePredicate = new HashMap<>();
    private Predicate<String> variableTypePredicate = v -> false;

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
      this.namePredicate = names::contains;
      return this;
    }

    @Override
    public ParametersBuilder withNames(String... names) {
      return withNames(Arrays.asList(names));
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
      var typesWithStar = Arrays.stream(types).map(t -> "*" + t);
      variableTypePredicate = Stream.concat(Arrays.stream(types), typesWithStar).collect(Collectors.toSet())::contains;
      return this;
    }

    @Override
    public MethodMatchers build() {
      if (parametersTypesPredicate == null) {
        // This can happen if only parametersTreePredicate is set
        parametersTypesPredicate = p -> true;
      }
      return new MethodMatchers(types, methodReceiver, namePredicate, parametersTypesPredicate, parametersTreePredicate,
        variableTypePredicate);
    }
  }
}
