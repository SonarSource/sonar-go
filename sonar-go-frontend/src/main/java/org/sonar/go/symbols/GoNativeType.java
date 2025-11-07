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
package org.sonar.go.symbols;

import java.util.Set;
import org.sonar.plugins.go.api.FloatLiteralTree;
import org.sonar.plugins.go.api.ImaginaryLiteralTree;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.LiteralTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.Tree;

public class GoNativeType {
  public static final String UNKNOWN = "UNKNOWN";

  public static final String BOOL = "bool";
  public static final String STRING = "string";
  public static final String INT = "int";
  public static final String INT8 = "int8";
  public static final String INT16 = "int16";
  public static final String INT32 = "int32";
  public static final String INT64 = "int64";
  public static final String UINT = "uint";
  public static final String UINT8 = "uint8";
  public static final String UINT16 = "uint16";
  public static final String UINT32 = "uint32";
  public static final String UINT64 = "uint64";
  public static final String UINTPTR = "uintptr";
  public static final String FLOAT32 = "float32";
  public static final String FLOAT64 = "float64";
  public static final String COMPLEX64 = "complex64";
  public static final String COMPLEX128 = "complex128";

  // Default inferred types in go
  public static final String DEFAULT_INT = INT;
  public static final String DEFAULT_FLOAT = FLOAT64;
  public static final String DEFAULT_COMPLEX = COMPLEX128;

  private static final Set<String> INTEGERS = Set.of(INT, INT8, INT16, INT32, INT64, UINT, UINT8, UINT16, UINT32, UINT64, UINTPTR);
  private static final Set<String> FLOATS = Set.of(FLOAT32, FLOAT64);
  private static final Set<String> COMPLEXES = Set.of(COMPLEX64, COMPLEX128);
  private static final Set<String> BOOLEAN_VALUES = Set.of("true", "false");

  private GoNativeType() {
  }

  public static boolean isInt(String type) {
    return INTEGERS.contains(type);
  }

  public static boolean isFloat(String type) {
    return FLOATS.contains(type);
  }

  public static boolean isComplex(String type) {
    return COMPLEXES.contains(type);
  }

  public static String computeTypeFromValue(Tree tree) {
    if (tree instanceof IntegerLiteralTree) {
      return DEFAULT_INT;
    } else if (tree instanceof FloatLiteralTree) {
      return DEFAULT_FLOAT;
    } else if (tree instanceof ImaginaryLiteralTree) {
      return DEFAULT_COMPLEX;
    } else if (tree instanceof StringLiteralTree) {
      return STRING;
    } else if (tree instanceof LiteralTree literalTree && BOOLEAN_VALUES.contains(literalTree.value())) {
      return BOOL;
    }
    return UNKNOWN;
  }
}
