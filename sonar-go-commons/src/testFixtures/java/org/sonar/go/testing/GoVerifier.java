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
package org.sonar.go.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.go.api.HasTextRange;
import org.sonar.go.api.TextPointer;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.TopLevelTree;
import org.sonar.go.api.Tree;
import org.sonar.go.api.checks.CheckContext;
import org.sonar.go.api.checks.GoCheck;
import org.sonar.go.api.checks.GoVersion;
import org.sonar.go.api.checks.InitContext;
import org.sonar.go.api.checks.SecondaryLocation;
import org.sonar.go.visitors.SymbolVisitor;
import org.sonar.go.visitors.TreeContext;
import org.sonar.go.visitors.TreeVisitor;
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.go.testing.TestGoConverter.GO_CONVERTER_DEBUG_TYPE_CHECK;

public class GoVerifier {
  private static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");

  public static void verify(String fileName, GoCheck check) {
    createVerifier(BASE_DIR.resolve(fileName), check).assertOneOrMoreIssues();
  }

  public static void verifyWithGoVersion(String fileName, GoCheck check, GoVersion goVersion) {
    createVerifier(BASE_DIR.resolve(fileName), check, goVersion).assertOneOrMoreIssues();
  }

  public static void verifyNoIssue(String fileName, GoCheck check) {
    createVerifier(BASE_DIR.resolve(fileName), check).assertNoIssues();
  }

  public static void verifyNoIssueWithGoVersion(String fileName, GoCheck check, GoVersion goVersion) {
    createVerifier(BASE_DIR.resolve(fileName), check, goVersion).assertNoIssues();
  }

  private static SingleFileVerifier createVerifier(Path path, GoCheck check) {
    return createVerifier(path, check, GoVersion.UNKNOWN_VERSION);
  }

  private static SingleFileVerifier createVerifier(Path path, GoCheck check, GoVersion goVersion) {

    SingleFileVerifier verifier = SingleFileVerifier.create(path, UTF_8);

    String testFileContent = readFile(path);
    Tree root = GO_CONVERTER_DEBUG_TYPE_CHECK.parse(testFileContent, null);

    ((TopLevelTree) root).allComments()
      .forEach(comment -> {
        TextPointer start = comment.textRange().start();
        verifier.addComment(start.line(), start.lineOffset() + 1, comment.text(), 2, 0);
      });

    TestContext ctx = new TestContext(verifier, path.getFileName().toString(), testFileContent, goVersion);
    new SymbolVisitor<>().scan(ctx, root);
    check.initialize(ctx);
    ctx.scan(root);

    return verifier;
  }

  private static String readFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + path, e);
    }
  }

  private static class TestContext extends TreeContext implements InitContext, CheckContext {

    private final TreeVisitor<TestContext> visitor;
    private final SingleFileVerifier verifier;
    private final GoVersion goVersion;
    private final String filename;
    private final String testFileContent;
    private Consumer<Tree> onLeave;

    public TestContext(SingleFileVerifier verifier, String filename, String testFileContent, GoVersion goVersion) {
      this.verifier = verifier;
      this.filename = filename;
      this.goVersion = goVersion;
      this.testFileContent = testFileContent;
      visitor = new TreeVisitor<>();
    }

    public void scan(@Nullable Tree root) {
      visitor.scan(this, root);
      if (onLeave != null) {
        onLeave.accept(root);
      }
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
    }

    @Override
    public void registerOnLeave(BiConsumer<CheckContext, Tree> visitor) {
      this.onLeave = tree -> visitor.accept(this, tree);
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
    public String filename() {
      return filename;
    }

    @Override
    public String fileContent() {
      return testFileContent;
    }

    @Override
    public GoVersion goVersion() {
      return goVersion;
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(toHighlight, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(toHighlight.textRange(), message, secondaryLocations, gap);
    }

    public void reportFileIssue(String message) {
      reportFileIssue(message, null);
    }

    @Override
    public void reportFileIssue(String message, @Nullable Double gap) {
      verifier.reportIssue(message).onFile().withGap(gap);
    }

    private void reportIssue(TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      TextPointer start = textRange.start();
      TextPointer end = textRange.end();
      SingleFileVerifier.Issue issue = verifier
        .reportIssue(message)
        .onRange(start.line(), start.lineOffset() + 1, end.line(), end.lineOffset())
        .withGap(gap);
      secondaryLocations.forEach(secondary -> issue.addSecondary(
        secondary.textRange.start().line(),
        secondary.textRange.start().lineOffset() + 1,
        secondary.textRange.end().line(),
        secondary.textRange.end().lineOffset(),
        secondary.message));
    }

  }
}
