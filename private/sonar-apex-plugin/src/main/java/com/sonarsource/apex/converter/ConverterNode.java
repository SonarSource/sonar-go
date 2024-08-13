/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import com.sonarsource.apex.converter.visitor.GeneratedApexAstVisitor.Property;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.Tree;

public class ConverterNode {

  private final Object nativeNode;

  private Map<Property, List<Tree>> childrenMap;

  private List<Tree> children;

  ConverterNode(Object nativeNode) {
    this.nativeNode = nativeNode;
  }

  // This can be used inside the consumers of this class when looking for the parent native node
  public Object nativeNode() {
    return nativeNode;
  }

  void addChild(@Nullable Property property, Tree child) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(child);
    sortLastInsertedElement();
    if (childrenMap == null) {
      childrenMap = new HashMap<>();
    }
    childrenMap.computeIfAbsent(property, key -> new ArrayList<>()).add(child);
  }

  /** Mandatory for {@link ApexToSlangNativeConverter} textRange() */
  private void sortLastInsertedElement() {
    for (int i = children.size() - 1; i > 0 && comparePos(children.get(i - 1), children.get(i)) > 0; i--) {
      Tree tree = children.get(i);
      children.set(i, children.get(i - 1));
      children.set(i - 1, tree);
    }
  }

  private static int comparePos(Tree a, Tree b) {
    return a.textRange().start().compareTo(b.textRange().start());
  }

  List<Tree> children() {
    if (children == null) {
      return Collections.emptyList();
    }
    return children;
  }

  List<Tree> children(@Nullable Property property) {
    if (childrenMap == null) {
      return Collections.emptyList();
    }
    return childrenMap.getOrDefault(property, Collections.emptyList());
  }

}
