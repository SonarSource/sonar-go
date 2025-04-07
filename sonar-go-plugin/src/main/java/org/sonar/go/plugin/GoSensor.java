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
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.go.checks.GoCheckList;
import org.sonar.go.converter.GoConverter;
import org.sonar.go.utils.NativeKinds;
import org.sonar.plugins.go.api.ASTConverter;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.GoCheck;

public class GoSensor extends SlangSensor {

  private final Checks<GoCheck> checks;

  private ASTConverter goConverter = null;

  public GoSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory,
    NoSonarFilter noSonarFilter, GoLanguage language, GoConverter goConverter) {
    super(noSonarFilter, fileLinesContextFactory, language);
    checks = initializeChecks(checkFactory);
    this.goConverter = goConverter;
  }

  GoSensor(Checks<GoCheck> checks, FileLinesContextFactory fileLinesContextFactory,
    NoSonarFilter noSonarFilter, GoLanguage language, GoConverter goConverter) {
    super(noSonarFilter, fileLinesContextFactory, language);
    this.checks = checks;
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

  protected Checks<GoCheck> initializeChecks(CheckFactory checkFactory) {
    var goChecks = checkFactory.<GoCheck>create(GoRulesDefinition.REPOSITORY_KEY);
    goChecks.addAnnotatedChecks(GoCheckList.checks());
    return goChecks;
  }

  @Override
  protected Checks<GoCheck> checks() {
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
    return NativeKinds.isStringNativeKind(tree, str -> str.contains("GenDecl"));
  }
}
