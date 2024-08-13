/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isTestClass;

@Rule(key = "S5377")
public class MissingSharingLevelCheck implements SlangCheck {

  private static final Set<String> SHARING_MODIFIERS = new HashSet<>(Arrays.asList(
    "WithSharingModifier",
    "WithoutSharingModifier",
    "InheritedSharingModifier"));

  // Native node to represent "extends MyClass".
  private static final String CLASS_TYPE_REF = "ClassTypeRef";

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classDeclarationTree) -> {
      IdentifierTree classIdentifier = classDeclarationTree.identifier();
      if (classIdentifier != null && !hasSharingModifier(classDeclarationTree.classTree()) && !isTestClass(classDeclarationTree)) {
        classDeclarationTree.descendants()
          .filter(ExpressionUtils::isQuery)
          .findFirst()
          .ifPresent(query ->
            ctx.reportIssue(
              classIdentifier,
              "Add \"with sharing\", \"without sharing\" or \"inherited sharing\"",
              new SecondaryLocation(query, "query")));
      }
    });
  }

  private static boolean hasSharingModifier(Tree parent) {
    return parent.children().stream()
      .filter(NativeTree.class::isInstance)
      .map(NativeTree.class::cast)
      .map(NativeTree::nativeKind)
      .map(Object::toString)
      .anyMatch(nativeKind ->
        SHARING_MODIFIERS.contains(nativeKind)
          // The sharing modifier can be on the parent class, to kill the noise, we consider it as having a sharing modifier.
          || CLASS_TYPE_REF.equals(nativeKind));
  }

}
