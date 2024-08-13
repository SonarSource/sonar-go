/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import apex.jorje.data.Identifier;
import apex.jorje.data.Location;
import apex.jorje.data.ast.AssignmentOp;
import apex.jorje.data.ast.BinaryOp;
import apex.jorje.data.ast.BlockMember;
import apex.jorje.data.ast.BooleanOp;
import apex.jorje.data.ast.CatchBlock;
import apex.jorje.data.ast.ClassDecl;
import apex.jorje.data.ast.CompilationUnit;
import apex.jorje.data.ast.ElseBlock;
import apex.jorje.data.ast.Expr;
import apex.jorje.data.ast.IfBlock;
import apex.jorje.data.ast.MethodDecl;
import apex.jorje.data.ast.Modifier;
import apex.jorje.data.ast.ParameterRef;
import apex.jorje.data.ast.PostfixOp;
import apex.jorje.data.ast.PrefixOp;
import apex.jorje.data.ast.Stmnt;
import apex.jorje.data.ast.Stmnt.BlockStmnt;
import apex.jorje.data.ast.Stmnt.SwitchStmnt;
import apex.jorje.data.ast.VariableDecl;
import apex.jorje.data.ast.VariableDecls;
import apex.jorje.data.ast.WhenBlock;
import com.google.common.collect.Lists;
import com.sonarsource.apex.converter.impl.ApexIdentifierTreeImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BaseTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.CatchTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.FunctionInvocationTreeImpl;
import org.sonarsource.slang.impl.IfTreeImpl;
import org.sonarsource.slang.impl.JumpTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.LoopTreeImpl;
import org.sonarsource.slang.impl.MatchCaseTreeImpl;
import org.sonarsource.slang.impl.MatchTreeImpl;
import org.sonarsource.slang.impl.MemberSelectTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.ParameterTreeImpl;
import org.sonarsource.slang.impl.ParenthesizedExpressionTreeImpl;
import org.sonarsource.slang.impl.ReturnTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.ThrowTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

public class ApexConverter implements ASTConverter {

  @Override
  public Tree parse(String content) {
    CompilationUnit compilationUnit = Parser.parse(content);
    StronglyTypedConverter converter = new StronglyTypedConverter(Lexer.parse(content));
    converter._case(compilationUnit);
    return converter.topLevelTree();
  }

  static class StronglyTypedConverter extends ApexToSlangNativeConverter {
    private static final NativeKind ANNOTATION_KIND = new ClassNativeKind(Modifier.Annotation.class);
    private static final NativeKind TEST_METHOD_MODIFIER_KIND = new ClassNativeKind(Modifier.TestMethodModifier.class);

