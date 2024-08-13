package org.sonarsource.apex.parser.poc.parser;

import apex.jorje.parser.impl.ApexLexer;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.sonarsource.apex.parser.poc.format.FormatCompilationUnit;
import org.sonarsource.apex.parser.poc.format.FormatTokens;

import static org.sonarsource.apex.parser.poc.utils.TestUtils.assertFileContent;
import static org.sonarsource.apex.parser.poc.utils.TestUtils.readFile;

public class ParserTest {

  @Test
  public void empty_class() throws IOException, RecognitionException {
    assertFileContent(parserReport("sources/empty_class.cls"), "parser/ParserTest.empty_class.md");
  }

  @Test
  public void one_field() throws IOException, RecognitionException {
    assertFileContent(parserReport("sources/one_field.cls"), "parser/ParserTest.one_field.md");
  }

  @Test
  public void one_method() throws IOException, RecognitionException {
    assertFileContent(parserReport("sources/one_method.cls"), "parser/ParserTest.one_method.md");
  }

  private static String parserReport(String apexSourcePath) throws IOException, RecognitionException {
    String code = readFile(apexSourcePath);

    StringBuilder out = new StringBuilder();
    out.append("## Source Code\n");
    out.append("```java\n");
    out.append(code);
    out.append("```\n");

    SimpleAstNode compilationUnit = Parser.parse(code);
    out.append("## AST\n");
    out.append(FormatCompilationUnit.table(compilationUnit)).append("\n");

    Collection<CommonToken> comments = compilationUnit.tokens.stream()
      .filter(token -> token.getType() == ApexLexer.BLOCK_COMMENT || token.getType() == ApexLexer.EOL_COMMENT)
      .collect(Collectors.toList());

    out.append("## Comments\n");
    if (!comments.isEmpty()) {
      out.append(FormatTokens.table(comments)).append("\n");
    }
    return out.toString();
  }

}
