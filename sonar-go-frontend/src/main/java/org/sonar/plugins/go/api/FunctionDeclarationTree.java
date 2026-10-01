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
package org.sonar.plugins.go.api;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.plugins.go.api.cfg.ControlFlowGraph;

public interface FunctionDeclarationTree extends Tree {

  /**
   * The result list of the function, which holds a named or an unnamed field per result, or null when the function
   * returns nothing.
   */
  @CheckForNull
  FieldListTree returnType();

  /**
   * Can return null when the function is a function literal (closure).
   */
  @CheckForNull
  IdentifierTree name();

  List<Tree> formalParameters();

  /**
   * Can return null when the function is external (non-Go)
   */
  @CheckForNull
  BlockTree body();

  /**
   * The receiver of the method, or null when the function is not a method. A receiver list holds a single field, named
   * or unnamed.
   */
  @CheckForNull
  FieldListTree receiver();

  /**
   * Return receiver name. It is lazy calculated and cached for next invocations.
   * @return receiver name.
   */
  @CheckForNull
  String receiverName();

  /**
   * Return receiver type. It is lazy calculated and cached for next invocations.
   * @return receiver type.
   */
  @CheckForNull
  String receiverType();

  @CheckForNull
  FieldListTree typeParameters();

  TextRange rangeToHighlight();

  @CheckForNull
  ControlFlowGraph cfg();

  /**
   * Return function declaration signature. This signature may be treated as unique id.
   * @return function declaration signature
   */
  String signature();
}
