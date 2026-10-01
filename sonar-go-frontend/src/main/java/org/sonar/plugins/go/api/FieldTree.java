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

/**
 * A field of a {@link FieldListTree}: the names it declares, if any, and their type. The parameters of a function are
 * {@link ParameterTree} instead, one per name, and the fields of a struct or of an interface stay native.
 */
public interface FieldTree extends Tree {

  /**
   * The names this field declares, which is empty for a field holding a type alone, such as an unnamed result.
   */
  List<IdentifierTree> names();

  @CheckForNull
  Tree type();
}
