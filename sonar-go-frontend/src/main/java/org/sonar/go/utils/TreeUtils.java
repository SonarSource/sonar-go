/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.MemberSelectTree;
import org.sonar.plugins.go.api.Tree;

public class TreeUtils {
  private TreeUtils() {
    // empty, util class
  }

  public static final Predicate<Tree> IS_NOT_SEMICOLON = Predicate.not(tree -> NativeKinds.isStringNativeKindOfType(tree, "Semicolon"));
  public static final Predicate<Tree> IS_NOT_EMPTY_NATIVE_TREE = Predicate.not(tree -> NativeKinds.isStringNativeKindOfType(tree, ""));

  public static <T extends Tree> List<String> getIdentifierNames(List<T> trees) {
    return trees.stream().filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .toList();
  }

  public static <T extends Tree> String getIdentifierName(List<T> trees) {
    return trees.stream().filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .collect(Collectors.joining("."));
  }

  /**
   * Used mainly for {@link MemberSelectTree}, to get the first identifier, ignoring others. E.g.:
   * <pre>
   * {@code
   * a.b    -> a
   * a.b.c  -> a
   * a      -> a
   * 5      -> null
   * }
   * </pre>
   */
  public static Optional<IdentifierTree> retrieveFirstIdentifier(Tree tree) {
    if (tree instanceof IdentifierTree identifierTree) {
      return Optional.of(identifierTree);
    } else if (tree instanceof MemberSelectTree memberSelectTree) {
      return retrieveFirstIdentifier(memberSelectTree.expression());
    } else if (tree instanceof FunctionInvocationTree functionInvocationTree) { // something.call1().call2()
      return retrieveFirstIdentifier(functionInvocationTree.memberSelect());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Matches an assignment to the field {@code fieldName} of a variable, e.g. {@code server.Host = "acme.com"}, whose
   * assigned value satisfies {@code assignedValuePredicate}.
   *
   * @return the variable the field belongs to, e.g. {@code server}, or empty when the assignment does not match
   */
  public static Optional<IdentifierTree> receiverOfFieldAssignment(AssignmentExpressionTree assignment, String fieldName,
    Predicate<Tree> assignedValuePredicate) {
    return receiverOfFieldAssignment(assignment, null, fieldName, assignedValuePredicate);
  }

  /**
   * Same as {@link #receiverOfFieldAssignment(AssignmentExpressionTree, String, Predicate)}, additionally requiring the
   * variable to be of {@code type}, ignoring pointers. A {@code null} type accepts any variable.
   */
  public static Optional<IdentifierTree> receiverOfFieldAssignment(AssignmentExpressionTree assignment, @Nullable String type,
    String fieldName, Predicate<Tree> assignedValuePredicate) {
    if (assignment.leftHandSide() instanceof MemberSelectTree memberSelect &&
      memberSelect.expression() instanceof IdentifierTree receiver &&
      memberSelect.identifier().name().equals(fieldName) &&
      (type == null || ExpressionUtils.hasTypeIgnoringStar(receiver, type)) &&
      assignedValuePredicate.test(assignment.statementOrExpression())) {
      return Optional.of(receiver);
    }
    return Optional.empty();
  }

  public static Optional<IdentifierTree> retrieveLastIdentifier(Tree tree) {
    if (tree instanceof MemberSelectTree memberSelectTree) {
      return Optional.of(memberSelectTree.identifier());
    } else if (tree instanceof IdentifierTree identifierTree) {
      return Optional.of(identifierTree);
    }
    return Optional.empty();
  }
}
