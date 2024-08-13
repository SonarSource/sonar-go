/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

public final class ExpressionUtils {

  private static final String TEST_METHOD_MODIFIER_KIND = "TestMethodModifier";
  private static final String ANNOTATION_KIND = "Annotation";
  private static final String TEST_ANNOTATION_NAME = "isTest";
  private static final String TRIGGER_DECL_KIND = "TriggerDeclUnit";

  private static final Set<String> SOQL_SOSL_KINDS = new HashSet<>(Arrays.asList(
    // SOQL (Salesforce Object Query Language)
    "SoqlExpr",
    // SOSL (Salesforce Object Search Language)
    "SoslExpr"
  ));

  private static final Set<String> DML_KINDS = new HashSet<>(Arrays.asList(
    "DmlDeleteStmnt",
    "DmlInsertStmnt",
    "DmlMergeStmnt",
    "DmlUndeleteStmnt",
    "DmlUpdateStmnt",
    "DmlUpsertStmnt"
  ));

  private ExpressionUtils() {
    // utility class
  }

  public static boolean isQuery(Tree tree) {
    if (tree instanceof NativeTree) {
      String nativeKindName = ((NativeTree) tree).nativeKind().toString();
      return SOQL_SOSL_KINDS.contains(nativeKindName) ||
        DML_KINDS.contains(nativeKindName);
    }
    return false;
  }

  public static boolean isDMLQuery(Tree tree) {
    return tree instanceof NativeTree && DML_KINDS.contains(((NativeTree) tree).nativeKind().toString());
  }

  public static boolean isTrigger(NativeTree tree) {
    return TRIGGER_DECL_KIND.equals(tree.nativeKind().toString());
  }

  public static boolean isTestClass(ClassDeclarationTree classDeclarationTree) {
    return classDeclarationTree.classTree().children().stream().anyMatch(ExpressionUtils::isTestAnnotation);
  }

  public static boolean isTestFunction(FunctionDeclarationTree tree) {
    return tree.modifiers().stream().anyMatch(mod -> isTestModifier(mod) || isTestAnnotation(mod));
  }

  private static boolean isTestModifier(Tree tree) {
    return isNativeTreeKind(tree, TEST_METHOD_MODIFIER_KIND);
  }

  public static boolean isTestAnnotation(Tree tree) {
    return isNativeTreeKind(tree, ANNOTATION_KIND)
      && tree.children().stream()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .anyMatch(TEST_ANNOTATION_NAME::equalsIgnoreCase);
  }

  public static boolean isNativeTreeKind(Tree tree, String kind) {
    return tree instanceof NativeTree && kind.equals(((NativeTree) tree).nativeKind().toString());
  }
}
