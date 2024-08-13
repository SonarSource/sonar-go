package org.sonarsource.apex.parser.poc.compiler;

import apex.jorje.data.Locatable;
import apex.jorje.semantic.ast.compilation.AnonymousClass;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.compilation.UserClassMethods;
import apex.jorje.semantic.ast.compilation.UserEnum;
import apex.jorje.semantic.ast.compilation.UserExceptionMethods;
import apex.jorje.semantic.ast.compilation.UserInterface;
import apex.jorje.semantic.ast.compilation.UserTrigger;
import apex.jorje.semantic.ast.condition.StandardCondition;
import apex.jorje.semantic.ast.expression.ArrayLoadExpression;
import apex.jorje.semantic.ast.expression.ArrayStoreExpression;
import apex.jorje.semantic.ast.expression.AssignmentExpression;
import apex.jorje.semantic.ast.expression.BinaryExpression;
import apex.jorje.semantic.ast.expression.BindExpressions;
import apex.jorje.semantic.ast.expression.BooleanExpression;
import apex.jorje.semantic.ast.expression.CastExpression;
import apex.jorje.semantic.ast.expression.ClassRefExpression;
import apex.jorje.semantic.ast.expression.EmptyReferenceExpression;
import apex.jorje.semantic.ast.expression.InstanceOfExpression;
import apex.jorje.semantic.ast.expression.JavaMethodCallExpression;
import apex.jorje.semantic.ast.expression.JavaVariableExpression;
import apex.jorje.semantic.ast.expression.LiteralExpression;
import apex.jorje.semantic.ast.expression.MapEntryNode;
import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.NewListInitExpression;
import apex.jorje.semantic.ast.expression.NewListLiteralExpression;
import apex.jorje.semantic.ast.expression.NewMapInitExpression;
import apex.jorje.semantic.ast.expression.NewMapLiteralExpression;
import apex.jorje.semantic.ast.expression.NewObjectExpression;
import apex.jorje.semantic.ast.expression.NewSetInitExpression;
import apex.jorje.semantic.ast.expression.NewSetLiteralExpression;
import apex.jorje.semantic.ast.expression.PackageVersionExpression;
import apex.jorje.semantic.ast.expression.PostfixExpression;
import apex.jorje.semantic.ast.expression.PrefixExpression;
import apex.jorje.semantic.ast.expression.ReferenceExpression;
import apex.jorje.semantic.ast.expression.SoqlExpression;
import apex.jorje.semantic.ast.expression.SoslExpression;
import apex.jorje.semantic.ast.expression.SuperMethodCallExpression;
import apex.jorje.semantic.ast.expression.SuperVariableExpression;
import apex.jorje.semantic.ast.expression.TernaryExpression;
import apex.jorje.semantic.ast.expression.ThisMethodCallExpression;
import apex.jorje.semantic.ast.expression.ThisVariableExpression;
import apex.jorje.semantic.ast.expression.TriggerVariableExpression;
import apex.jorje.semantic.ast.expression.VariableExpression;
import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.ast.member.Property;
import apex.jorje.semantic.ast.member.bridge.BridgeMethodCreator;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.ast.modifier.AnnotationParameter;
import apex.jorje.semantic.ast.modifier.ModifierGroup;
import apex.jorje.semantic.ast.modifier.ModifierNode;
import apex.jorje.semantic.ast.statement.BlockStatement;
import apex.jorje.semantic.ast.statement.BreakStatement;
import apex.jorje.semantic.ast.statement.CatchBlockStatement;
import apex.jorje.semantic.ast.statement.ContinueStatement;
import apex.jorje.semantic.ast.statement.DmlDeleteStatement;
import apex.jorje.semantic.ast.statement.DmlInsertStatement;
import apex.jorje.semantic.ast.statement.DmlMergeStatement;
import apex.jorje.semantic.ast.statement.DmlUndeleteStatement;
import apex.jorje.semantic.ast.statement.DmlUpdateStatement;
import apex.jorje.semantic.ast.statement.DmlUpsertStatement;
import apex.jorje.semantic.ast.statement.DoLoopStatement;
import apex.jorje.semantic.ast.statement.ElseWhenBlock;
import apex.jorje.semantic.ast.statement.ExpressionStatement;
import apex.jorje.semantic.ast.statement.FieldDeclaration;
import apex.jorje.semantic.ast.statement.FieldDeclarationStatements;
import apex.jorje.semantic.ast.statement.ForEachStatement;
import apex.jorje.semantic.ast.statement.ForLoopStatement;
import apex.jorje.semantic.ast.statement.IfBlockStatement;
import apex.jorje.semantic.ast.statement.IfElseBlockStatement;
import apex.jorje.semantic.ast.statement.ReturnStatement;
import apex.jorje.semantic.ast.statement.RunAsBlockStatement;
import apex.jorje.semantic.ast.statement.SwitchStatement;
import apex.jorje.semantic.ast.statement.ThrowStatement;
import apex.jorje.semantic.ast.statement.TryCatchFinallyBlockStatement;
import apex.jorje.semantic.ast.statement.TypeWhenBlock;
import apex.jorje.semantic.ast.statement.ValueWhenBlock;
import apex.jorje.semantic.ast.statement.VariableDeclaration;
import apex.jorje.semantic.ast.statement.VariableDeclarationStatements;
import apex.jorje.semantic.ast.statement.WhenCases;
import apex.jorje.semantic.ast.statement.WhileLoopStatement;
import apex.jorje.semantic.ast.visitor.AdditionalPassScope;
import apex.jorje.semantic.ast.visitor.AstVisitor;

