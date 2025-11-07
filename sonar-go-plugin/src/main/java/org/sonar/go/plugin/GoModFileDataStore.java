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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.checks.GoModFileData;

/**
 * This class provide a way to easily access Go mod files in the file system.
 * It aim to provide an efficient way to retrieve the closest Go mod file from a URI, through a tree structure.
 */
public class GoModFileDataStore {
  private Node root = new Node();
  private String rootPath = "";

  String getRootPath() {
    return rootPath;
  }

  public void addGoModFile(URI uriGoMod, GoModFileData goModFileData) {
    // Resolving to '.' to get rid of the go.mod file name
    var folders = splitPathIntoFolderName(uriGoMod.resolve(".").getPath());
    var currentNode = root;
    for (String folder : folders) {
      currentNode = currentNode.children.computeIfAbsent(folder, k -> new Node());
    }
    currentNode.goModFileData = goModFileData;
  }

  /**
   * This method is to be called when we are done storing Go mod file data.
   * It will perform optimization to get rid of the firsts empty nodes in the tree.
   */
  public void complete() {
    var sb = new StringBuilder();
    while (root.children.size() == 1 && root.goModFileData == GoModFileData.UNKNOWN_DATA) {
      var entry = root.children.entrySet().iterator().next();
      sb.append("/").append(entry.getKey());
      root = entry.getValue();
    }
    rootPath = sb.toString();
  }

  public GoModFileData retrieveClosestGoModFileData(String path) {
    if (!path.startsWith(rootPath)) {
      return GoModFileData.UNKNOWN_DATA;
    }
    path = path.substring(rootPath.length());
    var currentGoModFileData = root.goModFileData;
    var folders = splitPathIntoFolderName(path);
    var currentNode = root;
    for (String folder : folders) {
      currentNode = currentNode.children.get(folder);
      if (currentNode == null) {
        // If we reach a node that does not exist, we return the last known Go mod file data
        return currentGoModFileData;
      }
      if (currentNode.goModFileData != GoModFileData.UNKNOWN_DATA) {
        currentGoModFileData = currentNode.goModFileData;
      }
    }
    return currentGoModFileData;
  }

  public GoModFileData retrieveClosestGoModFileData(URI uri) {
    return retrieveClosestGoModFileData(uri.getPath());
  }

  private static String[] splitPathIntoFolderName(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return path.split("/");
  }

  private static class Node {
    @Nullable
    private GoModFileData goModFileData;
    private final Map<String, Node> children;

    public Node() {
      this.goModFileData = GoModFileData.UNKNOWN_DATA;
      this.children = new HashMap<>();
    }
  }
}
