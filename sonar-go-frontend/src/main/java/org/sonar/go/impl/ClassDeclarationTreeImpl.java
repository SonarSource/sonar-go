/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class ClassDeclarationTreeImpl extends BaseTreeImpl implements ClassDeclarationTree {

  private final IdentifierTree identifier;
  private final Tree classTree;

  public ClassDeclarationTreeImpl(TreeMetaData metaData, @Nullable IdentifierTree identifier, Tree classTree) {
    super(metaData);
    this.identifier = identifier;
    this.classTree = classTree;
  }

  @CheckForNull
  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public Tree classTree() {
    return classTree;
  }

  @Override
  public List<Tree> children() {
    // identifier is not added to the children as it is already part of this classTree structure
    return Collections.singletonList(classTree);
  }
}