public abstract class AstNodeVisitor extends AstVisitor<AdditionalPassScope> {

  protected abstract boolean enterAstNode(Locatable node, AdditionalPassScope scope);

  protected abstract void exitAstNode(Locatable node, AdditionalPassScope scope);

  @Override
  protected boolean defaultVisit() {
    return true;
  }

  @Override
  public boolean visit(AnonymousClass node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(AnonymousClass node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserClass node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserClass node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserEnum node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserEnum node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserInterface node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserInterface node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserTrigger node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserTrigger node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ArrayLoadExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ArrayLoadExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ArrayStoreExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ArrayStoreExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(AssignmentExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(AssignmentExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BinaryExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BinaryExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BooleanExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BooleanExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ClassRefExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ClassRefExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(CastExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(CastExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(InstanceOfExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(InstanceOfExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(JavaMethodCallExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(JavaMethodCallExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(JavaVariableExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(JavaVariableExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(LiteralExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(LiteralExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ReferenceExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ReferenceExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(EmptyReferenceExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(EmptyReferenceExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(MethodCallExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(MethodCallExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewListInitExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewListInitExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewMapInitExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewMapInitExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewSetInitExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewSetInitExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewListLiteralExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewListLiteralExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewSetLiteralExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewSetLiteralExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewMapLiteralExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewMapLiteralExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewObjectExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewObjectExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(NewKeyValueObjectExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(NewKeyValueObjectExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(PackageVersionExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(PackageVersionExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(PostfixExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(PostfixExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(PrefixExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(PrefixExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(TernaryExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(TernaryExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(StandardCondition node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(StandardCondition node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(TriggerVariableExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(TriggerVariableExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(VariableExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(VariableExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BreakStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BreakStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ContinueStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ContinueStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlDeleteStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlDeleteStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlInsertStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlInsertStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlMergeStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlMergeStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlUndeleteStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlUndeleteStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlUpdateStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlUpdateStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DmlUpsertStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DmlUpsertStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(DoLoopStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(DoLoopStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ExpressionStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ExpressionStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ForEachStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ForEachStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ForLoopStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ForLoopStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(FieldDeclaration node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(FieldDeclaration node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(FieldDeclarationStatements node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(FieldDeclarationStatements node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(IfBlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(IfBlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(IfElseBlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(IfElseBlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ReturnStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ReturnStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(RunAsBlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(RunAsBlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ThrowStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ThrowStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(VariableDeclaration node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(VariableDeclaration node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(VariableDeclarationStatements node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(VariableDeclarationStatements node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(WhileLoopStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(WhileLoopStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BindExpressions node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BindExpressions node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(SoqlExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(SoqlExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(SoslExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(SoslExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(MapEntryNode node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(MapEntryNode node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(CatchBlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(CatchBlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(TryCatchFinallyBlockStatement node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(TryCatchFinallyBlockStatement node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(Property node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(Property node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(Field node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(Field node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(Parameter node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(Parameter node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(Method node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(Method node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(BridgeMethodCreator node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(BridgeMethodCreator node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserClassMethods node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserClassMethods node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(UserExceptionMethods node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(UserExceptionMethods node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(Annotation node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(Annotation node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(AnnotationParameter node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(AnnotationParameter node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ModifierGroup node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ModifierGroup node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ModifierNode node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ModifierNode node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(SuperMethodCallExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(SuperMethodCallExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ThisMethodCallExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ThisMethodCallExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(SuperVariableExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(SuperVariableExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ThisVariableExpression node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ThisVariableExpression node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(SwitchStatement switchStatement, AdditionalPassScope scope) {
    return enterAstNode(switchStatement, scope);
  }

  @Override
  public void visitEnd(SwitchStatement switchStatement, AdditionalPassScope scope) {
    exitAstNode(switchStatement, scope);
  }

  @Override
  public boolean visit(ValueWhenBlock node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ValueWhenBlock node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(WhenCases.LiteralCase node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(WhenCases.LiteralCase node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(WhenCases.IdentifierCase node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(WhenCases.IdentifierCase node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(TypeWhenBlock node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(TypeWhenBlock node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }

  @Override
  public boolean visit(ElseWhenBlock node, AdditionalPassScope scope) {
    return enterAstNode(node, scope);
  }

  @Override
  public void visitEnd(ElseWhenBlock node, AdditionalPassScope scope) {
    exitAstNode(node, scope);
  }
}
