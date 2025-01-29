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
package org.sonar.go.checks.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.MemberSelectTree;
import org.sonar.go.api.NativeTree;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.checks.NativeKinds;

public class MethodMatchers {
  private final String type;
  private final String packageName;
  private final Predicate<String> namePredicate;
  private final Predicate<List<String>> parametersPredicate;

  private final Set<String> imports = new HashSet<>();

  private MethodMatchers(String type, Predicate<String> namePredicate, Predicate<List<String>> parametersPredicate) {
    this.type = type;
    this.packageName = type.substring(type.lastIndexOf('/') + 1);
    this.namePredicate = namePredicate;
    this.parametersPredicate = parametersPredicate;
  }

  public static TypeBuilder create() {
    return new MethodMatchersBuilder();
  }

  public void addImports(TopLevelTree topLevelTree) {
    imports.clear();
    imports.addAll(TreeUtils.getImportsAsStrings(topLevelTree));
  }

  public Optional<IdentifierTree> matches(Tree tree) {
    if (imports.contains(type) && NativeKinds.isFunctionCall(tree)) {
      return tree.children().stream()
        .filter(MemberSelectTree.class::isInstance)
        .map(MemberSelectTree.class::cast)
        .findFirst()
        .flatMap(this::getMethodIdentifierIfMatches)
        .filter(i -> parametersPredicate.test(extractArgTypes((NativeTree) tree)));
    }
    return Optional.empty();
  }

  private Optional<IdentifierTree> getMethodIdentifierIfMatches(MemberSelectTree memberSelectTree) {
    return Optional.of(memberSelectTree)
      .filter(m -> m.expression() instanceof IdentifierTree currentPackageName && packageName.equals(currentPackageName.name()))
      .map(MemberSelectTree::identifier)
      .filter(i -> namePredicate.test(i.name()));
  }

  private static List<String> extractArgTypes(NativeTree nativeTree) {
    // Check if we have four elements: the method name, opening parenthesis, arguments, and closing parenthesis.
    if (nativeTree.children().size() == 4) {
      var args = (NativeTree) nativeTree.children().get(2);
      return args.children().stream()
        // We don't have any type information at this point.
        .map(arg -> "UNKNOWN")
        .toList();
    }
    return Collections.emptyList();
  }

  public interface TypeBuilder {
    NameBuilder ofType(String type);
  }

  public interface NameBuilder {
    ParametersBuilder withName(String name);

    ParametersBuilder withNames(Collection<String> names);
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
    public ParametersBuilder withName(String name) {
      this.namePredicate = s -> s.equals(name);
      return this;
    }

    @Override
    public ParametersBuilder withNames(Collection<String> names) {
      this.namePredicate = names::contains;
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
