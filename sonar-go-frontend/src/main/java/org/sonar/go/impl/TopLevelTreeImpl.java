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
package org.sonar.go.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.ImportDeclarationTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.TopLevelTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class TopLevelTreeImpl extends BaseTreeImpl implements TopLevelTree {

  private final List<Tree> declarations;
  private final List<Comment> allComments;
  private final Token firstCpdToken;
  private final Set<String> importsAsStrings;

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments) {
    this(metaData, declarations, allComments, null);
  }

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments, @Nullable Token firstCpdToken) {
    super(metaData);
    this.declarations = declarations;
    this.allComments = allComments;
    this.firstCpdToken = firstCpdToken;
    this.importsAsStrings = getImportsAsStrings(declarations);
  }

  @Override
  public List<Tree> declarations() {
    return declarations;
  }

  @Override
  public List<Comment> allComments() {
    return allComments;
  }

  @CheckForNull
  @Override
  public Token firstCpdToken() {
    return firstCpdToken;
  }

  @Override
  public List<Tree> children() {
    return declarations();
  }

  @Override
  public boolean doesImportType(String type) {
    return importsAsStrings.contains(type);
  }

  private static Set<String> getImportsAsStrings(List<Tree> declarations) {
    return declarations.stream()
      .filter(ImportDeclarationTree.class::isInstance)
      .flatMap(it -> it.children().stream())
      .filter(ImportSpecificationTree.class::isInstance)
      .map(ImportSpecificationTree.class::cast)
      // Imports with aliases are not supported currently, they are filtered-out to avoid false positives
      .filter(spec -> spec.name() == null)
      .map(spec -> spec.path().content())
      .collect(Collectors.toSet());
  }
}
