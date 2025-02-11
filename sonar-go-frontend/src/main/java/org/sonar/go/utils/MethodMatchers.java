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

public class MethodMatchers {
  private final String type;
  private final Predicate<String> namePredicate;
  private final Predicate<List<String>> parametersPredicate;

  private final Set<String> imports = new HashSet<>();

  private MethodMatchers(String type, Predicate<String> namePredicate, Predicate<List<String>> parametersPredicate) {
    this.type = type;
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

  public Optional<IdentifierTree> matches(@Nullable Tree tree) {
    if (imports.contains(type)
      && tree instanceof FunctionInvocationTree functionInvocation
      && MethodCall.of(functionInvocation).is(namePredicate)
      && parametersPredicate.test(extractArgTypes(functionInvocation))) {

      return Optional.of(functionInvocation.memberSelect())
        .filter(MemberSelectTree.class::isInstance)
        .map(MemberSelectTree.class::cast)
        .map(MemberSelectTree::identifier);
    }
    return Optional.empty();
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
      return new MethodMatchers(type, namePredicate, parametersPredicate);
    }
  }
}
