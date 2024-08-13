/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.Location;
import com.sonarsource.apex.converter.visitor.GeneratedApexAstVisitor;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

/**
 * Creates the SlangNative node in a bottom up approach - starts with the leaves and then builds the parents
 * based on their children
 */
public class ApexToSlangNativeConverter extends GeneratedApexAstVisitor {

  private static final Object ROOT = new Object();
  protected final TextRange topLevelRange;
  private final TreeMetaDataProvider metaDataProvider;
  private final Set<Class<?>> skippedUselessHierarchyLevel;
  private final Map<Class<?>, NativeKind> nativeKindMap = new HashMap<>();
  private final Deque<ConverterNode> hierarchy = new LinkedList<>();
  private final Deque<Property> properties = new LinkedList<>();
  final LineSet lineSet;

  ApexToSlangNativeConverter(Lexer.Tokens tokens, Set<Class<?>> skippedUselessHierarchyLevel) {
    this.skippedUselessHierarchyLevel = skippedUselessHierarchyLevel;
    topLevelRange = tokens.range();
    lineSet = tokens.lineSet;
    metaDataProvider = new TreeMetaDataProvider(tokens.comments, tokens.tokensWithoutComments);
    push(new ConverterNode(ROOT));
  }

  TopLevelTree topLevelTree() {
    TreeMetaData metaData = metaDataProvider.metaData(topLevelRange);
    return new TopLevelTreeImpl(metaData, hierarchy.peek().children(), metaDataProvider.allComments());
  }

  @Override
  protected void defaultEnter(Object nativeNode, @Nullable Location location) {
    if (isUselessHierarchyLevelNode(nativeNode)) {
      return;
    }
    push(new ConverterNode(nativeNode));
  }

  @Override
  protected void defaultExit(Object nativeNode, @Nullable Location location) {
    if (isUselessHierarchyLevelNode(nativeNode)) {
      return;
    }
    ConverterNode convNode = pop();
    if (location != null || !convNode.children().isEmpty()) {
      add(new NativeTreeImpl(metaData(convNode, location), kind(nativeNode), convNode.children()));
    }
  }

  TreeMetaDataProvider metaDataProvider() {
    return metaDataProvider;
  }

  TextRange rangeBetween(Tree startTree, Tree endTree) {
    return new TextRangeImpl(startTree.textRange().start(), endTree.textRange().end());
  }

  private boolean isUselessHierarchyLevelNode(Object nativeNode) {
    return skippedUselessHierarchyLevel.contains(nativeNode.getClass());
  }

  NativeKind kind(Object node) {
    return nativeKindMap.computeIfAbsent(node.getClass(), ClassNativeKind::new);
  }

  private void push(ConverterNode node) {
    hierarchy.push(node);
    properties.push(null);
  }

  ConverterNode pop() {
    properties.pop();
    return hierarchy.pop();
  }

  @Override
  protected void prepareChildVisit(Property property) {
    properties.pop();
    properties.push(property);
  }

  void add(Tree tree) {
    hierarchy.peek().addChild(properties.peek(), tree);
  }

  TreeMetaData metaData(ConverterNode node, @Nullable Location location) {
    return metaDataProvider.metaData(textRange(location, node.children()));
  }

  /**
   * We cannot trust the location, it can be smaller that its actual size given its children.
   * E.g. "public class A {}"
   */
  private TextRange textRange(@Nullable Location location, List<Tree> children) {
    if (children.isEmpty() && location == null) {
      throw new IllegalStateException("Null location and no child");
    } else if (location == null) {
      Tree firstChild = children.get(0);
      Tree lastChild = children.get(children.size() - 1);
      return rangeBetween(firstChild, lastChild);
    } else {
      TextRange range = lineSet.range(location.getStartIndex(), location.getEndIndex());
      if (!children.isEmpty()) {
        Tree firstChild = children.get(0);
        if (range.start().compareTo(firstChild.textRange().start()) > 0) {
          range = new TextRangeImpl(firstChild.textRange().start(), range.end());
        }
        Tree lastChild = children.get(children.size() - 1);
        if (range.end().compareTo(lastChild.textRange().end()) < 0) {
          range = new TextRangeImpl(range.start(), lastChild.textRange().end());
        }
      }
      return range;
    }
  }

}
