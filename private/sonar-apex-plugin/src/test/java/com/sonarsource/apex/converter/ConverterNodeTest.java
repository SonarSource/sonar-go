/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import com.sonarsource.apex.converter.visitor.GeneratedApexAstVisitor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterNodeTest {

  @Test
  void nativeNode() {
    String content = "native node";
    ConverterNode converterNode = new ConverterNode(content);
    assertThat(converterNode.nativeNode()).isEqualTo(content);
  }

  @Test
  void children_per_property() throws IOException {
    List<Path> testFiles = ApexConverterTest.getApexSources();
    Tree actualAst = ApexConverterTest.parse(testFiles.get(0));
    ConverterNode converterNode = new ConverterNode("");
    converterNode.addChild(GeneratedApexAstVisitor.Property.IfBlock.EXPR, actualAst);
    assertThat(converterNode.children(GeneratedApexAstVisitor.Property.IfBlock.EXPR).get(0)).isEqualTo(actualAst);
  }

  @Test
  void children_no_property() {
    ConverterNode converterNode = new ConverterNode("");
    assertThat(converterNode.children(null)).isEmpty();
    assertThat(converterNode.children()).isEmpty();
  }
}
