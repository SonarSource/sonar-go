/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.go.visitors.TreeVisitor;
import org.sonar.plugins.go.api.GoInputFile;
import org.sonar.plugins.go.api.HasTextRange;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.GoModFileData;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

public class ChecksVisitor extends TreeVisitor<InputFileContext> {

  private final DurationStatistics statistics;

  private final GoModFileDataStore goModFileDataStore;

  public ChecksVisitor(GoChecks goChecks, DurationStatistics statistics, GoModFileDataStore goModFileDataStore) {
    this.statistics = statistics;
    this.goModFileDataStore = goModFileDataStore;
    Collection<GoCheck> rulesActiveInSonarQube = goChecks.all();
    for (GoCheck check : rulesActiveInSonarQube) {
      var ruleKey = goChecks.ruleKey(check);
      Objects.requireNonNull(ruleKey);
      check.initialize(new ContextAdapter(ruleKey));
    }
  }

  public class ContextAdapter implements InitContext, CheckContext {

    public final RuleKey ruleKey;
    private InputFileContext currentCtx;
    private GoModFileData currentGoModFileData;

    public ContextAdapter(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public GoModFileData goModFileData() {
      return currentGoModFileData;
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> visitor) {
      ChecksVisitor.this.register(cls, statistics.time(ruleKey.rule(), (ctx, tree) -> {
        currentCtx = ctx;
        currentGoModFileData = goModFileDataStore.retrieveClosestGoModFileData(currentCtx.goInputFile.uri());
        visitor.accept(this, tree);
      }));
    }

    @Override
    public void registerOnLeave(BiConsumer<CheckContext, Tree> visitor) {
      ChecksVisitor.this.registerOnLeaveTree(TopLevelTree.class, statistics.time(ruleKey.rule(), (InputFileContext ctx, TopLevelTree tree) -> {
        currentCtx = ctx;
        visitor.accept(this, tree);
      }));
    }

    @Override
    public Deque<Tree> ancestors() {
      return currentCtx.ancestors();
    }

    @Override
    public String filename() {
      return currentCtx.goInputFile.filename();
    }

    @Override
    public GoInputFile goInputFile() {
      return currentCtx.goInputFile;
    }

    @Override
    public String fileContent() {
      try {
        return currentCtx.goInputFile.contents();
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read content of " + currentCtx.goInputFile, e);
      }
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message) {
      reportIssue(toHighlight, message, Collections.emptyList());
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation) {
      reportIssue(toHighlight, message, Collections.singletonList(secondaryLocation));
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(toHighlight, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(toHighlight.textRange(), message, secondaryLocations, gap);
    }

    @Override
    public void reportFileIssue(String message) {
      reportFileIssue(message, null);
    }

    @Override
    public void reportFileIssue(String message, @Nullable Double gap) {
      reportIssue((TextRange) null, message, Collections.emptyList(), gap);
    }

    private void reportIssue(@Nullable TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      currentCtx.reportIssue(ruleKey, textRange, message, secondaryLocations, gap);
    }

  }

}
