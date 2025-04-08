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
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.sonar.plugins.go.api.NativeKind;
import org.sonar.plugins.go.api.TextRange;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;

public class SerializationContext {

  public static final String TYPE_ATTRIBUTE = "@type";

  private final PolymorphicConverter polymorphicConverter;

  public SerializationContext(PolymorphicConverter polymorphicConverter) {
    this.polymorphicConverter = polymorphicConverter;
  }

  public JsonObject newTypedObject(Tree tree) {
    String jsonType = polymorphicConverter.getJsonType(tree);
    if (jsonType == null) {
      throw new IllegalStateException("Unsupported implementation class: " + tree.getClass().getName());
    }
    return Json.object()
      .add(TYPE_ATTRIBUTE, jsonType)
      .add("metaData", RangeConverter.metaDataReference(tree));
  }

  public <T extends Tree> JsonValue toJson(@Nullable T object) {
    return polymorphicConverter.toJson(this, object);
  }

  public JsonValue toJson(@Nullable Token token) {
    return Json.value(RangeConverter.tokenReference(token));
  }

  public JsonValue toJson(Enum<?> entry) {
    return Json.value(entry.name());
  }

  public JsonValue toJson(NativeKind kind) {
    return Json.value(StringNativeKind.toString(kind));
  }

  public JsonValue toJson(@Nullable TextRange range) {
    return Json.value(RangeConverter.format(range));
  }

  public <T extends Tree> JsonArray toJsonArray(List<T> nodes) {
    JsonArray array = Json.array();
    nodes.stream().map(this::toJson).forEach(array::add);
    return array;
  }

  public <T> JsonArray toJsonArray(List<T> nodes, BiFunction<SerializationContext, T, JsonValue> converter) {
    JsonArray array = Json.array();
    nodes.stream().map(node -> converter.apply(this, node)).forEach(array::add);
    return array;
  }

}
