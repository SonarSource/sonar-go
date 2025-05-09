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
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.sonar.go.persistence.JsonTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeserializationContextTest extends JsonTestHelper {

  private DeserializationContext context = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER)
    .withMetaDataProvider(metaDataProvider);

  @Test
  void shouldResolveToken() {
    var token = otherToken(1, 0, "foo");
    var json = Json.object()
      .add("tokenReference", "1:0:1:3");

    assertThat(context.fieldToToken(json, "tokenReference")).isSameAs(token);
    assertThat(context.fieldToNullableToken(json, "tokenReference")).isSameAs(token);
    assertThat(context.fieldToNullableToken(json, "unknown")).isNull();
  }

  @Test
  void shouldThrowExceptionWhenTokenNotFound() {
    otherToken(1, 0, "foo");
    var json = Json.object()
      .add("tokenReference", "7:13:7:20");

    var e = assertThrows(NoSuchElementException.class,
      () -> context.fieldToToken(json, "tokenReference"));
    assertThat(e).hasMessage("Token not found: 7:13:7:20");
  }

  @Test
  void shouldResolveMetaData() {
    var token = otherToken(1, 0, "foo");
    var json = Json.object()
      .add("metaData", "1:0:1:3");

    assertThat(context.metaData(json)).isNotNull();
    assertThat(context.metaData(json).textRange()).isEqualTo(token.textRange());
  }

  @Test
  void shouldThrowExceptionForMetaDataWhenNotFound() {
    var json = Json.object()
      .add("field1", "42");

    var e = assertThrows(IllegalStateException.class,
      () -> context.metaData(json));
    assertThat(e).hasMessage("Missing non-null value for field 'metaData' at '' member: {\"field1\":\"42\"}");
  }

  @Test
  void shouldReturnListForObjectList() {
    var nodes = Arrays.asList("A", "B", "C");
    var array = Json.array();
    nodes.stream().map(value -> Json.object().add("value", value)).forEach(array::add);

    var actual = context.objectList(array, (ctx, object) -> object.getString("value", null));
    assertThat(actual).containsExactly("A", "B", "C");

    assertThat(context.objectList(null, (ctx, object) -> object)).isEmpty();
    assertThat(context.objectList(Json.NULL, (ctx, object) -> object)).isEmpty();
  }

  @Test
  void shouldThrowExceptionForInvalidObjectList() {
    context.pushPath("root");
    var jsonValue = Json.value(42);
    var e = assertThrows(IllegalStateException.class,
      () -> context.objectList(jsonValue, (ctx, object) -> object));
    assertThat(e).hasMessage("Expect Array instead of JsonNumber at 'root' member: 42");
  }

  @Test
  void shouldReturnStringForFieldToNullableString() {
    var json = Json.object()
      .add("field1", "abc");
    assertThat(context.fieldToNullableString(json, "field1")).isEqualTo("abc");
    assertThat(context.fieldToNullableString(json, "field2")).isNull();
  }

  @Test
  void shouldThrowExceptionFieldToNullableStringForNotString() {
    var json = Json.object()
      .add("field1", 42);
    context.pushPath("root");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToNullableString(json, "field1"));
    assertThat(e).hasMessage("Expect String instead of 'JsonNumber' for field 'field1' at 'root' member: {\"field1\":42}");
  }

  @Test
  void shouldReturnStringForFieldToString() {
    var json = Json.object()
      .add("field1", "abc");
    assertThat(context.fieldToString(json, "field1")).isEqualTo("abc");
  }

  @Test
  void shouldThrowExceptionFieldToStringForMissingString() {
    var json = Json.object()
      .add("field1", "abc");
    context.pushPath("TopLevel");
    context.pushPath("AssignmentExpression");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToString(json, "field2"));
    assertThat(e).hasMessage("Missing non-null value for field 'field2' at 'TopLevel/AssignmentExpression' member: {\"field1\":\"abc\"}");
  }

  @Test
  void shouldThrowExceptionFieldToStringForNullString() {
    var json = Json.object()
      .add("field1", Json.NULL);
    context.pushPath("TopLevel");
    context.pushPath("AssignmentExpression");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToString(json, "field1"));
    assertThat(e).hasMessage("Missing non-null value for field 'field1' at 'TopLevel/AssignmentExpression' member: {\"field1\":null}");
  }

  @Test
  void shouldReturnRangeForFieldToRange() {
    var json = Json.object()
      .add("field1", "1:2:3:4");
    var range = context.fieldToRange(json, "field1");
    assertThat(range.start().line()).isEqualTo(1);
    assertThat(range.start().lineOffset()).isEqualTo(2);
    assertThat(range.end().line()).isEqualTo(3);
    assertThat(range.end().lineOffset()).isEqualTo(4);
  }

  @Test
  void shouldThrowExceptionFieldToRangeWhenFieldIsMissing() {
    var json = Json.object()
      .add("field1", "1:2:3:4");
    context.pushPath("root");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToRange(json, "field2"));
    assertThat(e).hasMessage("Missing non-null value for field 'field2' at 'root' member: {\"field1\":\"1:2:3:4\"}");
  }

  @Test
  void shouldReturnInWhenFieldToTnt() {
    var json = Json.object()
      .add("field1", 123);
    assertThat(context.fieldToInt(json, "field1")).isEqualTo(123);
  }

  @Test
  void shouldThrowExceptionFieldToIntForInvalidNumber() {
    var json = Json.object()
      .add("field1", "42");
    context.pushPath("root");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToInt(json, "field1"));
    assertThat(e).hasMessage("Expect Number instead of 'JsonString' for field 'field1' at 'root' member: {\"field1\":\"42\"}");
  }

  @Test
  void shouldThrowExceptionFieldToIntForJsonNull() {
    var json = Json.object()
      .add("field1", Json.NULL);
    context.pushPath("TopLevel");
    context.pushPath("AssignmentExpression");
    var e = assertThrows(IllegalStateException.class,
      () -> context.fieldToInt(json, "field1"));
    assertThat(e).hasMessage("Missing non-null value for field 'field1' at 'TopLevel/AssignmentExpression' member: {\"field1\":null}");
  }

  @Test
  void shouldReturnListForStringArray() {
    var jsonString = Json.array("foo", "bar", "baz");
    var actual = context.stringList(jsonString, (ctx, value) -> value);
    assertThat(actual).containsExactly("foo", "bar", "baz");
  }

  @Test
  void shouldReturnEmptyListForNull() {
    var actual = context.stringList(null, (ctx, value) -> value);
    assertThat(actual).isEmpty();
  }

  @Test
  void shouldReturnEmptyListForJsonNull() {
    var actual = context.stringList(Json.NULL, (ctx, value) -> value);
    assertThat(actual).isEmpty();
  }

  @Test
  void shouldThrowExceptionForStringListWhenNotArray() {
    var jsonObject = Json.object().add("foo", "bar");
    var e = assertThrows(IllegalStateException.class,
      () -> context.stringList(jsonObject, (ctx, value) -> value));
    assertThat(e).hasMessage("Expect Array instead of JsonObject at '' member: {\"foo\":\"bar\"}");
  }
}
