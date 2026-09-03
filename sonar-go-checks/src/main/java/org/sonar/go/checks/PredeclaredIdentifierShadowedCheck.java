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
package org.sonar.go.checks;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.go.symbols.Usage;
import org.sonar.go.utils.NativeKinds;
import org.sonar.go.utils.VariableHelper;
import org.sonar.plugins.go.api.ClassDeclarationTree;
import org.sonar.plugins.go.api.FunctionDeclarationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.ImportSpecificationTree;
import org.sonar.plugins.go.api.IndexExpressionTree;
import org.sonar.plugins.go.api.IndexListExpressionTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.NativeTree;
import org.sonar.plugins.go.api.ParameterTree;
import org.sonar.plugins.go.api.StarExpressionTree;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.VariableDeclarationTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;

@Rule(key = "S978")
public class PredeclaredIdentifierShadowedCheck implements GoCheck {

  /**
   * The identifiers of the universe block, the outermost scope of a program, that a declaration can shadow.
   */
  private static final Set<String> PREDECLARED_IDENTIFIERS = Set.of(
    // Types
    "any", "bool", "byte", "comparable", "complex64", "complex128", "error", "float32", "float64",
    "int", "int8", "int16", "int32", "int64", "rune", "string",
    "uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
    // Constants
    "true", "false", "iota",
    // Zero value
    "nil",
    // Functions
    "append", "cap", "clear", "close", "complex", "copy", "delete", "imag", "len",
    "make", "max", "min", "new", "panic", "print", "println", "real", "recover");

  private static final String TYPE_SPECIFICATION_KIND_SUFFIX = "(TypeSpec)";
  private static final String TYPE_PARAMETERS_KIND = "TypeParams";
  private static final String FIELD_LIST_KIND = "FieldList";
  private static final String RANGE_HEADER_KIND = "RangeHeader";
  private static final String TOKEN_KIND = "Tok";
  private static final String DEFINE_TOKEN = ":=";

  @Override
  public void initialize(InitContext init) {
    // "var" and "const" declarations, short variable declarations, "if"/"for"/"switch" initializers, type switch
    // guards and "select" communication clauses
    init.register(VariableDeclarationTree.class, (ctx, tree) -> tree.identifiers().stream()
      .filter(PredeclaredIdentifierShadowedCheck::isDeclaringUsage)
      .forEach(identifier -> checkIdentifier(ctx, identifier)));
    init.register(FunctionDeclarationTree.class, PredeclaredIdentifierShadowedCheck::checkFunctionDeclaration);
    init.register(ClassDeclarationTree.class, PredeclaredIdentifierShadowedCheck::checkTypeDeclaration);
    init.register(ImportSpecificationTree.class, (ctx, tree) -> checkIdentifier(ctx, tree.name()));
    init.register(LoopTree.class, PredeclaredIdentifierShadowedCheck::checkRangeClause);
    init.register(NativeTree.class, PredeclaredIdentifierShadowedCheck::checkTypeSpecification);
  }

  private static void checkFunctionDeclaration(CheckContext ctx, FunctionDeclarationTree tree) {
    // A method name lives in the namespace of its receiver type, it shadows nothing
    if (tree.receiver() == null) {
      checkIdentifier(ctx, tree.name());
    }
    tree.formalParameters().stream()
      .filter(ParameterTree.class::isInstance)
      .map(ParameterTree.class::cast)
      .forEach(parameter -> checkIdentifier(ctx, parameter.identifier()));
    checkFieldNames(ctx, tree.typeParameters());
    // Only named results have identifiers to report, an unnamed result list holds types alone
    checkFieldNames(ctx, tree.returnType());
    checkReceiver(ctx, tree.receiver());
  }

  private static void checkReceiver(CheckContext ctx, @Nullable Tree receiver) {
    if (receiver == null) {
      return;
    }
    checkFieldNames(ctx, receiver);
    // The type arguments of a receiver, as in "func (p *pair[key]) get()", are always identifiers declaring the type
    // parameters of the method, so they shadow for the whole method scope
    VariableHelper.getFields(receiver)
      .flatMap(field -> field.children().stream())
      .map(type -> type instanceof StarExpressionTree pointer ? pointer.operand() : type)
      .forEach(type -> checkTypeArguments(ctx, type));
  }

