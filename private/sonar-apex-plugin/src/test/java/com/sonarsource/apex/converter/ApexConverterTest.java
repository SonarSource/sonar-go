/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import com.sonarsource.apex.converter.ApexConverter.StronglyTypedConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.plugin.converter.ASTConverterValidation;
import org.sonarsource.slang.visitors.TreePrinter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApexConverterTest {

  public static ASTConverter converter() {
    return new ASTConverterValidation(new ApexConverter(), ASTConverterValidation.ValidationMode.THROW_EXCEPTION);
  }

  @Test
  void parser_error() {
    ASTConverter converter = converter();
    assertThatThrownBy(() -> converter.parse("invalid apex code"))
      .isInstanceOf(ParseException.class)
      .hasMessage("ParseException: Syntax(error = UnexpectedToken(loc = (1, 9, 2, 6), token = 'apex'))")
      .extracting(ex -> "line " + ((ParseException) ex).getPosition().line() + " col " + ((ParseException) ex).getPosition().lineOffset())
      .hasToString("line 1 col 8");
    converter.terminate();
  }

  @Test
  void all_cls_files() throws IOException {
    for (Path apexPath : getApexSources()) {
      Path astPath = Paths.get(apexPath.toString().replaceFirst("\\.cls$", ".txt"));
      String actualAst = TreePrinter.table(parse(apexPath));
      String expectingAst = astPath.toFile().exists() ? new String(Files.readAllBytes(astPath), UTF_8) : "";
      assertThat(actualAst)
        .describedAs("In the file: " + astPath + " (run ApexConverterTest.main manually)")
        .isEqualTo(expectingAst);
    }
  }

  public static void main(String[] args) throws IOException {
    fix_all_cls_files_test_automatically();
  }

  private static void fix_all_cls_files_test_automatically() throws IOException {
    for (Path apexPath : getApexSources()) {
      Path astPath = Paths.get(apexPath.toString().replaceFirst("\\.cls$", ".txt"));
      String actualAst = TreePrinter.table(parse(apexPath));
      Files.write(astPath, actualAst.getBytes(UTF_8));
    }
  }

  public static List<Path> getApexSources() throws IOException {
    try (Stream<Path> pathStream = Files.walk(Paths.get("src", "test", "java", "com", "sonarsource", "apex", "converter", "ast"))) {
      return pathStream
        .filter(path -> !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".cls"))
        .sorted()
        .collect(Collectors.toList());
    }
  }

  @Test
  void top_level_tree() {
    Tree tree = converter().parse("class A { // c1\n  /* c2 */\n}\n");
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    assertThat(tree.textRange()).hasToString("TextRange[1, 0, 3, 1]");

    TreeMetaData metaData = tree.metaData();
    assertThat(metaData.tokens()).hasSize(4);
    assertThat(metaData.tokens().get(0).text()).isEqualTo("class");
    assertThat(metaData.tokens().get(0).type()).isEqualTo(Token.Type.KEYWORD);
    assertThat(metaData.tokens().get(0).textRange()).hasToString("TextRange[1, 0, 1, 5]");

    assertThat(metaData.tokens().get(3).text()).isEqualTo("}");
    assertThat(metaData.tokens().get(3).type()).isEqualTo(Token.Type.OTHER);
    assertThat(metaData.tokens().get(3).textRange()).hasToString("TextRange[3, 0, 3, 1]");

    assertThat(metaData.commentsInside()).hasSize(2);
    assertThat(metaData.commentsInside().get(0).text()).isEqualTo("// c1");
    assertThat(metaData.commentsInside().get(1).text()).isEqualTo("/* c2 */");
  }

  @Test
  void new_metadata_including_ranges() {
    StronglyTypedConverter converter = new StronglyTypedConverter(Lexer.parse("String txt;"));
    TreeMetaData metaData = converter.metaDataProvider().metaData(new TextRangeImpl(1, 7, 1, 10));
    assertThat(metaData.tokens()).hasSize(1);
    assertThat(metaData.textRange()).hasToString("TextRange[1, 7, 1, 10]");
    metaData = converter.newMetadataIncluding(metaData, new TextRangeImpl(1, 0, 1, 6));
    assertThat(metaData.tokens()).hasSize(2);
    assertThat(metaData.textRange()).hasToString("TextRange[1, 0, 1, 10]");
    metaData = converter.newMetadataIncluding(metaData, new TextRangeImpl(1, 10, 1, 11));
    assertThat(metaData.tokens()).hasSize(3);
    assertThat(metaData.textRange()).hasToString("TextRange[1, 0, 1, 11]");
    metaData = converter.newMetadataIncluding(metaData, new TextRangeImpl(1, 10, 1, 11));
    assertThat(metaData.textRange()).hasToString("TextRange[1, 0, 1, 11]");
  }

  @Test
  void identifiers_and_keywords_ambiguity() {
    Tree tree = converter().parse("" +
      "trigger trigger2 on Account (before delete, after insert) {\n" +
      "    Account before = Trigger.old;\n" +
      "    Integer when = before.type, switch = 5;\n" +
      "    switch on when {\n" +
      "      when 2 {\n" +
      "          System.debug('xxx');\n" +
      "      }\n" +
      "      when else {\n" +
      "      }\n" +
      "    }\n" +
      "}\n");

    String tokenList = tree.metaData().tokens().stream()
      .map(token -> token.type() == Token.Type.KEYWORD ? "[" + token.text() + "]" : token.text())
      .collect(Collectors.joining("\n"));

    assertThat(tokenList).isEqualTo(("" +
      "[trigger] trigger2 [on] Account ( [before] [delete] , [after] [insert] ) {" +
      "    Account before = Trigger . old ;" +
      "    Integer when = before . type , switch = 5 ;" +
      "    [switch] [on] when {" +
      "      [when] 2 {" +
      "          System . debug ( 'xxx' ) ;" +
      "      }" +
      "      [when] [else] {" +
      "      }" +
      "    }" +
      " }").replaceAll(" +", "\n"));
  }

  public static Tree parse(Path path) throws IOException {
    String code = new String(Files.readAllBytes(path), UTF_8);
    try {
      return converter().parse(code);
    } catch (ParseException e) {
      throw new ParseException(e.getMessage() + " in file " + path, e.getPosition(), e);
    } catch (RuntimeException e) {
      throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage() + " in file " + path, e);
    }
  }

}
