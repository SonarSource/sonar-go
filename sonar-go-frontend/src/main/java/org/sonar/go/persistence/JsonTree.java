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
package org.sonar.go.persistence;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.go.persistence.conversion.DeserializationContext;
import org.sonar.go.persistence.conversion.JsonTreeConverter;
import org.sonar.go.persistence.conversion.SerializationContext;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.TreeOrError;

public final class JsonTree {

  private JsonTree() {
  }

  public static String toJson(Tree tree) {
    TreeMetaData metaData = tree.metaData();
    TreeMetaDataProvider provider = new TreeMetaDataProvider(metaData.commentsInside(), metaData.tokens());
    SerializationContext ctx = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    return Json.object()
      .add("treeMetaData", JsonTreeConverter.TREE_METADATA_PROVIDER_TO_JSON.apply(ctx, provider))
      .add("tree", ctx.toJson(tree))
      .toString();
  }

  public static Tree fromJsonSingleTree(String json) {
    JsonObject root = Json.parse(json).asObject();
    return fromJsonSingleTree(root);
  }

  private static Tree fromJsonSingleTree(JsonObject root) {
    JsonObject treeMetaData = root.get("treeMetaData").asObject();
    DeserializationContext ctx = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    TreeMetaDataProvider metaDataProvider = JsonTreeConverter.TREE_METADATA_PROVIDER_FROM_JSON.apply(ctx, treeMetaData);
    ctx = ctx.withMetaDataProvider(metaDataProvider);
    return ctx.fieldToNullableObject(root, "tree", Tree.class);
  }

  public static Map<String, TreeOrError> fromJson(String json) {
    Map<String, TreeOrError> trees = new HashMap<>();
    JsonObject root = Json.parse(json).asObject();
    for (String name : root.names()) {
      var treeRoot = root.get(name).asObject();
      var errorJson = treeRoot.get("error");
      if (errorJson != null && errorJson.isString()) {
        var errorMessage = errorJson.asString();
        trees.put(name, TreeOrError.of(errorMessage));
      } else {
        var tree = fromJsonSingleTree(treeRoot);
        trees.put(name, TreeOrError.of(tree));
      }
    }
    return trees;
  }

}
