/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonar.go.checks;

import java.math.BigInteger;
import org.sonar.check.Rule;
import org.sonar.plugins.go.api.IntegerLiteralTree;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

import static org.sonar.plugins.go.api.IntegerLiteralTree.Base.OCTAL;

@Rule(key = "S1314")
public class OctalValuesCheck implements GoCheck {

  private static final String MESSAGE = "Use decimal values instead of octal ones.";
  private static final BigInteger EIGHT = BigInteger.valueOf(OCTAL.getRadix());
  private static final int FILE_PERMISSION_MASK_LENGTH = 3;

  @Override
  public void initialize(InitContext init) {
    init.register(IntegerLiteralTree.class, (ctx, literal) -> {
      if (literal.getBase() == OCTAL && !isException(literal)) {
        ctx.reportIssue(literal, MESSAGE);
      }
    });
  }

  private static boolean isException(IntegerLiteralTree literalTree) {
    // octal literal < 8 are authorized, as well as octal literals with 3 digits, as they are often used for file permissions
    var value = literalTree.getIntegerValue();
    return value.compareTo(EIGHT) < 0 || literalTree.getNumericPart().length() == FILE_PERMISSION_MASK_LENGTH;
  }

}
