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
package org.sonar.go.persistence.conversion;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.api.BlockTree;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.NativeKind;
import org.sonar.go.api.TextRange;
import org.sonar.go.api.Token;
import org.sonar.go.api.Tree;
import org.sonar.go.api.TreeMetaData;
import org.sonar.go.api.cfg.Block;
import org.sonar.go.api.cfg.ControlFlowGraph;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.go.impl.cfg.BlockImpl;
import org.sonar.go.impl.cfg.ControlFlowGraphImpl;

import static org.sonar.go.persistence.conversion.JsonTreeConverter.BODY;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.FORMAL_PARAMETERS;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.NAME;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RECEIVER;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.RETURN_TYPE;
import static org.sonar.go.persistence.conversion.JsonTreeConverter.TYPE_PARAMETERS;

public class DeserializationContext {

  private static final Logger LOG = LoggerFactory.getLogger(DeserializationContext.class);

  private static final int MAX_ILLEGAL_ELEMENT_TEXT_LENGTH = 80;

  private final PolymorphicConverter polymorphicConverter;

  private final Deque<String> jsonPath = new LinkedList<>();
  private final Map<Integer, Tree> cfgIndexToTree = new HashMap<>();

  private TreeMetaDataProvider metaDataProvider = null;

  public DeserializationContext(PolymorphicConverter polymorphicConverter) {
    this.polymorphicConverter = polymorphicConverter;
  }

  public DeserializationContext withMetaDataProvider(TreeMetaDataProvider metaDataProvider) {
    this.metaDataProvider = metaDataProvider;
    return this;
  }

  public void pushPath(String fieldName) {
    jsonPath.addLast(fieldName);
  }

  public void popPath() {
    jsonPath.removeLast();
  }

  public String path() {
    return String.join("/", jsonPath);
  }

  public TreeMetaData metaData(JsonObject json) {
    return RangeConverter.resolveMetaData(metaDataProvider, fieldToString(json, "metaData"));
  }

  public RuntimeException newIllegalMemberException(String message, @Nullable Object illegalElement) {
    String elementText = String.valueOf(illegalElement);
    elementText = elementText.substring(0, Math.min(elementText.length(), MAX_ILLEGAL_ELEMENT_TEXT_LENGTH));
    return new IllegalStateException(message + " at '" + path() + "' member: " + elementText);
  }

  @Nullable
  public <T extends Tree> T fieldToNullableObject(JsonObject parent, String fieldName, Class<T> expectedClass) {
    JsonValue json = parent.get(fieldName);
    if (json == null || Json.NULL.equals(json)) {
      return null;
    }
    return object(json, fieldName, expectedClass);
  }

  public <T extends Tree> T fieldToObject(JsonObject parent, String fieldName, Class<T> expectedClass) {
    JsonValue json = parent.get(fieldName);
    if (json == null || Json.NULL.equals(json)) {
      throw newIllegalMemberException("Unexpected null value for field '" + fieldName + "'", json);
    }
    return object(json, fieldName, expectedClass);
  }

  public NativeKind fieldToNativeKind(JsonObject parent, String fieldName) {
    return StringNativeKind.of(fieldToString(parent, fieldName, ""));
  }

  public <T extends Enum<T>> T fieldToEnum(JsonObject parent, String fieldName, Class<T> enumType) {
    return Enum.valueOf(enumType, fieldToString(parent, fieldName));
  }

  public <T extends Enum<T>> T fieldToEnum(JsonObject parent, String fieldName, String defaultValue, Class<T> enumType) {
    return Enum.valueOf(enumType, fieldToString(parent, fieldName, defaultValue));
  }

  public <T extends Tree> List<T> fieldToObjectList(JsonObject parent, String fieldName, Class<T> expectedClass) {
    return objectList(parent.get(fieldName), fieldName + "[]", expectedClass);
  }

  public <T extends Tree> List<T> objectList(@Nullable JsonValue value, String memberName, Class<T> expectedClass) {
    return objectList(value, jsonChild -> object(jsonChild, memberName, expectedClass));
  }

  public <T> List<T> objectList(@Nullable JsonValue value, BiFunction<DeserializationContext, JsonObject, T> converter) {
    return objectList(value, jsonChild -> converter.apply(this, jsonChild));
  }

  private <T> List<T> objectList(@Nullable JsonValue value, Function<JsonObject, T> converter) {
    if (value == null || value.isNull()) {
      return Collections.emptyList();
    }
    if (!value.isArray()) {
      throw newIllegalMemberException("Expect Array instead of " + value.getClass().getSimpleName(), value);
    }
    List<T> result = new ArrayList<>();
    for (JsonValue jsonValue : value.asArray()) {
      result.add(converter.apply(jsonValue.asObject()));
    }
    return result;
  }

  public String fieldToNullableString(JsonObject json, String fieldName) {
    JsonValue value = json.get(fieldName);
    if (value == null || Json.NULL.equals(value)) {
      return null;
    }
    return fieldToString(json, fieldName);
  }

  public String fieldToString(JsonObject json, String fieldName) {
    JsonValue value = json.get(fieldName);
    if (value == null || Json.NULL.equals(value)) {
      throw newIllegalMemberException("Missing non-null value for field '" + fieldName + "'", json);
    }
    if (!value.isString()) {
      throw newIllegalMemberException("Expect String instead of '" + value.getClass().getSimpleName() +
        "' for field '" + fieldName + "'", json);
    }
    return value.asString();
  }

