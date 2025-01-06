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
package org.sonar.go.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  static final Class[] GO_CHECK_BLACK_LIST = {
    BadClassNameCheck.class,
    // Can not enable rule S1066, as Go if-trees are containing an initializer, not well handled by SLang
    CollapsibleIfStatementsCheck.class,
    TabsCheck.class,
    // Can not enable rule S1172 since it it not possible to identify overridden function with modifier (to avoid FP)
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class,
    UnusedPrivateMethodCheck.class,
    // Replaced by language specific test
    CodeAfterJumpCheck.class,
    DuplicateBranchCheck.class,
    OneStatementPerLineCheck.class
  };

  private static final Collection<Class<?>> GO_LANGUAGE_SPECIFIC_CHECKS = Arrays.asList(
    CodeAfterJumpGoCheck.class,
    DuplicateBranchGoCheck.class,
    OneStatementPerLineGoCheck.class);

  public static List<Class<?>> checks() {
    List<Class<?>> list = new ArrayList<>(CheckList.excludeChecks(GO_CHECK_BLACK_LIST));
    list.addAll(GO_LANGUAGE_SPECIFIC_CHECKS);
    return list;
  }
}
