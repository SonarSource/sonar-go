package org.sonarsource.apex.parser.poc.compiler;

import apex.jorje.data.Locations;
import apex.jorje.parser.impl.BaseApexLexer;
import apex.jorje.semantic.ast.compilation.Compilation;
import apex.jorje.semantic.common.EmptySymbolProvider;
import apex.jorje.semantic.common.TestAccessEvaluator;
import apex.jorje.semantic.common.TestQueryValidators;
import apex.jorje.semantic.compiler.ApexCompiler;
import apex.jorje.semantic.compiler.CodeUnit;
import apex.jorje.semantic.compiler.CompilationInput;
import apex.jorje.semantic.compiler.CompilerStage;
import apex.jorje.semantic.compiler.SourceFile;
import apex.jorje.semantic.compiler.parser.ParserEngine;
import java.util.Collections;
import java.util.List;

public final class Compiler {

  static {
    // Disable ApexLexer log: INFO: Deduped array ApexLexer.DFA22_transition
    java.util.logging.Logger log = java.util.logging.Logger.getLogger(BaseApexLexer.class.getName());
    log.setLevel(java.util.logging.Level.WARNING);
    // Fulfill apex.jorje.data.Location "getStartIndex()" and "getEndIndex()" during parsing
    Locations.useIndexFactory();
  }

  private Compiler() {
    // utility class
  }

  public static Compilation parse(String source) {
    SourceFile sourceFile = SourceFile.builder()
      .setFileBased(true)
      .setTrusted(true)
      .setBody(source)
      .build();

    List<SourceFile> sourceFiles = Collections.singletonList(sourceFile);

    CompilationInput compilationInput = new CompilationInput(
      sourceFiles,
      EmptySymbolProvider.get(),
      new TestAccessEvaluator(),
      new TestQueryValidators.Noop(),
      null);

    ApexCompiler compiler = ApexCompiler.builder()
      .setHiddenTokenBehavior(ParserEngine.HiddenTokenBehavior.COLLECT_COMMENTS)
      .setInput(compilationInput)
      .build();

    List<CodeUnit> codeUnits = compiler.compile(CompilerStage.POST_TYPE_RESOLVE);

    CodeUnit codeUnit = codeUnits.get(0);
    if (!codeUnit.getErrors().isEmpty()) {
      throw new IllegalStateException("Parse Error: " + codeUnit.getErrors().getFirst().getMessage());
    }
    return codeUnit.getNode();
  }

}