    private static final Map<PrefixOp, UnaryExpressionTree.Operator> SUPPORTED_PREFIX_UNARY_OPERATORS = new EnumMap<>(PrefixOp.class);
    private static final Map<PostfixOp, UnaryExpressionTree.Operator> SUPPORTED_POSTFIX_UNARY_OPERATORS = new EnumMap<>(PostfixOp.class);
    private static final Map<BinaryOp, BinaryExpressionTree.Operator> SUPPORTED_BINARY_OPERATORS = new EnumMap<>(BinaryOp.class);
    private static final Map<BooleanOp, BinaryExpressionTree.Operator> BOOLEAN_OPERATORS = new EnumMap<>(BooleanOp.class);
    private static final Map<AssignmentOp, AssignmentExpressionTree.Operator> ASSIGNMENT_OPERATORS = new EnumMap<>(AssignmentOp.class);
    static {
      SUPPORTED_PREFIX_UNARY_OPERATORS.put(PrefixOp.NOT, UnaryExpressionTree.Operator.NEGATE);
      SUPPORTED_PREFIX_UNARY_OPERATORS.put(PrefixOp.POSITIVE, UnaryExpressionTree.Operator.PLUS);
      SUPPORTED_PREFIX_UNARY_OPERATORS.put(PrefixOp.NEGATIVE, UnaryExpressionTree.Operator.MINUS);

      SUPPORTED_POSTFIX_UNARY_OPERATORS.put(PostfixOp.INC, UnaryExpressionTree.Operator.INCREMENT);
      SUPPORTED_POSTFIX_UNARY_OPERATORS.put(PostfixOp.DEC, UnaryExpressionTree.Operator.DECREMENT);

      SUPPORTED_BINARY_OPERATORS.put(BinaryOp.ADDITION, BinaryExpressionTree.Operator.PLUS);
      SUPPORTED_BINARY_OPERATORS.put(BinaryOp.SUBTRACTION, BinaryExpressionTree.Operator.MINUS);
      SUPPORTED_BINARY_OPERATORS.put(BinaryOp.MULTIPLICATION, BinaryExpressionTree.Operator.TIMES);
      SUPPORTED_BINARY_OPERATORS.put(BinaryOp.DIVISION, BinaryExpressionTree.Operator.DIVIDED_BY);

      BOOLEAN_OPERATORS.put(BooleanOp.DOUBLE_EQUAL, BinaryExpressionTree.Operator.EQUAL_TO);
      BOOLEAN_OPERATORS.put(BooleanOp.NOT_EQUAL, BinaryExpressionTree.Operator.NOT_EQUAL_TO);
      BOOLEAN_OPERATORS.put(BooleanOp.ALT_NOT_EQUAL, BinaryExpressionTree.Operator.NOT_EQUAL_TO);
      BOOLEAN_OPERATORS.put(BooleanOp.GREATER_THAN, BinaryExpressionTree.Operator.GREATER_THAN);
      BOOLEAN_OPERATORS.put(BooleanOp.GREATER_THAN_EQUAL, BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO);
      BOOLEAN_OPERATORS.put(BooleanOp.LESS_THAN, BinaryExpressionTree.Operator.LESS_THAN);
      BOOLEAN_OPERATORS.put(BooleanOp.LESS_THAN_EQUAL, BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO);
      BOOLEAN_OPERATORS.put(BooleanOp.AND, BinaryExpressionTree.Operator.CONDITIONAL_AND);
      BOOLEAN_OPERATORS.put(BooleanOp.OR, BinaryExpressionTree.Operator.CONDITIONAL_OR);

      ASSIGNMENT_OPERATORS.put(AssignmentOp.EQUALS, AssignmentExpressionTree.Operator.EQUAL);
      ASSIGNMENT_OPERATORS.put(AssignmentOp.ADDITION_EQUALS, AssignmentExpressionTree.Operator.PLUS_EQUAL);
    }

    // Skip native ast node without added value: no location and only one child
    private static final Set<Class<?>> SKIPPED_USELESS_HIERARCHY_LEVEL = new HashSet<>(Arrays.asList(
      BlockMember.MethodMember.class,
      CompilationUnit.AnonymousBlockUnit.class));

    StronglyTypedConverter(Lexer.Tokens tokens) {
      super(tokens, SKIPPED_USELESS_HIERARCHY_LEVEL);
    }

    @Override
    protected void exit(ClassDecl node) {
      ConverterNode convNode = pop();
      NativeTreeImpl classTree = new NativeTreeImpl(metaData(convNode, node.loc), kind(node), convNode.children());
      IdentifierTree identifier = (IdentifierTree) convNode.children(Property.ClassDecl.NAME).get(0);
      add(new ClassDeclarationTreeImpl(metaData(convNode, node.loc), identifier, classTree));
    }

    @Override
    protected void exit(MethodDecl node) {
      ConverterNode convNode = pop();
      //MethodDecl has no location, it will be build from his children
      TreeMetaData metaData = metaData(convNode, null);

      List<Tree> modifiers = ensurePrivateModifier(convNode.children(Property.MethodDecl.MODIFIERS));
      boolean isConstructor = !node.type.isPresent();
      Tree returnType = isConstructor ? null : convNode.children(Property.MethodDecl.TYPE).get(0);
      IdentifierTree name = (IdentifierTree)convNode.children(Property.MethodDecl.NAME).get(0);
      List<Tree> parameters = convNode.children(Property.MethodDecl.PARAMETERS);
      BlockTree body = node.stmnt.map(stmnt -> getMethodBlockTree(convNode, node.modifiers)).orElse(null);
      add(new FunctionDeclarationTreeImpl(metaData, modifiers, isConstructor, returnType, name, parameters, body, Collections.emptyList()));
    }

    @Override
    protected void exit(Expr.MethodCallExpr node) {
      if (node.names.isEmpty()) {
        super.exit(node);
        return;
      }
      ConverterNode convNode = pop();
      // MethodCallExpr has no location, it will be build from his children. The parenthesis will therefore not be included
      TreeMetaData metaData = metaData(convNode, null);

      List<Tree> members = new ArrayList<>();

      if (node.dottedExpr.isPresent()) {
        members.add(convNode.children(Property.MethodCallExpr.DOTTEDEXPR).get(0));
      }

      members.addAll(convNode.children(Property.MethodCallExpr.NAMES));

      List<Tree> arguments = convNode.children(Property.MethodCallExpr.INPUTPARAMETERS);
      add(new FunctionInvocationTreeImpl(metaData, createMemberSelectTree(members), arguments));
    }

