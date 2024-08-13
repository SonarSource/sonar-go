/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S5378")
public class QueriesSharingUserPermissionsCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classTree) -> findUnsafeModifier(classTree)
      .ifPresent(unsafeModifier -> {
        List<Tree> queries = classTree.descendants()
          .filter(ExpressionUtils::isQuery)
          .collect(Collectors.toList());

        queries.removeAll(getNestedQueries(queries));

        queries.forEach(query -> ctx.reportIssue(
          query,
          getMessage(unsafeModifier),
          new SecondaryLocation(unsafeModifier, "permissions modifier")));
      }));
  }

  private static String getMessage(NativeTree modifierTree) {
    String modifiers = "WithoutSharingModifier".equals(modifierTree.nativeKind().toString()) ? "without sharing" : "with inherited sharing";
    return "Make sure that executing SOQL, SOSL or DML queries " + modifiers + " is safe here";
  }

  private static Optional<NativeTree> findUnsafeModifier(ClassDeclarationTree classTree) {
    return firstNativeChild(classTree.classTree(), "WithoutSharingModifier")
      .map(Optional::of)
      .orElseGet(() -> firstNativeChild(classTree.classTree(), "InheritedSharingModifier"));
  }

  private static Optional<NativeTree> firstNativeChild(Tree parent, String nativeChildKind) {
    return parent.children().stream()
      .filter(NativeTree.class::isInstance)
      .map(NativeTree.class::cast)
      .filter(nativeTree -> nativeChildKind.equals(nativeTree.nativeKind().toString()))
      .findFirst();
  }

  private static List<Tree> getNestedQueries(List<Tree> queries) {
    return queries.stream()
      .filter(ExpressionUtils::isDMLQuery)
      .flatMap(Tree::descendants)
      .filter(ExpressionUtils::isQuery)
      .collect(Collectors.toList());
  }

}
