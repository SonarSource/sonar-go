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

/**
 * A field list holding a name and a type per field: the receiver, the results and the type parameters of a function or
 * of a function <em>type</em>, and the type parameters of a type declaration. Only the names of a list reached through
 * {@link FunctionDeclarationTree#receiver()} or {@link FunctionDeclarationTree#returnType()} are declared in the scope
 * of a function body; the ones of a bare function type, such as the {@code err} of a {@code func() (err error)} used
 * as the type of a field or of a variable, are not. The parameters of a function are {@link ParameterTree} instead,
 * one per name, and the fields of a struct or of an interface stay native.
 */
public interface FieldListTree extends Tree {

  List<FieldTree> fields();

  /**
   * The names declared by the fields of this list. Only the names of the fields are returned, so that the members of a
   * composite type used as the type of a field are left out.
   */
  default List<IdentifierTree> names() {
    return fields().stream().flatMap(field -> field.names().stream()).toList();
  }
}
