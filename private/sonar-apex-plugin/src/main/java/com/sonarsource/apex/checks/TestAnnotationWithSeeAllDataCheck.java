/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.apex.checks.utils.ExpressionUtils.isNativeTreeKind;

@Rule(key = "S5395")
public class TestAnnotationWithSeeAllDataCheck implements SlangCheck {

  private static final String MESSAGE = "Remove \"SeeAllData = true\" from \"@isTest\"";

  private static final String SEE_ALL_DATA_KEY = "seealldata";
  private static final String ANNOTATION_KEY_VALUE_KIND = "AnnotationKeyValue";
  private static final String TRUE_ANNOTATION_VALUE_KIND = "TrueAnnotationValue";

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classDeclarationTree) ->
      reportSeeAllDataTrueAnnotation(ctx, classDeclarationTree.classTree().children())
    );

    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) ->
      reportSeeAllDataTrueAnnotation(ctx, functionDeclarationTree.modifiers())
    );
  }

  private static void reportSeeAllDataTrueAnnotation(CheckContext ctx, List<Tree> trees) {
    trees.stream()
    .filter(ExpressionUtils::isTestAnnotation)
    .forEach(
      testAnnotation -> reportKeyValueSeeAllDataTrue(ctx, testAnnotation)
    );
  }

  private static void reportKeyValueSeeAllDataTrue(CheckContext ctx, Tree testAnnotation) {
    testAnnotation.children().stream()
      .filter(child -> isNativeTreeKind(child, ANNOTATION_KEY_VALUE_KIND))
      .forEach(annotationKeyValue -> {
        if (isSeeAllDataTrue(annotationKeyValue)) {
          ctx.reportIssue(annotationKeyValue, MESSAGE);
        }
      });
  }

  private static boolean isSeeAllDataTrue(Tree annotationKeyValue) {
    return annotationKeyValue.children().stream()
      .filter(child -> isSeeAllData(child) || isNativeTreeKind(child, TRUE_ANNOTATION_VALUE_KIND))
      .count() == 2;
  }

  private static boolean isSeeAllData(Tree tree) {
    return tree instanceof IdentifierTree && SEE_ALL_DATA_KEY.equals(((IdentifierTree) tree).identifier());
  }
}