  public String fieldToString(JsonObject json, String fieldName, String defaultValue) {
    return json.getString(fieldName, defaultValue);
  }

  public int fieldToInt(JsonObject json, String fieldName) {
    JsonValue value = json.get(fieldName);
    if (value == null || Json.NULL.equals(value)) {
      throw newIllegalMemberException("Missing non-null value for field '" + fieldName + "'", json);
    }
    if (!value.isNumber()) {
      throw newIllegalMemberException("Expect Number instead of '" + value.getClass().getSimpleName() +
        "' for field '" + fieldName + "'", json);
    }
    return value.asInt();
  }

  public TextRange fieldToRange(JsonObject json, String fieldName) {
    return RangeConverter.parse(fieldToString(json, fieldName));
  }

  public Token fieldToToken(JsonObject json, String fieldName) {
    return RangeConverter.resolveToken(metaDataProvider, fieldToString(json, fieldName));
  }

  @Nullable
  public Token fieldToNullableToken(JsonObject json, String fieldName) {
    return RangeConverter.resolveToken(metaDataProvider, fieldToNullableString(json, fieldName));
  }

  private <T extends Tree> T object(JsonValue json, String memberName, Class<T> expectedClass) {
    pushPath(memberName);
    if (!json.isObject()) {
      throw newIllegalMemberException("Unexpected value for Tree", json);
    }
    JsonObject jsonObject = json.asObject();
    String jsonType = fieldToString(jsonObject, SerializationContext.TYPE_ATTRIBUTE);
    T object = polymorphicConverter.fromJson(this, jsonType, jsonObject, memberName, expectedClass);
    popPath();
    int cfgId = jsonObject.getInt("__cfgId", -1);
    if (cfgId >= 0) {
      cfgIndexToTree.put(cfgId, object);
    }
    return object;
  }

  public FunctionDeclarationTreeImpl functionDeclarationTree(JsonObject json) {
    Tree returnType = fieldToNullableObject(json, RETURN_TYPE, Tree.class);
    Tree receiver = fieldToNullableObject(json, RECEIVER, Tree.class);
    IdentifierTree name = fieldToNullableObject(json, NAME, IdentifierTree.class);
    List<Tree> formalParameters = fieldToObjectList(json, FORMAL_PARAMETERS, Tree.class);
    Tree typeParameters = fieldToNullableObject(json, TYPE_PARAMETERS, Tree.class);
    BlockTree body = fieldToNullableObject(json, BODY, BlockTree.class);
    // We want to first build the underlying nodes (that will eventually be stored in the map "cfgIndexToTree")
    // before building the CFG, as it requires the nodes. We don't want to inline the value in the constructor call as it would
    // add an implicit contract for the parameters order.
    ControlFlowGraph cfg = controlFlowGraph(json);
    return new FunctionDeclarationTreeImpl(
      metaData(json),
      returnType,
      receiver,
      name,
      formalParameters,
      typeParameters,
      body,
      cfg);
  }

  @CheckForNull
  public ControlFlowGraph controlFlowGraph(JsonObject json) {
    try {
      List<BlockImpl> mappedBlocks = new ArrayList<>();
      JsonValue cfgValue = json.get("cfg");
      if (cfgValue == null || !cfgValue.isObject()) {
        return null;
      }
      JsonObject cfgObject = cfgValue.asObject();
      JsonValue blocksValue = cfgObject.get("Blocks");
      if (!blocksValue.isArray()) {
        return null;
      }
      List<JsonObject> blocks = blocksValue.asArray().values().stream()
        .filter(JsonValue::isObject)
        .map(JsonValue::asObject)
        .toList();

      for (JsonObject block : blocks) {
        JsonValue node = block.get("Node");
        List<Tree> nodes = Collections.emptyList();
        if (node.isArray()) {
          nodes = node.asArray().values().stream()
            .filter(JsonValue::isNumber)
            .map(JsonValue::asInt)
            .map(cfgIndexToTree::get)
            .filter(Objects::nonNull)
            .toList();
        }
        mappedBlocks.add(
          new BlockImpl(nodes));
      }

      // Second pass to set successors
      for (int i = 0; i < blocks.size(); i++) {
        JsonObject currentBlock = blocks.get(i);
        JsonValue successorValue = currentBlock.get("Successors");
        if (!successorValue.isArray()) {
          mappedBlocks.get(i).setSuccessors(Collections.emptyList());
        } else {
          List<BlockImpl> successors = successorValue.asArray().values().stream()
            .filter(JsonValue::isNumber)
            .map(JsonValue::asInt)
            .map(mappedBlocks::get)
            .filter(Objects::nonNull)
            .toList();
          mappedBlocks.get(i).setSuccessors((List<Block>) (List<? extends Block>) successors);
        }
      }

      return new ControlFlowGraphImpl((List<Block>) (List<? extends Block>) mappedBlocks);
    } catch (Exception e) {
      // We want to catch any exception when building a CFG, as it is not critical to have one to perform the majority of the analysis.
      LOG.warn("Error while transferring a CFG.", e);
    }
    return null;
  }
}
