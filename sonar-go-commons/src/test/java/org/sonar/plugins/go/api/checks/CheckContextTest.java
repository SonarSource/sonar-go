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
package org.sonar.plugins.go.api.checks;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.go.visitors.TreeContext;
import org.sonar.plugins.go.api.HasTextRange;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CheckContextTest {

  @Test
  void testParent() {
    Tree a = mock(Tree.class);
    Tree b = mock(Tree.class);
    Tree c = mock(Tree.class);

    CheckContextToTestDefaultMethod context = new CheckContextToTestDefaultMethod();
    assertThat(context.parent()).isNull();

    context.enter(a);
    assertThat(context.parent()).isNull();

    context.enter(b);
    assertThat(context.parent()).isSameAs(a);

    context.enter(c);
    assertThat(context.parent()).isSameAs(b);

    context.leave(c);
    assertThat(context.parent()).isSameAs(a);

    context.leave(b);
    assertThat(context.parent()).isNull();
  }

  @Test
  void testFirstAncestorOfKind() {
    A a1 = mock(A.class);
    B b1 = mock(B.class);
    A a2 = mock(A.class);
    B b2 = mock(B.class);

    CheckContextToTestDefaultMethod context = new CheckContextToTestDefaultMethod();
    assertThat(context.firstAncestorOfKind(A.class)).isEmpty();

    context.enter(a1);
    assertThat(context.firstAncestorOfKind(A.class)).isEmpty();

    context.enter(b1);
    assertThat(context.firstAncestorOfKind(A.class)).contains(a1);

    context.enter(a2);
    assertThat(context.firstAncestorOfKind(A.class)).contains(a1);

    context.enter(b2);
    assertThat(context.firstAncestorOfKind(A.class)).contains(a2);

    context.leave(b2);
    assertThat(context.firstAncestorOfKind(A.class)).contains(a1);
  }

  private static class CheckContextToTestDefaultMethod extends TreeContext implements CheckContext {

    public String filename() {
      return null;
    }

    public InputFile inputFile() {
      return null;
    }

    public String fileContent() {
      return null;
    }

    @Override
    public GoModFileData goModFileData() {
      return null;
    }

    public void reportIssue(TextRange textRange, String message) {
      // do nothing, test method
    }

    public void reportIssue(HasTextRange toHighlight, String message) {
      // do nothing, test method
    }

    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation) {
      // do nothing, test method
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
      // do nothing, test method
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      // do nothing, test method
    }

    public void reportFileIssue(String message) {
      // do nothing, test method
    }

    public void reportFileIssue(String message, @Nullable Double gap) {
      // do nothing, test method
    }

  }

  private static class A implements Tree {
    @Override
    public List<Tree> children() {
      return List.of();
    }

    @Override
    public TreeMetaData metaData() {
      return null;
    }
  }

  private static class B implements Tree {
    @Override
    public List<Tree> children() {
      return List.of();
    }

    @Override
    public TreeMetaData metaData() {
      return null;
    }
  }
}
