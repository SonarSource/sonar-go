package org.sonarsource.apex.parser.poc.compiler;

import apex.jorje.semantic.ast.compilation.Compilation;
import java.io.IOException;
import org.junit.Test;
import org.sonarsource.apex.parser.poc.format.FormatAstNode;

import static org.sonarsource.apex.parser.poc.utils.TestUtils.assertFileContent;
import static org.sonarsource.apex.parser.poc.utils.TestUtils.readFile;

public class CompilerTest {

  @Test
  public void empty_class() throws IOException {
    assertFileContent(compilerReport("sources/empty_class.cls"), "compiler/CompilerTest.empty_class.md");
  }

  @Test
  public void one_field() throws IOException {
    assertFileContent(compilerReport("sources/one_field.cls"), "compiler/CompilerTest.one_field.md");
  }

  @Test
  public void one_method() throws IOException {
    assertFileContent(compilerReport("sources/one_method.cls"), "compiler/CompilerTest.one_method.md");
  }

  private static String compilerReport(String apexSourcePath) throws IOException {
    String code = readFile(apexSourcePath);

    StringBuilder out = new StringBuilder();
    out.append("## Source Code\n");
    out.append("```java\n");
    out.append(code);
    out.append("```\n");

    Compilation compilation = Compiler.parse(code);
    out.append("## AST\n");
    out.append(FormatAstNode.table(compilation)).append("\n");
    return out.toString();
  }

}
