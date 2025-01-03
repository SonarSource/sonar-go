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
package org.sonar.go.plugin;

import java.util.function.Predicate;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.go.converter.GoConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.checks.api.SlangCheck;

public class GoSensor extends SlangSensor {

  private final Checks<SlangCheck> checks;

  private ASTConverter goConverter = null;

  public GoSensor(SonarRuntime sonarRuntime, CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory,
    NoSonarFilter noSonarFilter, GoLanguage language, GoConverter goConverter) {
    super(sonarRuntime, noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(GoRulesDefinition.REPOSITORY_KEY);
    checks.addAnnotatedChecks(GoCheckList.checks());
    this.goConverter = goConverter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(GoLanguage.KEY)
      .name("Code Quality and Security for Go");
  }

  @Override
  protected ASTConverter astConverter(SensorContext sensorContext) {
    return goConverter;
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

  @Override
  protected String repositoryKey() {
    return GoRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected Predicate<Tree> executableLineOfCodePredicate() {
    return super.executableLineOfCodePredicate().and(t -> !(t instanceof VariableDeclarationTree)
      && !isGenericDeclaration(t));
  }

  private static boolean isGenericDeclaration(Tree tree) {
    return tree instanceof NativeTree &&
      ((NativeTree) tree).nativeKind().toString().contains("GenDecl");
  }
}
