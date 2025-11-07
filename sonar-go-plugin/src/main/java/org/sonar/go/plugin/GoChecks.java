/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.go.api.checks.GoCheck;

public class GoChecks {
  private final CheckFactory checkFactory;
  // Visible for testing
  protected final List<Checks<GoCheck>> checksByRepository = new ArrayList<>();

  public GoChecks(CheckFactory checkFactory) {
    this.checkFactory = checkFactory;
  }

  public GoChecks addChecks(String repositoryKey, Iterable<Class<?>> checkClass) {
    checksByRepository.add(checkFactory.<GoCheck>create(repositoryKey).addAnnotatedChecks(checkClass));
    return this;
  }

  public List<GoCheck> all() {
    return checksByRepository.stream().flatMap(c -> c.all().stream()).toList();
  }

  @Nullable
  public RuleKey ruleKey(GoCheck check) {
    return checksByRepository.stream().map(c -> c.ruleKey(check)).filter(Objects::nonNull).findFirst().orElse(null);
  }
}
