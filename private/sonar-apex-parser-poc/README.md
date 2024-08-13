# Apex Parser POC (Proof Of Concept)

## Import apex-jorje-lsp.jar
```bash
./gradlew downloadAndVerifyDependency
```

## Build
```bash
./gradlew build
```

## Goal of this POC

The goal of this POC is to do a technical validation of the _Apex Parser_ provided by Salesforce in their
_Salesforce Extensions for VS Code_ available on github [salesforcedx-vscode](https://github.com/forcedotcom/salesforcedx-vscode) repository.

The validation will answer the question:
* Could we use Salesforce's _Apex Parser_ to build an SLang AST?

## What is Apex code?

It's a programing language similar to java.
[Apex Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_dev_guide.htm)

Note: Apex is a case-insensitive language, it will be the first of this kind converted to SLang, and this could have an impact on rule implementations.

## Salesforce's _Apex Parser_ dependency

Salesforce does not distribute its _Apex Parser_ through any dependency management repository,
and as of today it is not open source.
The only way to depend on it, is to download `apex-jorje-lsp.jar` from github [salesforcedx-vscode-apex/out](https://github.com/forcedotcom/salesforcedx-vscode/tree/master/packages/salesforcedx-vscode-apex/out).

This POC project has a `downloadAndVerifyDependency` gradle task in [build.gradle](build.gradle) to download the jar into `build/libs`.

It's a 17M fat jar, that contains a lot of dependencies probably not require in our context, like: testng, aspectj, hamcrest, guava, gson, slf4j, apache/commons/lang3, snakeyaml, log4j, logback
eclipse/xtend2, eclipse/lsp4j, eclipse/jdt, apache/axis, checkerframework, objectweb, google/common, jcommander

Reducing the dependency size and preventing dependency conflicts are not in the scope of this POC.

## Usage example of _Apex Parser_

There's no available documentation, no source code, but the jar is not obfuscated and can be easily explored or debugged using an IDE.

`pmd-apex` is a project that already uses this _Apex Parser_. PMD contributors created their own reduced dependency jar [pmd-apex-jorje](https://github.com/pmd/pmd/blob/master/pmd-apex-jorje).
Debugging one of [ApexParserTest.java](https://github.com/pmd/pmd/blob/master/pmd-apex/src/test/java/net/sourceforge/pmd/lang/apex/ast/ApexParserTest.java) test provides a good overview of
the parser usage. Even if uses an old version of it.

## Lexer - apex.jorje.parser.impl.ApexLexer

The [Lexer](src/main/java/org/sonarsource/apex/parser/poc/lexer/Lexer.java) class shows usage of the ApexLexer. It converts apex source code into `org.antlr.runtime.CommonToken` list.
Example of outputs:
* [LexerTest.empty_class.md](src/test/java/org/sonarsource/apex/parser/poc/lexer/LexerTest.empty_class.md)
* [LexerTest.one_field.md](src/test/java/org/sonarsource/apex/parser/poc/lexer/LexerTest.one_field.md)
* [LexerTest.one_method.md](src/test/java/org/sonarsource/apex/parser/poc/lexer/LexerTest.one_method.md)

Difficulties experienced during implementation of the `Lexer` class:
* A workaround is needed to prevent `ApexLexer` to log unwanted INFO messages using `java.util.logging.Logger`.
* A method of `ApexLexer` needed to be overridden to prevent it to write to `System.err.println`.
* To collect `start` and `end` on tokens, a global variable needs to defined through the `Locations.useIndexFactory()` call.

Conclusion: Even if `ApexLexer` is not perfectly designed, we can use it in our context.

## Compiler - apex.jorje.semantic.compiler.ApexCompiler

The [Compiler](src/main/java/org/sonarsource/apex/parser/poc/compiler/Compiler.java) class shows usage of the ApexCompiler. It converts apex source code into `apex.jorje.semantic.ast.compilation.Compilation` tree.
Example of outputs:
* [CompilerTest.empty_class.md](src/test/java/org/sonarsource/apex/parser/poc/compiler/CompilerTest.empty_class.md)
* [CompilerTest.one_field.md](src/test/java/org/sonarsource/apex/parser/poc/compiler/CompilerTest.one_field.md)
* [CompilerTest.one_method.md](src/test/java/org/sonarsource/apex/parser/poc/compiler/CompilerTest.one_method.md)

Note: The AST matches source code tree plus generated code like `<clinit>`, `<init>` and `clone` methods.

The `Compilation` AST nodes do not contain tokens, and provided locations can not be used to retrieve them.
For example, `UserClass` location match only it's name identifier.

Conclusion: Without tokens, the Compilation AST can not be used in our context.

## Parser - apex.jorje.parser.impl.ApexParser

The [Parser](src/main/java/org/sonarsource/apex/parser/poc/parser/Parser.java) class shows usage of the ApexParser. It converts apex source code into `apex.jorje.data.ast.CompilationUnit` tree.
Example of outputs:
* [ParserTest.empty_class.md](src/test/java/org/sonarsource/apex/parser/poc/parser/ParserTest.empty_class.md)
* [ParserTest.one_field.md](src/test/java/org/sonarsource/apex/parser/poc/parser/ParserTest.one_field.md)
* [ParserTest.one_method.md](src/test/java/org/sonarsource/apex/parser/poc/parser/ParserTest.one_method.md)

The `CompilationUnit` AST nodes do not contain tokens, but it's possible to retrieve then given the node location.
Node location is nullable, but in case of null value, location can be retrieve from children.

This parser is able to parse without error the 300'000 lines of code of the [its/sources/apex](../its/sources/apex) folder.

The `CompilationUnit` tree is not convenient to convert. Each AST node does not inherit from a common interface that provide the location, and there's no visitor.
The ApexParser is generated using `ANTLR`, but we don't have the original `apex/jorje/parser/impl/ApexParser.g` grammar file to generate a more suitable parser.
This POC has generated some code ([SimpleConverterGenerated.java](src/main/java/org/sonarsource/apex/parser/poc/parser/SimpleConverterGenerated.java)) to be able to visit the tree.

Conclusion: We can use ApexParser to build a SLang AST