    @CheckForNull
    private static BlockTree getMethodBlockTree(ConverterNode convNode, @Nullable List<Modifier> modifiers) {
      BlockTree block = (BlockTree)convNode.children(Property.MethodDecl.STMNT).get(0);
      if (block.children().isEmpty() && modifiers != null && modifiers.stream().anyMatch(Modifier.VirtualModifier.class::isInstance)) {
        // empty virtual method are considered to have null body (similar as abstract methods) to avoid FP
        return null;
      }
      return block;
    }

    @Override
    protected void exit(ParameterRef node) {
      ConverterNode converterNode = pop();
      IdentifierTree identifier = (IdentifierTree) converterNode.children(Property.ParameterRef.GETNAME).get(0);
      Tree type = converterNode.children(Property.ParameterRef.GETTYPE).get(0);
      List<Tree> modifiers = converterNode.children(Property.ParameterRef.GETMODIFIERS);
      List<TextRange> includingTextRanges = new ArrayList<>();
      includingTextRanges.add(type.textRange());
      if(!modifiers.isEmpty()) {
        includingTextRanges.addAll(modifiers.stream().map(Tree::textRange).collect(Collectors.toList()));
      }
      TreeMetaData paramMetaData = newMetadataIncluding(identifier.metaData(), includingTextRanges);

      add(new ParameterTreeImpl(paramMetaData, identifier, type, null, modifiers));
    }

    @Override
    protected void exit(Stmnt.TryCatchFinallyBlock node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      Tree tryBlock = converterNode.children(Property.TryCatchFinallyBlock.TRYBLOCK).get(0);
      List<CatchTree> catchBlocks = converterNode.children(Property.TryCatchFinallyBlock.CATCHBLOCKS).stream()
          .filter(CatchTree.class::isInstance)
          .map(CatchTree.class::cast)
          .collect(Collectors.toList());

      Tree finallyBlock = null;
      if(node.finallyBlock.isPresent()){
        finallyBlock = converterNode.children(Property.TryCatchFinallyBlock.FINALLYBLOCK).get(0);
      }

      add(new ExceptionHandlingTreeImpl(metaData,
          tryBlock, keyword(metaData, "try"),
          catchBlocks,
          finallyBlock));
    }

    @Override
    protected void exit(CatchBlock node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);
      //Empty catch parameter is not possible in Apex
      Tree catchParameter = converterNode.children(Property.CatchBlock.PARAMETER).get(0);
      Tree catchBlock = converterNode.children(Property.CatchBlock.STMNT).get(0);

      add(new CatchTreeImpl(metaData, catchParameter, catchBlock, keyword(metaData, "catch")));
    }

    @Override
    protected void exit(Stmnt.ForLoop node) {
      if (!node.stmnt.isPresent()) {
        // For without body are mapped to native
        super.exit(node);
        return;
      }
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);
      Tree condition = converterNode.children(Property.ForLoop.FORCONTROL).get(0);
      Tree body = converterNode.children(Property.ForLoop.STMNT).get(0);

