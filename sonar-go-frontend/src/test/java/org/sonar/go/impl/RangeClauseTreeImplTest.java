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
package org.sonar.go.impl;

import org.junit.jupiter.api.Test;
import org.sonar.go.testing.TestGoConverterSingleFile;
import org.sonar.go.utils.TreeCreationUtils;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.PlaceHolderTree;
import org.sonar.plugins.go.api.RangeClauseTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.utils.SyntacticEquivalence.areEquivalent;

class RangeClauseTreeImplTest {

  @Test
  void shouldProvideKeyValueAndRangedExpressionOfDeclaringClause() {
    var clause = parseClause("""
      package main
      func iterate(entries []string) {
        for index, entry := range entries {
          _, _ = index, entry
        }
      }
      """);

    assertThat(clause.isDeclaration()).isTrue();
    assertThat(((IdentifierTree) clause.key()).name()).isEqualTo("index");
    assertThat(((IdentifierTree) clause.value()).name()).isEqualTo("entry");
    assertThat(((IdentifierTree) clause.rangedExpression()).name()).isEqualTo("entries");
    assertThat(clause.children()).containsExactly(clause.key(), clause.value(), clause.rangedExpression());
  }

  @Test
  void shouldNotBeADeclarationWhenTheClauseAssigns() {
    var clause = parseClause("""
      package main
      func iterate(entries []string) {
        var index int
        var entry string
        for index, entry = range entries {
          _, _ = index, entry
        }
      }
      """);

    assertThat(clause.isDeclaration()).isFalse();
    assertThat(((IdentifierTree) clause.key()).name()).isEqualTo("index");
    assertThat(((IdentifierTree) clause.value()).name()).isEqualTo("entry");
  }

  @Test
  void shouldProvideNoKeyNorValueWhenTheClauseHasNone() {
    var clause = parseClause("""
      package main
      func iterate(entries []string) {
        for range entries {
        }
      }
      """);

    assertThat(clause.isDeclaration()).isFalse();
    assertThat(clause.key()).isNull();
    assertThat(clause.value()).isNull();
    assertThat(clause.children()).containsExactly(clause.rangedExpression());
  }

  @Test
  void shouldProvideNoValueWhenTheClauseHasOnlyAKey() {
    var clause = parseClause("""
      package main
      func iterate(entries []string) {
        for index := range entries {
          _ = index
        }
      }
      """);

    assertThat(((IdentifierTree) clause.key()).name()).isEqualTo("index");
    assertThat(clause.value()).isNull();
  }

  @Test
  void shouldProvideAPlaceHolderForAnIgnoredName() {
    var clause = parseClause("""
      package main
      func iterate(entries []string) {
        for _, entry := range entries {
          _ = entry
        }
      }
      """);

    assertThat(clause.key()).isInstanceOf(PlaceHolderTree.class);
    assertThat(((IdentifierTree) clause.value()).name()).isEqualTo("entry");
  }

  @Test
  void shouldNotBeTheConditionOfAPlainForLoop() {
    var loop = TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, """
      package main
      func count() {
        for index := 0; index < 3; index++ {
        }
      }
      """);

    assertThat(loop.condition()).isNotInstanceOf(RangeClauseTree.class);
  }

  @Test
  void shouldNotBeEquivalentToAClauseThatOnlyAssigns() {
    // "for index, entry := range entries" and "for index, entry = range entries" hold the same children, so only
    // isDeclaration tells them apart
    assertThat(areEquivalent(rangeClause(true), rangeClause(true))).isTrue();
    assertThat(areEquivalent(rangeClause(false), rangeClause(false))).isTrue();
    assertThat(areEquivalent(rangeClause(true), rangeClause(false))).isFalse();
  }

  private static RangeClauseTreeImpl rangeClause(boolean isDeclaration) {
    return new RangeClauseTreeImpl(null, TreeCreationUtils.identifier("index"), TreeCreationUtils.identifier("entry"),
      TreeCreationUtils.identifier("entries"), isDeclaration);
  }

  private static RangeClauseTree parseClause(String code) {
    return (RangeClauseTree) TestGoConverterSingleFile.parseAndRetrieve(LoopTree.class, code).condition();
  }
}
