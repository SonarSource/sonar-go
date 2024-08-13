package org.sonarsource.apex.parser.poc.lexer;

import java.io.IOException;
import org.junit.Test;
import org.sonarsource.apex.parser.poc.format.FormatTokens;

import static org.sonarsource.apex.parser.poc.utils.TestUtils.assertFileContent;
import static org.sonarsource.apex.parser.poc.utils.TestUtils.readFile;

public class LexerTest {

  @Test
  public void empty_class() throws IOException {
    assertFileContent(lexerReport("sources/empty_class.cls"), "lexer/LexerTest.empty_class.md");
  }

  @Test
  public void one_field() throws IOException {
    assertFileContent(lexerReport("sources/one_field.cls"), "lexer/LexerTest.one_field.md");
  }

  @Test
  public void one_method() throws IOException {
    assertFileContent(lexerReport("sources/one_method.cls"), "lexer/LexerTest.one_method.md");
  }

  private static String lexerReport(String apexSourcePath) throws IOException {
    String code = readFile(apexSourcePath);

    StringBuilder out = new StringBuilder();
    out.append("## Source Code\n");
    out.append("```java\n");
    out.append(code);
    out.append("```\n");

    Tokens tokens = Lexer.parse(code);
    out.append("## Tokens\n");
    out.append(FormatTokens.table(tokens)).append("\n");
    return out.toString();
  }

}
