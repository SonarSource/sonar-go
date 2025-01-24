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
package org.sonar.go.checks;

import org.sonar.go.api.NativeTree;
import org.sonar.go.api.Tree;
import org.sonar.go.persistence.conversion.StringNativeKind;

// Probably the sonar-go-frontend is a better place for this class after implementing SONARGO-97
public final class NativeKinds {
  public static final String LABEL = "LabeledStmt";
  public static final String SEMICOLON = "Semicolon";
  public static final String FUNCTION_CALL = "X(CallExpr)";
  public static final String STRUCT_FIELD_ACCESS = "Fun(SelectorExpr)";
  public static final String ARGUMENTS = "Args([]Expr)";

  private static final String IMPORT_SUFFIX = "](ImportSpec)";

  private NativeKinds() {
  }

  public static boolean isStringNativeKindOfType(Tree tree, String type) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().equals(type);
  }

  public static boolean isImport(Tree tree) {
    return tree instanceof NativeTree nativeTree
      && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
      && stringNativeKind.toString().endsWith(IMPORT_SUFFIX);
  }

}
