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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.go.utils.TreeUtils;
import org.sonar.plugins.go.api.FieldTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;

public class FieldTreeImpl extends BaseTreeImpl implements FieldTree {

  private final List<IdentifierTree> names;
  @Nullable
  private final Tree type;

  public FieldTreeImpl(TreeMetaData metaData, List<IdentifierTree> names, @Nullable Tree type) {
    super(metaData);
    this.names = names;
    this.type = type;
  }

  @Override
  public List<IdentifierTree> names() {
    return names;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>(names);
    TreeUtils.addToListIfNotNull(children, type);
    return children;
  }
}