      add(new LoopTreeImpl(metaData,
          condition,body,
          LoopTree.LoopKind.FOR,
          keyword(metaData, "for")));
    }

    @Override
    protected void exit(Stmnt.DoLoop node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);
      Tree condition = converterNode.children(Property.DoLoop.CONDITION).get(0);
      Tree body = converterNode.children(Property.DoLoop.STMNT).get(0);

      add(new LoopTreeImpl(metaData,
          condition,body,
          LoopTree.LoopKind.DOWHILE,
          keyword(metaData, "do")));
    }

    @Override
    protected void exit(Stmnt.WhileLoop node) {
      if (!node.stmnt.isPresent()) {
        // While without body are mapped to native
        super.exit(node);
        return;
      }
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);
      Tree condition = converterNode.children(Property.WhileLoop.CONDITION).get(0);
      Tree body = converterNode.children(Property.WhileLoop.STMNT).get(0);

      add(new LoopTreeImpl(metaData,
          condition,body,
          LoopTree.LoopKind.WHILE,
          keyword(metaData, "while")));
    }

    @Override
    protected void exit(BlockStmnt node) {
      ConverterNode convNode = pop();
      List<Tree> stmnts = convNode.children(Property.BlockStmnt.STMNTS);
      add(new BlockTreeImpl(metaData(convNode, node.loc), stmnts));
    }

    @Override
    protected void exit(Stmnt.ReturnStmnt node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      Tree body = null;
      if(node.expr.isPresent()){
        body = converterNode.children(Property.ReturnStmnt.EXPR).get(0);
      }

      add(new ReturnTreeImpl(metaData, keyword(metaData, "return"), body));
    }

    @Override
    protected void exit(Stmnt.ThrowStmnt node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      //Body of throw can not be empty
      Tree body = converterNode.children(Property.ThrowStmnt.EXPR).get(0);

      add(new ThrowTreeImpl(metaData, keyword(metaData, "throw"), body));
    }

    @Override
    protected void exit(Stmnt.ContinueStmnt node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      //Continue can not take label
      add(new JumpTreeImpl(metaData, keyword(metaData, "continue"), JumpTree.JumpKind.CONTINUE, null));
    }

    @Override
    protected void exit(Stmnt.BreakStmnt node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      //Break can not take label
      add(new JumpTreeImpl(metaData, keyword(metaData, "break"), JumpTree.JumpKind.BREAK, null));
    }

    @Override
    protected void exit(Expr.NestedExpr node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);

      // parentheses are not provided by the parser - needs to be manually collected
      Token leftParenthesis = metaDataProvider()
        .previousToken(metaData.tokens().get(0).textRange())
        .filter(token -> "(".equals(token.text()))
        .orElseThrow(() -> new ParseException("Expecting NestedExpr to start with left parenthesis.", metaData.textRange().start()));

      Token rightParenthesis = findPairingRightParenthesis(leftParenthesis);
      Tree expression = converterNode.children(Property.NestedExpr.EXPR).get(0);
      TreeMetaData newMetaData = newMetadataIncluding(metaData, leftParenthesis.textRange(), rightParenthesis.textRange());
      add(new ParenthesizedExpressionTreeImpl(newMetaData, expression, leftParenthesis, rightParenthesis));
    }

    private Token findPairingRightParenthesis(Token leftParenthesis) {
      List<Token> tokens = metaDataProvider().allTokens();
      int numberOpenParenthesis = 0;
      Token currentToken;
      int currentIndex = metaDataProvider().indexOfFirstToken(leftParenthesis.textRange());
      String text;
      do {
        if (currentIndex >= tokens.size()) {
          // reached end of the file - should never happen
          throw new ParseException("Unable to find right parenthesis closing the expression.", leftParenthesis.textRange().start());
        }
        currentToken = tokens.get(currentIndex);
        text = currentToken.text();
        if ("(".equals(text)) {
          numberOpenParenthesis++;
        } else if (")".equals(text)) {
          numberOpenParenthesis--;
        }
        currentIndex++;
      } while (!(")".equals(text) && numberOpenParenthesis == 0));
      return currentToken;
    }

    @Override
    protected void exit(Identifier node) {
      ConverterNode convNode = pop();
      add(new ApexIdentifierTreeImpl(metaData(convNode, node.getLoc()), node.getValue()));
    }

    @Override
    protected void exit(Expr.PostfixExpr node) {
      UnaryExpressionTree.Operator operator = SUPPORTED_POSTFIX_UNARY_OPERATORS.get(node.op);
      if (operator == null) {
        super.exit(node);
        return;
      }

      addUnaryExpression(operator, node.loc, Property.PostfixExpr.EXPR);
    }

    @Override
    protected void exit(Expr.PrefixExpr node) {
      UnaryExpressionTree.Operator operator = SUPPORTED_PREFIX_UNARY_OPERATORS.get(node.op);
      if (operator == null) {
        super.exit(node);
        return;
      }
      addUnaryExpression(operator, node.loc, Property.PrefixExpr.EXPR);
    }

    @Override
    protected void exit(Expr.BinaryExpr node) {
      BinaryExpressionTree.Operator operator = SUPPORTED_BINARY_OPERATORS.get(node.op);
      if (operator == null) {
        super.exit(node);
        return;
      }
      addBinaryExpression(operator, Property.BinaryExpr.LEFT, Property.BinaryExpr.RIGHT);
    }

    @Override
    protected void exit(Expr.BooleanExpr node) {
      BinaryExpressionTree.Operator operator = BOOLEAN_OPERATORS.get(node.op);
      if(operator == null){
        super.exit(node);
        return;
      }
      addBinaryExpression(operator, Property.BooleanExpr.LEFT, Property.BooleanExpr.RIGHT);
    }

    @Override
    protected void exit(Expr.LiteralExpr node) {
      ConverterNode convNode = pop();
      String value = lineSet.substring(node.loc.getStartIndex(), node.loc.getEndIndex());
      TreeMetaData metaData = metaData(convNode, node.loc);
      int contentLength = value.length();

      LiteralTree tree;
      if (contentLength >= 2 && value.charAt(0) == '\'' && value.charAt(contentLength - 1) == '\'') {
        // String literal
        tree = new StringLiteralTreeImpl(metaData, value, value.substring(1, contentLength - 1));
      } else {
        tree = new LiteralTreeImpl(metaData, value);
      }
      add(tree);
    }

    @Override
    protected void exit(CompilationUnit.TriggerDeclUnit node) {
      ConverterNode converterNode = pop();
      List<Tree> members = converterNode.children(Property.TriggerDeclUnit.MEMBERS);
      TreeMetaData metaData;
      TextRange textRangeBeforeMember = topLevelRange;
      if (members.isEmpty()) {
        // when members is empty, the StmntBlockMember is missing and "node.loc" does not include the '{' and '}' tokens.
        metaData = metaDataProvider().metaData(topLevelRange);
      } else {
        metaData = metaData(converterNode, node.loc);
        textRangeBeforeMember = new TextRangeImpl(topLevelRange.start(), members.get(0).textRange().start());
      }
      updateTokensAsKeyword(textRangeBeforeMember, "after", "before");
      add(new NativeTreeImpl(metaData, kind(node), converterNode.children()));
    }

    @Override
    protected void exit(Expr.AssignmentExpr node) {
      AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATORS.get(node.op);
      if (operator == null) {
        super.exit(node);
      } else {
        ConverterNode converterNode = pop();
        TreeMetaData metaData = metaData(converterNode, null);
        Tree left = converterNode.children(Property.AssignmentExpr.LEFT).get(0);
        Tree right = converterNode.children(Property.AssignmentExpr.RIGHT).get(0);
        add(new AssignmentExpressionTreeImpl(metaData, operator, left, right));
      }
    }

    @Override
    protected void exit(SwitchStmnt node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);
      updateFirstTokenAsKeyword(metaData.textRange(), "switch", node);

      Tree expression = converterNode.children(Property.SwitchStmnt.EXPR).get(0);
      List<MatchCaseTree> cases = converterNode.children(Property.SwitchStmnt.WHENBLOCKS).stream()
        .filter(MatchCaseTree.class::isInstance)
        .map(MatchCaseTree.class::cast)
        .collect(Collectors.toList());

      add(new MatchTreeImpl(metaData, expression, cases, keyword(metaData, "switch")));
    }

    @Override
    protected void exit(WhenBlock.ElseWhen node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);
      metaData = includePreviousKeyword(metaData, "else", node);
      metaData = includePreviousKeyword(metaData, "when", node);

      Tree statement = converterNode.children(Property.ElseWhen.STMNT).get(0);

      add(new MatchCaseTreeImpl(metaData, null, statement));
    }

    @Override
    protected void exit(WhenBlock.TypeWhen node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);
      metaData = includePreviousKeyword(metaData, "when", node);

      Tree type = converterNode.children(Property.TypeWhen.TYPEREF).get(0);
      Tree variableName = converterNode.children(Property.TypeWhen.NAME).get(0);
      Tree expression = CaseExpressionTreeImpl.create(metaDataProvider(), Arrays.asList(type, variableName));
      Tree statement = converterNode.children(Property.TypeWhen.STMNT).get(0);

      add(new MatchCaseTreeImpl(metaData, expression, statement));
    }

    @Override
    protected void exit(WhenBlock.ValueWhen node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);
      metaData = includePreviousKeyword(metaData, "when", node);

      List<Tree> cases = converterNode.children(Property.ValueWhen.WHENCASES);
      Tree expression = CaseExpressionTreeImpl.create(metaDataProvider(), cases);
      Tree statement = converterNode.children(Property.ValueWhen.STMNT).get(0);

      add(new MatchCaseTreeImpl(metaData, expression, statement));
    }

    @Override
    protected void exit(Stmnt.IfElseBlock node) {
      ConverterNode converterNode = pop();
      boolean singleIf = converterNode.children().size() == 1;
      if (singleIf) {
        add(converterNode.children().get(0));
        return;
      }

      List<IfTreeImpl> reversedIfTrees = Lists.reverse(converterNode.children().stream()
        .filter(IfTreeImpl.class::isInstance)
        .map(IfTreeImpl.class::cast)
        .collect(Collectors.toList()));

      IfTreeImpl currentIfElse = getLastIfElse(reversedIfTrees, converterNode);
      int index = 1;
      while (index < reversedIfTrees.size()) {
        IfTreeImpl onlyIf = reversedIfTrees.get(index);
        TextRange textRange = rangeBetween(onlyIf, currentIfElse);
        currentIfElse = new IfTreeImpl(
          metaDataProvider().metaData(textRange),
          onlyIf.condition(),
          onlyIf.thenBranch(),
          // else branch
          currentIfElse,
          onlyIf.ifKeyword(),
          keyword(metaDataProvider().metaData(currentIfElse.textRange()), "else"));

        index++;
      }

      add(currentIfElse);
    }

    @Override
    protected void exit(Expr.TernaryExpr node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);
      Tree condition = converterNode.children(Property.TernaryExpr.CONDITION).get(0);
      Tree thenBranch = converterNode.children(Property.TernaryExpr.TRUEEXPR).get(0);
      Tree elseBranch = converterNode.children(Property.TernaryExpr.FALSEEXPR).get(0);
      Token ifKeyword = previousToken(thenBranch.textRange(), "?", node);
      Token elseKeyword = previousToken(elseBranch.textRange(), ":", node);
      add(new IfTreeImpl(metaData, condition, thenBranch, elseBranch, ifKeyword, elseKeyword));
    }

    @Override
    protected void exit(IfBlock node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      Tree condition = converterNode.children(Property.IfBlock.EXPR).get(0);
      Tree thenBranch = converterNode.children(Property.IfBlock.STMNT).get(0);

      add(new IfTreeImpl(metaData(converterNode, node.loc),
        condition,
        thenBranch,
        null,
        keyword(metaData, "if"),
        null));
    }

    @Override
    protected void exit(ElseBlock node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, node.loc);

      Tree elseBranch = converterNode.children(Property.ElseBlock.STMNT).get(0);

      add(new ElseTreeImpl(metaData(converterNode, node.loc), elseBranch, keyword(metaData, "else")));
    }

    @Override
    protected void exit(Modifier.PrivateModifier node) {
      handleModifier(node.loc, ModifierTree.Kind.PRIVATE);
    }

    @Override
    protected void exit(Modifier.PublicModifier node) {
      handleModifier(node.loc, ModifierTree.Kind.PUBLIC);
    }

    @Override
    protected void exit(Modifier.OverrideModifier node) {
      handleModifier(node.loc, ModifierTree.Kind.OVERRIDE);
    }

    private void updateFirstTokenAsKeyword(TextRange textRange, String expectedKeyword, Object nativeNode) {
      Token firstToken = metaDataProvider().firstToken(textRange)
        .filter(token -> token.text().equalsIgnoreCase(expectedKeyword))
        .orElseThrow(() -> new ParseException(nativeNode.getClass().getCanonicalName() + " without '" + expectedKeyword + "' keyword token", textRange.start()));
      metaDataProvider().updateTokenType(firstToken, Token.Type.KEYWORD);
    }

    private void updateTokensAsKeyword(TextRange textRange, String... keywords) {
      List<Token> tokens = metaDataProvider().metaData(textRange).tokens();
      for (String keyword : keywords) {
        tokens.stream()
          .filter(token -> token.text().equalsIgnoreCase(keyword))
          .forEach(token -> metaDataProvider().updateTokenType(token, Token.Type.KEYWORD));
      }
    }

    private TreeMetaData includePreviousKeyword(TreeMetaData metaData, String expectedKeyword, Object nativeNode) {
      Token previousToken = previousToken(metaData.textRange(), expectedKeyword, nativeNode);
      if (previousToken.type() != Token.Type.KEYWORD) {
        metaDataProvider().updateTokenType(previousToken, Token.Type.KEYWORD);
      }
      return newMetadataIncluding(metaData, previousToken.textRange());
    }

    private Token previousToken(TextRange textRange, String expectedKeyword, Object nativeNode) {
      return metaDataProvider().previousToken(textRange, token -> expectedKeyword.equalsIgnoreCase(token.text()))
        .orElseThrow(() -> new ParseException(
          String.format("'%s' without previous '%s' keyword token.", nativeNode.getClass().getCanonicalName(), expectedKeyword),
          textRange.start()));
    }

    TreeMetaData newMetadataIncluding(TreeMetaData metaData, TextRange... textRanges) {
      return newMetadataIncluding(metaData, Arrays.asList(textRanges));
    }

    TreeMetaData newMetadataIncluding(TreeMetaData metaData, List<TextRange> textRanges) {
      TextRange mergedMetaData = metaData.textRange();
      for (TextRange textRange : textRanges) {
        if (textRange.start().compareTo(mergedMetaData.start()) < 0) {
          mergedMetaData = new TextRangeImpl(textRange.start(), mergedMetaData.end());
        }
        if (textRange.end().compareTo(mergedMetaData.end()) > 0) {
          mergedMetaData = new TextRangeImpl(mergedMetaData.start(), textRange.end());
        }
      }
      return metaDataProvider().metaData(mergedMetaData);
    }

    private static List<Tree> ensurePrivateModifier(List<Tree> modifiers) {
      // we do not consider test methods to be private to avoid many FP
      if (modifiers.stream().anyMatch(StronglyTypedConverter::isTestModifier)) {
        List<Tree> newModifiers = new ArrayList<>(modifiers.size());
        newModifiers.addAll(modifiers);

        modifiers.stream()
          .filter(modifier -> modifier instanceof ModifierTree && ((ModifierTree) modifier).kind() == ModifierTree.Kind.PRIVATE)
          .findFirst()
          .ifPresent(privateModifier -> {
            newModifiers.remove(privateModifier);
            newModifiers.add(new NativeTreeImpl(privateModifier.metaData(), new ClassNativeKind(Modifier.PrivateModifier.class), Collections.emptyList()));
          });

        return newModifiers;
      }
      return modifiers;
    }

    private static boolean isTestModifier(Tree modifier) {
      if (!(modifier instanceof NativeTree)) {
        return false;
      }
      NativeTree nativeModifier = (NativeTree) modifier;
      if (nativeModifier.nativeKind().equals(ANNOTATION_KIND)
        && !modifier.children().isEmpty()
        && modifier.children().get(0) instanceof IdentifierTree) {
        String name = ((IdentifierTree) modifier.children().get(0)).name().toLowerCase(Locale.ENGLISH);
        return name.equals("istest") || name.equals("testvisible") || name.equals("testsetup");
      }
      return nativeModifier.nativeKind().equals(TEST_METHOD_MODIFIER_KIND);
    }

    private void handleModifier(Location loc, ModifierTree.Kind kind) {
      ConverterNode converterNode = pop();
      add(new ModifierTreeImpl(metaData(converterNode, loc), kind));
    }

    private void addUnaryExpression(UnaryExpressionTree.Operator operator, Location location, Property property) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, location);
      Tree operand = converterNode.children(property).get(0);

      UnaryExpressionTree unaryExpressionTree = new UnaryExpressionTreeImpl(metaData, operator, operand);
      add(unaryExpressionTree);
    }

    @Override
    protected void exit(VariableDecl node) {
      ConverterNode converterNode = pop();
      TreeMetaData metaData = metaData(converterNode, null);
      IdentifierTree identifier = (IdentifierTree) converterNode.children(Property.VariableDecl.NAME).get(0);
      Tree initializer = node.assignment.map(asgnmt -> converterNode.children(Property.VariableDecl.ASSIGNMENT).get(0)).orElse(null);
      add(new VariableDeclarationTreeImpl(metaData, identifier, null, initializer, false));
    }

    @Override
    protected void exit(Expr.VariableExpr node) {
      if (node.names.isEmpty()) {
        super.exit(node);
        return;
      }
      ConverterNode convNode = pop();
      List<Tree> members = new ArrayList<>();

      if (node.dottedExpr.isPresent()) {
        members.add(convNode.children(Property.VariableExpr.DOTTEDEXPR).get(0));
      }

      members.addAll(convNode.children(Property.VariableExpr.NAMES));

      add(createMemberSelectTree(members));
    }

    /**
     * Create a nested tree of MemberSelectTree from members, bottom-up.
     * The first element of members is therefore the expression of the leaf member select, and
     * the last element as the identifier of the root MemberSelect.
     */
    private Tree createMemberSelectTree(List<Tree> members) {
      Tree currentMemberSelect = members.get(0);

      for (int i = 1; i < members.size(); i++) {
        Tree currentName = members.get(i);
        currentMemberSelect = new MemberSelectTreeImpl(newMetadataIncluding(currentName.metaData(), currentMemberSelect.textRange()),
          currentMemberSelect, (IdentifierTree)currentName);
      }

      return currentMemberSelect;
    }

    @Override
    protected void exit(VariableDecls node) {
      ConverterNode converterNode = pop();
      Tree typeTree = converterNode.children(Property.VariableDecls.TYPE).get(0);
      List<Tree> children = converterNode.children();

      if (node.decls.size() == 1) {
        // When there is only a single declaration, we move the type inside the VariableDeclarationTree
        VariableDeclarationTree variableDecl = (VariableDeclarationTree) converterNode.children(Property.VariableDecls.DECLS).get(0);
        children = new ArrayList<>(children);
        children.remove(typeTree);
        children.remove(variableDecl);
        TreeMetaData metaData = newMetadataIncluding(variableDecl.metaData(), typeTree.textRange());
        children.add(new VariableDeclarationTreeImpl(metaData, variableDecl.identifier(), typeTree, variableDecl.initializer(), false));
      }

      add(new NativeTreeImpl(metaData(converterNode, null), kind(node), children));
    }

    private void addBinaryExpression(BinaryExpressionTree.Operator operator, Property left, Property right) {
      ConverterNode converterNode = pop();
      Tree leftTree = converterNode.children(left).get(0);
      Tree rightTree = converterNode.children(right).get(0);
      TreeMetaData operatorMetadata = metaDataProvider().metaData(new TextRangeImpl(leftTree.textRange().end(), rightTree.textRange().start()));
      Token operatorToken = operatorMetadata.tokens().get(0);
      BinaryExpressionTree binaryExpressionTree = new BinaryExpressionTreeImpl(
        metaDataProvider().metaData(rangeBetween(leftTree, rightTree)),
        operator,
        operatorToken,
        leftTree,
        rightTree);
      add(binaryExpressionTree);
    }

    private IfTreeImpl getLastIfElse(List<IfTreeImpl> reversedIfTrees, ConverterNode converterNode) {
      Optional<ElseTreeImpl> elseTree = converterNode.children().stream()
        .filter(ElseTreeImpl.class::isInstance)
        .map(ElseTreeImpl.class::cast)
        .findFirst();

      IfTreeImpl lastIfElse;
      IfTreeImpl lastIf = reversedIfTrees.get(0);
      if (elseTree.isPresent()) {
        ElseTreeImpl elseTreeImpl = elseTree.get();
        TextRange textRange = rangeBetween(lastIf, elseTreeImpl);
        lastIfElse = new IfTreeImpl(metaDataProvider().metaData(textRange),
          lastIf.condition(),
          lastIf.thenBranch(),
          elseTreeImpl.elseBranch,
          lastIf.ifKeyword(),
          elseTreeImpl.elseKeyword);
      } else {
        lastIfElse = lastIf;
      }

      return lastIfElse;
    }

    private static Token keyword(TreeMetaData metadata, String keywordText) {
      return metadata.tokens().stream()
        .filter(token -> Token.Type.KEYWORD.equals(token.type()))
        .filter(token -> keywordText.equalsIgnoreCase(token.text()))
        .findFirst()
        .orElseThrow(() -> new ParseException("Unable to find keyword '" + keywordText + "'", metadata.textRange().start()));
    }

    private static class ElseTreeImpl extends BaseTreeImpl {
      private final Tree elseBranch;
      private final Token elseKeyword;

      ElseTreeImpl(TreeMetaData metaData, Tree elseBranch, Token elseKeyword) {
        super(metaData);
        this.elseBranch = elseBranch;
        this.elseKeyword = elseKeyword;
      }

      @Override
      public List<Tree> children() {
        throw new UnsupportedOperationException("It is a private class, it should not call children().");
      }
    }

    private static class CaseExpressionTreeImpl extends BaseTreeImpl {
      private final List<Tree> children;

      private CaseExpressionTreeImpl(TreeMetaData metaData, List<Tree> children) {
        super(metaData);
        this.children = children;
      }

      private static CaseExpressionTreeImpl create(TreeMetaDataProvider treeMetaDataProvider, List<Tree> children) {
        TextRange textRange = new TextRangeImpl(children.get(0).textRange().start(), children.get(children.size() - 1).textRange().end());
        TreeMetaData metadata = treeMetaDataProvider.metaData(textRange);
        return new CaseExpressionTreeImpl(metadata, children);
      }

      @Override
      public List<Tree> children() {
        return children;
      }
    }
  }

}