  private static void checkTypeArguments(CheckContext ctx, Tree type) {
    if (type instanceof IndexExpressionTree singleArgument) {
      checkIdentifier(ctx, singleArgument.index());
    } else if (type instanceof IndexListExpressionTree arguments) {
      checkIdentifiers(ctx, arguments.indices());
    }
  }

  private static void checkTypeDeclaration(CheckContext ctx, ClassDeclarationTree tree) {
    checkIdentifier(ctx, tree.identifier());
    checkTypeParameters(ctx, tree.classTree().children());
  }

  /**
   * A parenthesized {@code type} declaration holds no identifier of its own, so the converter leaves it native and
   * maps each of its specifications to a {@code (TypeSpec)} native tree, which starts with the declared name and can
   * be followed by the type parameters. Unlike a single {@code type} declaration, which is a
   * {@link ClassDeclarationTree} wrapping a {@code (TypeSpecWrapped)} native tree.
   */
  private static void checkTypeSpecification(CheckContext ctx, NativeTree tree) {
    if (!NativeKinds.isStringNativeKind(tree, kind -> kind.endsWith(TYPE_SPECIFICATION_KIND_SUFFIX))) {
      return;
    }
    var children = tree.children();
    children.stream()
      .filter(IdentifierTree.class::isInstance)
      .findFirst()
      .ifPresent(name -> checkIdentifier(ctx, name));
    checkTypeParameters(ctx, children);
  }

  private static void checkTypeParameters(CheckContext ctx, List<Tree> children) {
    children.stream()
      .filter(child -> NativeKinds.isStringNativeKindOfType(child, TYPE_PARAMETERS_KIND, FIELD_LIST_KIND))
      .forEach(typeParameters -> checkFieldNames(ctx, typeParameters));
  }

  /**
   * The key and the value of a {@code range} clause using {@code :=} are declarations, unlike the ones of a
   * {@code range} clause using {@code =}, which assign to already declared variables.
   */
  private static void checkRangeClause(CheckContext ctx, LoopTree tree) {
    var header = tree.condition();
    if (header == null || !NativeKinds.isStringNativeKindOfType(header, RANGE_HEADER_KIND)) {
      return;
    }
    // The children before the assignment token are the key and the value of the clause, the ones after it are the ranged expression
    var children = header.children();
    for (var i = 0; i < children.size(); i++) {
      if (NativeKinds.isStringNativeKindOfType(children.get(i), TOKEN_KIND)) {
        if (isDefineToken(children.get(i))) {
          checkIdentifiers(ctx, children.subList(0, i));
        }
        return;
      }
    }
  }

  /**
   * Reports the names declared by the fields of a {@code FieldList} native tree: the parameters, the named results,
   * the receiver or the type parameters of a function, or the type parameters of a type declaration.
   */
  private static void checkFieldNames(CheckContext ctx, @Nullable Tree fieldList) {
    VariableHelper.getFieldNames(fieldList).forEach(identifier -> checkIdentifier(ctx, identifier));
  }

  /**
   * A short variable declaration only declares the names that are new in its scope, the others are plain assignments:
   * {@code max, err := f()} followed by {@code max, other := g()} declares {@code max} once, and a name already
   * declared as a parameter is only assigned by a later {@code :=}. All those usages resolve to the same symbol, so
   * only its first declaring usage shadows the predeclared identifier.
   */
  private static boolean isDeclaringUsage(IdentifierTree identifier) {
    var symbol = identifier.symbol();
    if (symbol == null) {
      // Without a symbol, this usage is the only one known
      return true;
    }
    return symbol.getUsages().stream()
      .filter(usage -> usage.type() == Usage.UsageType.DECLARATION || usage.type() == Usage.UsageType.PARAMETER)
      .findFirst()
      .map(usage -> usage.identifier() == identifier)
      .orElse(true);
  }

  private static void checkIdentifiers(CheckContext ctx, List<? extends Tree> trees) {
    trees.forEach(tree -> checkIdentifier(ctx, tree));
  }

  private static void checkIdentifier(CheckContext ctx, @Nullable Tree tree) {
    if (tree instanceof IdentifierTree identifier && PREDECLARED_IDENTIFIERS.contains(identifier.name())) {
      ctx.reportIssue(identifier, "Rename this identifier, \"" + identifier.name() + "\" shadows a predeclared identifier.");
    }
  }

  private static boolean isDefineToken(Tree tree) {
    return tree.metaData().tokens().stream().anyMatch(token -> DEFINE_TOKEN.equals(token.text()));
  }
}
