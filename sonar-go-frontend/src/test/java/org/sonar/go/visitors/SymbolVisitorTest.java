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
package org.sonar.go.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.go.api.IdentifierTree;
import org.sonar.go.api.IntegerLiteralTree;
import org.sonar.go.symbols.GoNativeType;
import org.sonar.go.symbols.Symbol;
import org.sonar.go.symbols.Usage;
import org.sonar.go.testing.TestGoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.go.testing.TextRangeGoAssert.assertThat;

class SymbolVisitorTest {

  @Test
  void variableShouldHaveSymbolForGlobalVariable() {
    var symbols = parseAndGetSymbols("""
      package main
      var x int = 5
      """);
    assertThat(symbols).hasSize(1);
    var x = symbols.get(0);
    assertThat(x.getType()).isEqualTo(GoNativeType.INT);
    assertThat(x.getUsages()).extracting("type").containsExactly(Usage.UsageType.DECLARATION);
  }

  @Test
  void symbolShouldTrackAllUsages() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var x = 1
        x = 2
        fmt.Println(x)
      }
      """);
    assertThat(symbols).hasSize(3);

    var x = symbols.get(0);
    assertThat(x).isSameAs(symbols.get(1)).isSameAs(symbols.get(2));
    assertThat(x.getType()).isEqualTo(GoNativeType.INT);
    assertThat(x.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.ASSIGNMENT, Usage.UsageType.REFERENCE);

    var declaration = x.getUsages().get(0);
    assertThat(declaration.identifier().name()).isEqualTo("x");
    assertThat(declaration.identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(declaration.value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.value()).isEqualTo("1"));

    var assignment = x.getUsages().get(1);
    assertThat(assignment.identifier().name()).isEqualTo("x");
    assertThat(assignment.identifier().textRange()).hasRange(4, 2, 4, 3);
    assertThat(assignment.value()).isInstanceOfSatisfying(IntegerLiteralTree.class, integer -> assertThat(integer.value()).isEqualTo("2"));

    var reference = x.getUsages().get(2);
    assertThat(reference.identifier().name()).isEqualTo("x");
    assertThat(reference.identifier().textRange()).hasRange(5, 14, 5, 15);
    assertThat(reference.value()).isNull();
  }

  @Test
  void shouldProperlyTrackMultipleAssignment() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var x = 1
        y := 2
        x, y = 3, 4
        x, y = foo()
      }
      """);
    assertThat(symbols).hasSize(6);

    var x = symbols.get(0);
    assertThat(x).isSameAs(symbols.get(2)).isSameAs(symbols.get(4));
    assertThat(x.getType()).isEqualTo(GoNativeType.INT);
    assertThat(x.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.ASSIGNMENT, Usage.UsageType.ASSIGNMENT);

    var y = symbols.get(1);
    assertThat(y).isSameAs(symbols.get(3)).isSameAs(symbols.get(5));
    assertThat(y.getType()).isEqualTo(GoNativeType.INT);
    assertThat(y.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.ASSIGNMENT, Usage.UsageType.ASSIGNMENT);

    assertThat(x).isNotSameAs(y);
  }

  @Test
  void shouldProperlyTrackParameterVariable() {
    var symbols = parseAndGetSymbols("""
      package main
      func main(x int) {
        x = 2
        fmt.Println(x)
      }
      """);
    assertThat(symbols).hasSize(3);

    var x = symbols.get(0);
    assertThat(x).isSameAs(symbols.get(1)).isSameAs(symbols.get(2));
    assertThat(x.getType()).isEqualTo(GoNativeType.INT);
    assertThat(x.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.PARAMETER, Usage.UsageType.ASSIGNMENT, Usage.UsageType.REFERENCE);
  }

  @Test
  void shouldHandleVariableShadowingGlobalToLocal() {
    var symbols = parseAndGetSymbols("""
      package main
      var x int = 1
      func main() {
        var x = 1
        fmt.Println(x)
      }
      func other() {
        fmt.Println(x)
      }
      """);
    assertThat(symbols).hasSize(4);

    var xGlobal = symbols.get(0);
    assertThat(xGlobal).isSameAs(symbols.get(3)).isNotSameAs(symbols.get(1));
    assertThat(xGlobal.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(xGlobal.getUsages().get(0).identifier().textRange()).hasRange(2, 4, 2, 5);
    assertThat(xGlobal.getUsages().get(1).identifier().textRange()).hasRange(8, 14, 8, 15);

    var xLocal = symbols.get(1);
    assertThat(xLocal).isSameAs(symbols.get(2));
    assertThat(xLocal.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(xLocal.getUsages().get(0).identifier().textRange()).hasRange(4, 6, 4, 7);
    assertThat(xLocal.getUsages().get(1).identifier().textRange()).hasRange(5, 14, 5, 15);

    assertThat(xGlobal).isNotSameAs(xLocal);
  }

  @Test
  void shouldHandleVariableShadowingLocal() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var x = 1
        if true {
          var x = 2
          fmt.Println(x)
        }
        fmt.Println(x)
      }
      """);
    assertThat(symbols).hasSize(4);

    var xLocalFunc = symbols.get(0);
    assertThat(xLocalFunc).isSameAs(symbols.get(3)).isNotSameAs(symbols.get(1));
    assertThat(xLocalFunc.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(xLocalFunc.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(xLocalFunc.getUsages().get(1).identifier().textRange()).hasRange(8, 14, 8, 15);

    var xLocalBranch = symbols.get(1);
    assertThat(xLocalBranch).isSameAs(symbols.get(2));
    assertThat(xLocalBranch.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(xLocalBranch.getUsages().get(0).identifier().textRange()).hasRange(5, 8, 5, 9);
    assertThat(xLocalBranch.getUsages().get(1).identifier().textRange()).hasRange(6, 16, 6, 17);

    assertThat(xLocalFunc).isNotSameAs(xLocalBranch);
  }

  @Test
  void shouldHandleVariableShadowingLocalForLoop() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        i := 20
        for i := 0; i < 5; i++ {
          fmt.Printf("for %d\\n", i)
        }
        fmt.Printf("outside: %d\\n", i)
      }
      """);
    assertThat(symbols).hasSize(6);

    var iOutsideOfLoop = symbols.get(0);
    assertThat(iOutsideOfLoop).isSameAs(symbols.get(5));
    assertThat(iOutsideOfLoop.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(iOutsideOfLoop.getUsages().get(0).identifier().textRange()).hasRange(3, 2, 3, 3);
    assertThat(iOutsideOfLoop.getUsages().get(1).identifier().textRange()).hasRange(7, 30, 7, 31);

    var iInLoop = symbols.get(1);
    assertThat(iInLoop).isSameAs(symbols.get(2)).isSameAs(symbols.get(3)).isSameAs(symbols.get(4));
    assertThat(iInLoop.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(iInLoop.getUsages().get(0).identifier().textRange()).hasRange(4, 6, 4, 7);
    assertThat(iInLoop.getUsages().get(1).identifier().textRange()).hasRange(4, 14, 4, 15);
    assertThat(iInLoop.getUsages().get(2).identifier().textRange()).hasRange(4, 21, 4, 22);
    assertThat(iInLoop.getUsages().get(3).identifier().textRange()).hasRange(5, 27, 5, 28);

    assertThat(iOutsideOfLoop).isNotSameAs(iInLoop);
  }

  @Test
  void shouldNotConfuseMemberDeclarationElementWithVariableReference() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a, b, c int
        a.b.c()
      }
      """);
    assertThat(symbols).hasSize(4);

    var a = symbols.get(0);
    assertThat(a).isSameAs(symbols.get(3));
    assertThat(a.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(a.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(a.getUsages().get(1).identifier().textRange()).hasRange(4, 2, 4, 3);

    var b = symbols.get(1);
    assertThat(b.getUsages()).extracting("type").containsExactly(Usage.UsageType.DECLARATION);
    assertThat(b.getUsages().get(0).identifier().textRange()).hasRange(3, 9, 3, 10);

    var c = symbols.get(2);
    assertThat(c.getUsages()).extracting("type").containsExactly(Usage.UsageType.DECLARATION);
    assertThat(c.getUsages().get(0).identifier().textRange()).hasRange(3, 12, 3, 13);

    assertThat(a).isNotSameAs(b).isNotSameAs(c);
    assertThat(b).isNotSameAs(c);
  }

  @Test
  void testWithThreeLevelsOfNesting() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        if true {
          fmt.Printf(a)
          var a int = 2
          if true {
            fmt.Printf(a)
            var a int = 3
            fmt.Printf(a)
          }
        }
      }
      """);
    assertThat(symbols).hasSize(6);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(1));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 15, 5, 16);

    var aIf1 = symbols.get(2);
    assertThat(aIf1).isSameAs(symbols.get(3));
    assertThat(aIf1.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aIf1.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aIf1.getUsages().get(1).identifier().textRange()).hasRange(8, 17, 8, 18);

    var aIf2 = symbols.get(4);
    assertThat(aIf2).isSameAs(symbols.get(5));
    assertThat(aIf2.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aIf2.getUsages().get(0).identifier().textRange()).hasRange(9, 10, 9, 11);
    assertThat(aIf2.getUsages().get(1).identifier().textRange()).hasRange(10, 17, 10, 18);

    assertThat(aMain).isNotSameAs(aIf1).isNotSameAs(aIf2);
    assertThat(aIf1).isNotSameAs(aIf2);
  }

  @Test
  void testWithMixedIfAndLoop() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        if true {
          fmt.Printf(a)
          var a int = 2
          fmt.Printf(a)
          for a := 0; a < 10; a++ {
            fmt.Printf(a)
            var a int = 3
            fmt.Printf(a)
          }
        }
      }
      """);
    assertThat(symbols).hasSize(10);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(1));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 15, 5, 16);

    var aIf = symbols.get(2);
    assertThat(aIf).isSameAs(symbols.get(3));
    assertThat(aIf.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aIf.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aIf.getUsages().get(1).identifier().textRange()).hasRange(7, 15, 7, 16);

    var aFor = symbols.get(4);
    assertThat(aFor).isSameAs(symbols.get(5)).isSameAs(symbols.get(6)).isSameAs(symbols.get(7));
    assertThat(aFor.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aFor.getUsages().get(0).identifier().textRange()).hasRange(8, 8, 8, 9);
    assertThat(aFor.getUsages().get(1).identifier().textRange()).hasRange(8, 16, 8, 17);
    assertThat(aFor.getUsages().get(2).identifier().textRange()).hasRange(8, 24, 8, 25);
    assertThat(aFor.getUsages().get(3).identifier().textRange()).hasRange(9, 17, 9, 18);

    var aInFor = symbols.get(8);
    assertThat(aInFor).isSameAs(symbols.get(9));
    assertThat(aInFor.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aInFor.getUsages().get(0).identifier().textRange()).hasRange(10, 10, 10, 11);
    assertThat(aInFor.getUsages().get(1).identifier().textRange()).hasRange(11, 17, 11, 18);

    assertThat(aMain).isNotSameAs(aIf).isNotSameAs(aFor).isNotSameAs(aInFor);
    assertThat(aIf).isNotSameAs(aFor).isNotSameAs(aInFor);
    assertThat(aFor).isNotSameAs(aInFor);
  }

  @Test
  void testWithIfAndElse() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        if true {
          fmt.Printf(a)
          var a int = 2
          fmt.Printf(a)
        } else {
          fmt.Printf(a)
          var a int = 3
          fmt.Printf(a)
        }
      }
      """);
    assertThat(symbols).hasSize(7);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(1)).isSameAs(symbols.get(4));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 15, 5, 16);
    assertThat(aMain.getUsages().get(2).identifier().textRange()).hasRange(9, 15, 9, 16);

    var aIf = symbols.get(2);
    assertThat(aIf).isSameAs(symbols.get(3));
    assertThat(aIf.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aIf.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aIf.getUsages().get(1).identifier().textRange()).hasRange(7, 15, 7, 16);

    var aElse = symbols.get(5);
    assertThat(aElse).isSameAs(symbols.get(6));
    assertThat(aElse.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aElse.getUsages().get(0).identifier().textRange()).hasRange(10, 8, 10, 9);
    assertThat(aElse.getUsages().get(1).identifier().textRange()).hasRange(11, 15, 11, 16);

    assertThat(aMain).isNotSameAs(aIf).isNotSameAs(aElse);
    assertThat(aIf).isNotSameAs(aElse);
  }

  @Test
  void testWithIfElseIfElse() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        if 1 > 2 {
          fmt.Printf(a)
          var a int = 2
          fmt.Printf(a)
        } else if 2 < 1 {
          fmt.Printf(a)
          var a int = 3
          fmt.Printf(a)
        } else {
          fmt.Printf(a)
          var a int = 4
          fmt.Printf(a)
        }
      }
      """);
    assertThat(symbols).hasSize(10);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(1)).isSameAs(symbols.get(4)).isSameAs(symbols.get(7));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 15, 5, 16);
    assertThat(aMain.getUsages().get(2).identifier().textRange()).hasRange(9, 15, 9, 16);
    assertThat(aMain.getUsages().get(3).identifier().textRange()).hasRange(13, 15, 13, 16);

    var aIf = symbols.get(2);
    assertThat(aIf).isSameAs(symbols.get(3));
    assertThat(aIf.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aIf.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aIf.getUsages().get(1).identifier().textRange()).hasRange(7, 15, 7, 16);

    var aElseIf = symbols.get(5);
    assertThat(aElseIf).isSameAs(symbols.get(6));
    assertThat(aElseIf.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aElseIf.getUsages().get(0).identifier().textRange()).hasRange(10, 8, 10, 9);
    assertThat(aElseIf.getUsages().get(1).identifier().textRange()).hasRange(11, 15, 11, 16);

    var aElse = symbols.get(8);
    assertThat(aElse).isSameAs(symbols.get(9));
    assertThat(aElse.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aElse.getUsages().get(0).identifier().textRange()).hasRange(14, 8, 14, 9);
    assertThat(aElse.getUsages().get(1).identifier().textRange()).hasRange(15, 15, 15, 16);

    assertThat(aMain).isNotSameAs(aIf).isNotSameAs(aElseIf).isNotSameAs(aElse);
    assertThat(aIf).isNotSameAs(aElseIf).isNotSameAs(aElse);
    assertThat(aElseIf).isNotSameAs(aElse);
  }

  @Test
  void testSwitch() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        switch a := 2; a {
        case 1:
          fmt.Println(a)
          var a int = 3
          fmt.Println(a)
        case 2:
          fmt.Println(a)
          var a int = 4
          fmt.Println(a)
        default:
          fmt.Println(a)
          var a int = 5
          fmt.Println(a)
        }
        fmt.Println(a)
      }
      """);
    assertThat(symbols).hasSize(13);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(12));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(18, 14, 18, 15);

    var aSwitch = symbols.get(1);
    assertThat(aSwitch).isSameAs(symbols.get(2)).isSameAs(symbols.get(3)).isSameAs(symbols.get(6)).isSameAs(symbols.get(9));
    assertThat(aSwitch.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aSwitch.getUsages().get(0).identifier().textRange()).hasRange(4, 9, 4, 10);
    assertThat(aSwitch.getUsages().get(1).identifier().textRange()).hasRange(4, 17, 4, 18);
    assertThat(aSwitch.getUsages().get(2).identifier().textRange()).hasRange(6, 16, 6, 17);
    assertThat(aSwitch.getUsages().get(3).identifier().textRange()).hasRange(10, 16, 10, 17);
    assertThat(aSwitch.getUsages().get(4).identifier().textRange()).hasRange(14, 16, 14, 17);

    var aCase1 = symbols.get(4);
    assertThat(aCase1).isSameAs(symbols.get(5));
    assertThat(aCase1.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aCase1.getUsages().get(0).identifier().textRange()).hasRange(7, 8, 7, 9);
    assertThat(aCase1.getUsages().get(1).identifier().textRange()).hasRange(8, 16, 8, 17);

    var aCase2 = symbols.get(7);
    assertThat(aCase2).isSameAs(symbols.get(8));
    assertThat(aCase2.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aCase2.getUsages().get(0).identifier().textRange()).hasRange(11, 8, 11, 9);
    assertThat(aCase2.getUsages().get(1).identifier().textRange()).hasRange(12, 16, 12, 17);

    var aDefault = symbols.get(10);
    assertThat(aDefault).isSameAs(symbols.get(11));
    assertThat(aDefault.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aDefault.getUsages().get(0).identifier().textRange()).hasRange(15, 8, 15, 9);
    assertThat(aDefault.getUsages().get(1).identifier().textRange()).hasRange(16, 16, 16, 17);

    assertThat(aMain).isNotSameAs(aSwitch).isNotSameAs(aCase1).isNotSameAs(aCase2).isNotSameAs(aDefault);
    assertThat(aSwitch).isNotSameAs(aCase1).isNotSameAs(aCase2).isNotSameAs(aDefault);
    assertThat(aCase1).isNotSameAs(aCase2).isNotSameAs(aDefault);
    assertThat(aCase2).isNotSameAs(aDefault);
  }

  @Test
  void testInnerAnonymousFunction() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        defer func() {
          fmt.Println(a)
          var a int = 2
          fmt.Println(a)
        }()
        fmt.Println(a)
      }
      """);
    assertThat(symbols).hasSize(5);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(1)).isSameAs(symbols.get(4));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 16, 5, 17);
    assertThat(aMain.getUsages().get(2).identifier().textRange()).hasRange(9, 14, 9, 15);

    var aInner = symbols.get(2);
    assertThat(aInner).isSameAs(symbols.get(3));
    assertThat(aInner.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aInner.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aInner.getUsages().get(1).identifier().textRange()).hasRange(7, 16, 7, 17);

    assertThat(aMain).isNotSameAs(aInner);
  }

  @Test
  void testInnerAssignedFunction() {
    var symbols = parseAndGetSymbols("""
      package main
      func main() {
        var a int = 1
        myFunc := func() {
          fmt.Println(a)
          var a int = 2
          fmt.Println(a)
        }
        fmt.Println(a)
      }
      """);
    assertThat(symbols).hasSize(6);

    var aMain = symbols.get(0);
    assertThat(aMain).isSameAs(symbols.get(2)).isSameAs(symbols.get(5));
    assertThat(aMain.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE, Usage.UsageType.REFERENCE);
    assertThat(aMain.getUsages().get(0).identifier().textRange()).hasRange(3, 6, 3, 7);
    assertThat(aMain.getUsages().get(1).identifier().textRange()).hasRange(5, 16, 5, 17);
    assertThat(aMain.getUsages().get(2).identifier().textRange()).hasRange(9, 14, 9, 15);

    var myFunc = symbols.get(1);
    assertThat(myFunc.getUsages()).extracting("type").containsExactly(Usage.UsageType.DECLARATION);
    assertThat(myFunc.getUsages().get(0).identifier().textRange()).hasRange(4, 2, 4, 8);

    var aInner = symbols.get(3);
    assertThat(aInner).isSameAs(symbols.get(4));
    assertThat(aInner.getUsages()).extracting("type")
      .containsExactly(Usage.UsageType.DECLARATION, Usage.UsageType.REFERENCE);
    assertThat(aInner.getUsages().get(0).identifier().textRange()).hasRange(6, 8, 6, 9);
    assertThat(aInner.getUsages().get(1).identifier().textRange()).hasRange(7, 16, 7, 17);

    assertThat(aMain).isNotSameAs(myFunc).isNotSameAs(aInner);
    assertThat(myFunc).isNotSameAs(aInner);
  }

  @Test
  void testSymbolVisitorCleanUpItsStateAfterVisit() {
    var ast = TestGoConverter.parse("package main\n const x = 1");
    SymbolVisitor<TreeContext> visitor = new SymbolVisitor<>();
    visitor.scan(mock(), ast);
    var secondAst = TestGoConverter.parse("package main\n func main() { x }");
    visitor.scan(mock(), secondAst);
    Optional<IdentifierTree> xSymbol = secondAst.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .filter(identifier -> identifier.name().equals("x"))
      .findFirst();
    assertThat(xSymbol).isPresent();
    // We do not track cross file symbols.
    assertThat(xSymbol.get().symbol()).isNull();
  }

  @Test
  void symbolVisitorShouldCleanItsSymbolTableAfterEachFile() {
    var ast1 = TestGoConverter.parse("package main\n const x = 1");
    var ast2 = TestGoConverter.parse("package main\n const x = 2");
    SymbolVisitor<TreeContext> visitor = new SymbolVisitor<>();
    visitor.scan(mock(), ast1);
    visitor.scan(mock(), ast2);

    Optional<IdentifierTree> xSymbolAst1 = ast1.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .filter(identifier -> identifier.name().equals("x"))
      .findFirst();
    Optional<IdentifierTree> xSymbolAst2 = ast2.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .filter(identifier -> identifier.name().equals("x"))
      .findFirst();

    assertThat(xSymbolAst1).isPresent();
    assertThat(xSymbolAst2).isPresent();
    assertThat(xSymbolAst1.get().symbol().getUsages()).hasSize(1);
    assertThat(xSymbolAst2.get().symbol().getUsages()).hasSize(1);
  }

  /**
   * Parse a go code and provide the symbols in order of presence in the code.
   */
  private List<Symbol> parseAndGetSymbols(String code) {
    var ast = TestGoConverter.parse(code);
    new SymbolVisitor<>().scan(mock(), ast);
    var symbols = new ArrayList<Symbol>();
    var symbolsRetriever = new TreeVisitor<>();
    symbolsRetriever.register(IdentifierTree.class, (ctx, identifier) -> {
      var symbol = identifier.symbol();
      if (symbol != null) {
        symbols.add(symbol);
      }
    });
    symbolsRetriever.scan(mock(), ast);
    return symbols;
  }
}
