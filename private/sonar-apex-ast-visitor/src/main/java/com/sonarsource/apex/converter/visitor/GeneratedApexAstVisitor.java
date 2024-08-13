/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor;

import apex.jorje.data.Location;
import javax.annotation.Nullable;

import static java.util.Optional.ofNullable;

@javax.annotation.Generated("com.sonarsource.apex.converter.visitor.GeneratedApexAstVisitorTest.main")
public class GeneratedApexAstVisitor implements
  apex.jorje.data.ast.AnnotationParameter.SwitchBlock,
  apex.jorje.data.ast.AnnotationValue.SwitchBlock,
  apex.jorje.data.ast.BlockMember.SwitchBlock,
  apex.jorje.data.ast.CompilationUnit.SwitchBlock,
  apex.jorje.data.ast.Expr.SwitchBlock,
  apex.jorje.data.ast.ForControl.SwitchBlock,
  apex.jorje.data.ast.Modifier.SwitchBlock,
  apex.jorje.data.ast.NewObject.SwitchBlock,
  apex.jorje.data.ast.Stmnt.SwitchBlock,
  apex.jorje.data.ast.TypeRef.Visitor<Void>,
  apex.jorje.data.ast.WhenBlock.SwitchBlock,
  apex.jorje.data.ast.WhenCase.SwitchBlock,
  apex.jorje.data.soql.DataCategoryOperator.SwitchBlock,
  apex.jorje.data.soql.Geolocation.SwitchBlock,
  apex.jorje.data.soql.GroupByType.SwitchBlock,
  apex.jorje.data.soql.LimitClause.SwitchBlock,
  apex.jorje.data.soql.OffsetClause.SwitchBlock,
  apex.jorje.data.soql.Order.SwitchBlock,
  apex.jorje.data.soql.OrderByExpr.SwitchBlock,
  apex.jorje.data.soql.OrderNull.SwitchBlock,
  apex.jorje.data.soql.QueryExpr.SwitchBlock,
  apex.jorje.data.soql.QueryLiteral.SwitchBlock,
  apex.jorje.data.soql.QueryOp.SwitchBlock,
  apex.jorje.data.soql.QueryOption.SwitchBlock,
  apex.jorje.data.soql.SelectClause.SwitchBlock,
  apex.jorje.data.soql.SelectExpr.SwitchBlock,
  apex.jorje.data.soql.TrackingType.SwitchBlock,
  apex.jorje.data.soql.UpdateStatsOption.SwitchBlock,
  apex.jorje.data.soql.UsingExpr.SwitchBlock,
  apex.jorje.data.soql.WhereCalcOp.SwitchBlock,
  apex.jorje.data.soql.WhereCompoundOp.SwitchBlock,
  apex.jorje.data.soql.WhereExpr.SwitchBlock,
  apex.jorje.data.soql.WithClause.SwitchBlock,
  apex.jorje.data.soql.WithIdentifierClause.SwitchBlock,
  apex.jorje.data.soql.WithKeyValue.SwitchBlock,
  apex.jorje.data.sosl.DivisionValue.SwitchBlock,
  apex.jorje.data.sosl.FindValue.SwitchBlock {

  protected void defaultEnter(Object node, @Nullable Location location) {
    // can be overridden
  }

  protected void defaultExit(Object node, @Nullable Location location) {
    // can be overridden
  }

  @Nullable
  protected static Location valid(@Nullable Location location) {
    // Some AST node have a non null location equals to Locations.NONE like apex.jorje.data.soql.Order.OrderAsc
    if (location == null || (location.getStartIndex() == 0 && location.getEndIndex() == 0)) {
      return null;
    }
    return location;
  }

  protected void prepareChildVisit(Property property) {
    // can be overridden
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.InvalidDeclUnit node) {
    // ignore AST leaf without location
  }

  public void _case(apex.jorje.data.sosl.FindValue.FindString node) {
    // ignore AST leaf without location
  }

  public void _case(apex.jorje.data.soql.Geolocation.GeolocationLiteral node) {
    // ignore AST leaf without location
  }

  public void _case(apex.jorje.data.Identifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.Identifier node) {
    defaultEnter(node, valid(node.getLoc()));
  }

  protected void exit(apex.jorje.data.Identifier node) {
    defaultExit(node, valid(node.getLoc()));
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue node) {
    prepareChildVisit(Property.AnnotationKeyValue.KEY);
    ofNullable(node.key).ifPresent(this::_case);
    prepareChildVisit(Property.AnnotationKeyValue.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter.AnnotationString node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.AnnotationParameter.AnnotationString node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.AnnotationParameter.AnnotationString node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.AnnotationValue node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.FalseAnnotationValue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.AnnotationValue.FalseAnnotationValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.AnnotationValue.FalseAnnotationValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.StringAnnotationValue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.AnnotationValue.StringAnnotationValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.AnnotationValue.StringAnnotationValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.TrueAnnotationValue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.AnnotationValue.TrueAnnotationValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.AnnotationValue.TrueAnnotationValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.BlockMember node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.BlockMember.FieldMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.FieldMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.FieldMember node) {
    prepareChildVisit(Property.FieldMember.VARIABLEDECLS);
    ofNullable(node.variableDecls).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.FieldMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerClassMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.InnerClassMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.InnerClassMember node) {
    prepareChildVisit(Property.InnerClassMember.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.InnerClassMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerEnumMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.InnerEnumMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.InnerEnumMember node) {
    prepareChildVisit(Property.InnerEnumMember.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.InnerEnumMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerInterfaceMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.InnerInterfaceMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.InnerInterfaceMember node) {
    prepareChildVisit(Property.InnerInterfaceMember.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.InnerInterfaceMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.MethodMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.MethodMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.MethodMember node) {
    prepareChildVisit(Property.MethodMember.METHODDECL);
    ofNullable(node.methodDecl).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.MethodMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.PropertyMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.PropertyMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.PropertyMember node) {
    prepareChildVisit(Property.PropertyMember.PROPERTYDECL);
    ofNullable(node.propertyDecl).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.PropertyMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.BlockMember.StaticStmntBlockMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.StaticStmntBlockMember node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.StaticStmntBlockMember node) {
    prepareChildVisit(Property.StaticStmntBlockMember.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.StaticStmntBlockMember node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.BlockMember.StmntBlockMember node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.BlockMember.StmntBlockMember node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.BlockMember.StmntBlockMember node) {
    prepareChildVisit(Property.StmntBlockMember.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.BlockMember.StmntBlockMember node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.CatchBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CatchBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.CatchBlock node) {
    prepareChildVisit(Property.CatchBlock.PARAMETER);
    ofNullable(node.parameter).ifPresent(this::_case);
    prepareChildVisit(Property.CatchBlock.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.CatchBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.ClassDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ClassDecl node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.ClassDecl node) {
    prepareChildVisit(Property.ClassDecl.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ClassDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.ClassDecl.TYPEARGUMENTS);
    node.typeArguments.ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ClassDecl.MEMBERS);
    ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ClassDecl.SUPERCLASS);
    node.superClass.ifPresent(this::_case);
    prepareChildVisit(Property.ClassDecl.INTERFACES);
    ofNullable(node.interfaces).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.ClassDecl node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.CompilationUnit node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit node) {
    prepareChildVisit(Property.AnonymousBlockUnit.MEMBERS);
    ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.ClassDeclUnit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CompilationUnit.ClassDeclUnit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.CompilationUnit.ClassDeclUnit node) {
    prepareChildVisit(Property.ClassDeclUnit.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.CompilationUnit.ClassDeclUnit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.EnumDeclUnit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CompilationUnit.EnumDeclUnit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.CompilationUnit.EnumDeclUnit node) {
    prepareChildVisit(Property.EnumDeclUnit.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.CompilationUnit.EnumDeclUnit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit node) {
    prepareChildVisit(Property.InterfaceDeclUnit.BODY);
    ofNullable(node.body).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit node) {
    prepareChildVisit(Property.TriggerDeclUnit.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.TriggerDeclUnit.TARGET);
    ofNullable(node.target).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.TriggerDeclUnit.MEMBERS);
    ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.ElseBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ElseBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.ElseBlock node) {
    prepareChildVisit(Property.ElseBlock.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.ElseBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.EnumDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.EnumDecl node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.EnumDecl node) {
    prepareChildVisit(Property.EnumDecl.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.EnumDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.EnumDecl.MEMBERS);
    ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.EnumDecl node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Expr.ArrayExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.ArrayExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.ArrayExpr node) {
    prepareChildVisit(Property.ArrayExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.ArrayExpr.INDEX);
    ofNullable(node.index).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.ArrayExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.AssignmentExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.AssignmentExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.AssignmentExpr node) {
    prepareChildVisit(Property.AssignmentExpr.LEFT);
    ofNullable(node.left).ifPresent(this::_case);
    prepareChildVisit(Property.AssignmentExpr.RIGHT);
    ofNullable(node.right).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.AssignmentExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.BinaryExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.BinaryExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.BinaryExpr node) {
    prepareChildVisit(Property.BinaryExpr.LEFT);
    ofNullable(node.left).ifPresent(this::_case);
    prepareChildVisit(Property.BinaryExpr.RIGHT);
    ofNullable(node.right).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.BinaryExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.BooleanExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.BooleanExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.BooleanExpr node) {
    prepareChildVisit(Property.BooleanExpr.LEFT);
    ofNullable(node.left).ifPresent(this::_case);
    prepareChildVisit(Property.BooleanExpr.RIGHT);
    ofNullable(node.right).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.BooleanExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.CastExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.CastExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.CastExpr node) {
    prepareChildVisit(Property.CastExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.CastExpr.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.CastExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.ClassRefExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.ClassRefExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.ClassRefExpr node) {
    prepareChildVisit(Property.ClassRefExpr.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.ClassRefExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.InstanceOf node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.InstanceOf node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.InstanceOf node) {
    prepareChildVisit(Property.InstanceOf.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.InstanceOf.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.InstanceOf node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.JavaMethodCallExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.JavaMethodCallExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.JavaMethodCallExpr node) {
    prepareChildVisit(Property.JavaMethodCallExpr.NAMES);
    ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.JavaMethodCallExpr.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.JavaMethodCallExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.JavaVariableExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.JavaVariableExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.JavaVariableExpr node) {
    prepareChildVisit(Property.JavaVariableExpr.NAMES);
    ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.JavaVariableExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.LiteralExpr node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.LiteralExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Expr.LiteralExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.MethodCallExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.MethodCallExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.MethodCallExpr node) {
    prepareChildVisit(Property.MethodCallExpr.DOTTEDEXPR);
    node.dottedExpr.ifPresent(this::_case);
    prepareChildVisit(Property.MethodCallExpr.NAMES);
    ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.MethodCallExpr.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.MethodCallExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.NestedExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.NestedExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.NestedExpr node) {
    prepareChildVisit(Property.NestedExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.NestedExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.NewExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.NewExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.NewExpr node) {
    prepareChildVisit(Property.NewExpr.CREATOR);
    ofNullable(node.creator).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.NewExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.PackageVersionExpr node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.PackageVersionExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Expr.PackageVersionExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.PostfixExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.PostfixExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.PostfixExpr node) {
    prepareChildVisit(Property.PostfixExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.PostfixExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.PrefixExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.PrefixExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.PrefixExpr node) {
    prepareChildVisit(Property.PrefixExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.PrefixExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.SoqlExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.SoqlExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.SoqlExpr node) {
    prepareChildVisit(Property.SoqlExpr.QUERY);
    ofNullable(node.query).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.SoqlExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.SoslExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.SoslExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.SoslExpr node) {
    prepareChildVisit(Property.SoslExpr.SEARCH);
    ofNullable(node.search).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.SoslExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.SuperMethodCallExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.SuperMethodCallExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.SuperMethodCallExpr node) {
    prepareChildVisit(Property.SuperMethodCallExpr.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.SuperMethodCallExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.SuperVariableExpr node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.SuperVariableExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Expr.SuperVariableExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.TernaryExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.TernaryExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.TernaryExpr node) {
    prepareChildVisit(Property.TernaryExpr.CONDITION);
    ofNullable(node.condition).ifPresent(this::_case);
    prepareChildVisit(Property.TernaryExpr.TRUEEXPR);
    ofNullable(node.trueExpr).ifPresent(this::_case);
    prepareChildVisit(Property.TernaryExpr.FALSEEXPR);
    ofNullable(node.falseExpr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.TernaryExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Expr.ThisMethodCallExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.ThisMethodCallExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.ThisMethodCallExpr node) {
    prepareChildVisit(Property.ThisMethodCallExpr.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.ThisMethodCallExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.ThisVariableExpr node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.ThisVariableExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Expr.ThisVariableExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.TriggerVariableExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.TriggerVariableExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.TriggerVariableExpr node) {
    prepareChildVisit(Property.TriggerVariableExpr.VARIABLE);
    ofNullable(node.variable).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Expr.TriggerVariableExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Expr.VariableExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Expr.VariableExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Expr.VariableExpr node) {
    prepareChildVisit(Property.VariableExpr.DOTTEDEXPR);
    node.dottedExpr.ifPresent(this::_case);
    prepareChildVisit(Property.VariableExpr.NAMES);
    ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Expr.VariableExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.FinallyBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.FinallyBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.FinallyBlock node) {
    prepareChildVisit(Property.FinallyBlock.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.FinallyBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.ForControl node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.ForControl.CStyleForControl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ForControl.CStyleForControl node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.ForControl.CStyleForControl node) {
    prepareChildVisit(Property.CStyleForControl.INITS);
    node.inits.ifPresent(this::_case);
    prepareChildVisit(Property.CStyleForControl.CONDITION);
    node.condition.ifPresent(this::_case);
    prepareChildVisit(Property.CStyleForControl.CONTROL);
    node.control.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.ForControl.CStyleForControl node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.ForControl.EnhancedForControl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ForControl.EnhancedForControl node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.ForControl.EnhancedForControl node) {
    prepareChildVisit(Property.EnhancedForControl.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.EnhancedForControl.INIT);
    ofNullable(node.init).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.ForControl.EnhancedForControl node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.ForInit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ForInit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.ForInit node) {
    prepareChildVisit(Property.ForInit.NAME);
    ofNullable(node.name).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ForInit.EXPR);
    node.expr.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.ForInit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.ForInits node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ForInits node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.ForInits node) {
    prepareChildVisit(Property.ForInits.TYPE);
    node.type.ifPresent(this::_case);
    prepareChildVisit(Property.ForInits.INITS);
    ofNullable(node.inits).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.ForInits node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.IfBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.IfBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.IfBlock node) {
    prepareChildVisit(Property.IfBlock.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.IfBlock.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.IfBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.InterfaceDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.InterfaceDecl node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.InterfaceDecl node) {
    prepareChildVisit(Property.InterfaceDecl.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.InterfaceDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.InterfaceDecl.TYPEARGUMENTS);
    node.typeArguments.ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.InterfaceDecl.MEMBERS);
    ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.InterfaceDecl.SUPERINTERFACE);
    node.superInterface.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.InterfaceDecl node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.MapLiteralKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.MapLiteralKeyValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.MapLiteralKeyValue node) {
    prepareChildVisit(Property.MapLiteralKeyValue.KEY);
    ofNullable(node.key).ifPresent(this::_case);
    prepareChildVisit(Property.MapLiteralKeyValue.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.MapLiteralKeyValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.MethodDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.MethodDecl node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.MethodDecl node) {
    prepareChildVisit(Property.MethodDecl.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.MethodDecl.TYPE);
    node.type.ifPresent(this::_case);
    prepareChildVisit(Property.MethodDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.MethodDecl.PARAMETERS);
    ofNullable(node.parameters).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.MethodDecl.STMNT);
    node.stmnt.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.MethodDecl node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Modifier node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Modifier.AbstractModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.AbstractModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.AbstractModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.Annotation node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.Annotation node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Modifier.Annotation node) {
    prepareChildVisit(Property.Annotation.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.Annotation.PARAMETERS);
    ofNullable(node.parameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Modifier.Annotation node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.FinalModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.FinalModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.FinalModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.GlobalModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.GlobalModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.GlobalModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.InheritedSharingModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.InheritedSharingModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.InheritedSharingModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.OverrideModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.OverrideModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.OverrideModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.PrivateModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.PrivateModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.PrivateModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.ProtectedModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.ProtectedModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.ProtectedModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.PublicModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.PublicModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.PublicModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.StaticModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.StaticModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.StaticModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.TestMethodModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.TestMethodModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.TestMethodModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.TransientModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.TransientModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.TransientModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.VirtualModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.VirtualModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.VirtualModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.WebServiceModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.WebServiceModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.WebServiceModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.WithSharingModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.WithSharingModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.WithSharingModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Modifier.WithoutSharingModifier node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Modifier.WithoutSharingModifier node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Modifier.WithoutSharingModifier node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.NameValueParameter node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NameValueParameter node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NameValueParameter node) {
    prepareChildVisit(Property.NameValueParameter.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.NameValueParameter.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.NameValueParameter node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewKeyValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewKeyValue node) {
    prepareChildVisit(Property.NewKeyValue.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.NewKeyValue.KEYVALUES);
    ofNullable(node.keyValues).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewKeyValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewListInit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewListInit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewListInit node) {
    prepareChildVisit(Property.NewListInit.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewListInit.EXPR);
    node.expr.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewListInit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewListLiteral node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewListLiteral node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewListLiteral node) {
    prepareChildVisit(Property.NewListLiteral.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewListLiteral.VALUES);
    ofNullable(node.values).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewListLiteral node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewMapInit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewMapInit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewMapInit node) {
    prepareChildVisit(Property.NewMapInit.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewMapInit.EXPR);
    node.expr.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewMapInit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewMapLiteral node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewMapLiteral node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewMapLiteral node) {
    prepareChildVisit(Property.NewMapLiteral.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewMapLiteral.PAIRS);
    ofNullable(node.pairs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewMapLiteral node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewSetInit node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewSetInit node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewSetInit node) {
    prepareChildVisit(Property.NewSetInit.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewSetInit.EXPR);
    node.expr.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewSetInit node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewSetLiteral node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewSetLiteral node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewSetLiteral node) {
    prepareChildVisit(Property.NewSetLiteral.TYPES);
    ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.NewSetLiteral.VALUES);
    ofNullable(node.values).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewSetLiteral node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewStandard node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.NewObject.NewStandard node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.NewObject.NewStandard node) {
    prepareChildVisit(Property.NewStandard.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.NewStandard.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.NewObject.NewStandard node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.ParameterRef node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.ParameterRef node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.ParameterRef node) {
    prepareChildVisit(Property.ParameterRef.GETMODIFIERS);
    ofNullable(node.getModifiers()).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ParameterRef.GETNAME);
    ofNullable(node.getName()).ifPresent(this::_case);
    prepareChildVisit(Property.ParameterRef.GETTYPE);
    ofNullable(node.getType()).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.ParameterRef node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.PropertyDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.PropertyDecl node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.PropertyDecl node) {
    prepareChildVisit(Property.PropertyDecl.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.PropertyDecl.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.PropertyDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.PropertyDecl.GETTER);
    node.getter.ifPresent(this::_case);
    prepareChildVisit(Property.PropertyDecl.SETTER);
    node.setter.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.PropertyDecl node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.PropertyGetter node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.PropertyGetter node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.PropertyGetter node) {
    prepareChildVisit(Property.PropertyGetter.MODIFIER);
    node.modifier.ifPresent(this::_case);
    prepareChildVisit(Property.PropertyGetter.STMNT);
    node.stmnt.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.PropertyGetter node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.PropertySetter node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.PropertySetter node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.PropertySetter node) {
    prepareChildVisit(Property.PropertySetter.MODIFIER);
    node.modifier.ifPresent(this::_case);
    prepareChildVisit(Property.PropertySetter.STMNT);
    node.stmnt.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.PropertySetter node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Stmnt.BlockStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.BlockStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.BlockStmnt node) {
    prepareChildVisit(Property.BlockStmnt.STMNTS);
    ofNullable(node.stmnts).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Stmnt.BlockStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.BreakStmnt node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.BreakStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Stmnt.BreakStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.ContinueStmnt node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.ContinueStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.ast.Stmnt.ContinueStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlDeleteStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlDeleteStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlDeleteStmnt node) {
    prepareChildVisit(Property.DmlDeleteStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.DmlDeleteStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlDeleteStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlInsertStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlInsertStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlInsertStmnt node) {
    prepareChildVisit(Property.DmlInsertStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.DmlInsertStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlInsertStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlMergeStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlMergeStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlMergeStmnt node) {
    prepareChildVisit(Property.DmlMergeStmnt.EXPR1);
    ofNullable(node.expr1).ifPresent(this::_case);
    prepareChildVisit(Property.DmlMergeStmnt.EXPR2);
    ofNullable(node.expr2).ifPresent(this::_case);
    prepareChildVisit(Property.DmlMergeStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlMergeStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt node) {
    prepareChildVisit(Property.DmlUndeleteStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.DmlUndeleteStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUpdateStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlUpdateStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlUpdateStmnt node) {
    prepareChildVisit(Property.DmlUpdateStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.DmlUpdateStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlUpdateStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUpsertStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DmlUpsertStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DmlUpsertStmnt node) {
    prepareChildVisit(Property.DmlUpsertStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.DmlUpsertStmnt.ID);
    node.id.ifPresent(this::_case);
    prepareChildVisit(Property.DmlUpsertStmnt.RUNASMODE);
    node.runAsMode.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DmlUpsertStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.DoLoop node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.DoLoop node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.DoLoop node) {
    prepareChildVisit(Property.DoLoop.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
    prepareChildVisit(Property.DoLoop.CONDITION);
    ofNullable(node.condition).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.DoLoop node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.ExpressionStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.ExpressionStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.ExpressionStmnt node) {
    prepareChildVisit(Property.ExpressionStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.ExpressionStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.ForLoop node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.ForLoop node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.ForLoop node) {
    prepareChildVisit(Property.ForLoop.FORCONTROL);
    ofNullable(node.forControl).ifPresent(this::_case);
    prepareChildVisit(Property.ForLoop.STMNT);
    node.stmnt.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.ForLoop node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.IfElseBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.IfElseBlock node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.IfElseBlock node) {
    prepareChildVisit(Property.IfElseBlock.IFBLOCKS);
    ofNullable(node.ifBlocks).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.IfElseBlock.ELSEBLOCK);
    node.elseBlock.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.IfElseBlock node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Stmnt.ReturnStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.ReturnStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.ReturnStmnt node) {
    prepareChildVisit(Property.ReturnStmnt.EXPR);
    node.expr.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.ReturnStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.RunAsBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.RunAsBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.RunAsBlock node) {
    prepareChildVisit(Property.RunAsBlock.INPUTPARAMETERS);
    ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.RunAsBlock.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.RunAsBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.SwitchStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.SwitchStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.SwitchStmnt node) {
    prepareChildVisit(Property.SwitchStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.SwitchStmnt.WHENBLOCKS);
    ofNullable(node.whenBlocks).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.Stmnt.SwitchStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.ThrowStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.ThrowStmnt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.ThrowStmnt node) {
    prepareChildVisit(Property.ThrowStmnt.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.ThrowStmnt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock node) {
    prepareChildVisit(Property.TryCatchFinallyBlock.TRYBLOCK);
    ofNullable(node.tryBlock).ifPresent(this::_case);
    prepareChildVisit(Property.TryCatchFinallyBlock.CATCHBLOCKS);
    ofNullable(node.catchBlocks).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.TryCatchFinallyBlock.FINALLYBLOCK);
    node.finallyBlock.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.Stmnt.VariableDeclStmnt node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.VariableDeclStmnt node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.VariableDeclStmnt node) {
    prepareChildVisit(Property.VariableDeclStmnt.VARIABLEDECLS);
    ofNullable(node.variableDecls).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.VariableDeclStmnt node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.Stmnt.WhileLoop node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.Stmnt.WhileLoop node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.ast.Stmnt.WhileLoop node) {
    prepareChildVisit(Property.WhileLoop.CONDITION);
    ofNullable(node.condition).ifPresent(this::_case);
    prepareChildVisit(Property.WhileLoop.STMNT);
    node.stmnt.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.Stmnt.WhileLoop node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.ast.TypeRef node) {
    node.accept(this);
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.ArrayTypeRef node) {
    enter(node);
    visitChildren(node);
    exit(node);
    return null;
  }

  protected void enter(apex.jorje.data.ast.TypeRefs.ArrayTypeRef node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.TypeRefs.ArrayTypeRef node) {
    prepareChildVisit(Property.ArrayTypeRef.GETHELDTYPE);
    ofNullable(node.getHeldType()).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.TypeRefs.ArrayTypeRef node) {
    defaultExit(node, null);
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.ClassTypeRef node) {
    enter(node);
    visitChildren(node);
    exit(node);
    return null;
  }

  protected void enter(apex.jorje.data.ast.TypeRefs.ClassTypeRef node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.TypeRefs.ClassTypeRef node) {
    prepareChildVisit(Property.ClassTypeRef.GETTYPEARGUMENTS);
    ofNullable(node.getTypeArguments()).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ClassTypeRef.GETNAMES);
    ofNullable(node.getNames()).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.TypeRefs.ClassTypeRef node) {
    defaultExit(node, null);
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.JavaTypeRef node) {
    enter(node);
    visitChildren(node);
    exit(node);
    return null;
  }

  protected void enter(apex.jorje.data.ast.TypeRefs.JavaTypeRef node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.TypeRefs.JavaTypeRef node) {
    prepareChildVisit(Property.JavaTypeRef.GETTYPEARGUMENTS);
    ofNullable(node.getTypeArguments()).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.JavaTypeRef.GETNAMES);
    ofNullable(node.getNames()).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.TypeRefs.JavaTypeRef node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.VariableDecl node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.VariableDecl node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.VariableDecl node) {
    prepareChildVisit(Property.VariableDecl.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.VariableDecl.ASSIGNMENT);
    node.assignment.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.VariableDecl node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.VariableDecls node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.VariableDecls node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.VariableDecls node) {
    prepareChildVisit(Property.VariableDecls.MODIFIERS);
    ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.VariableDecls.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.VariableDecls.DECLS);
    ofNullable(node.decls).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.VariableDecls node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.WhenBlock node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.WhenBlock.ElseWhen node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.WhenBlock.ElseWhen node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.WhenBlock.ElseWhen node) {
    prepareChildVisit(Property.ElseWhen.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.WhenBlock.ElseWhen node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.WhenBlock.TypeWhen node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.WhenBlock.TypeWhen node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.WhenBlock.TypeWhen node) {
    prepareChildVisit(Property.TypeWhen.TYPEREF);
    ofNullable(node.typeRef).ifPresent(this::_case);
    prepareChildVisit(Property.TypeWhen.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.TypeWhen.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.WhenBlock.TypeWhen node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.WhenBlock.ValueWhen node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.WhenBlock.ValueWhen node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.WhenBlock.ValueWhen node) {
    prepareChildVisit(Property.ValueWhen.WHENCASES);
    ofNullable(node.whenCases).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ValueWhen.STMNT);
    ofNullable(node.stmnt).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.WhenBlock.ValueWhen node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.WhenCase node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.WhenCase.EnumCase node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.WhenCase.EnumCase node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.WhenCase.EnumCase node) {
    prepareChildVisit(Property.EnumCase.IDENTIFIERS);
    ofNullable(node.identifiers).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.ast.WhenCase.EnumCase node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.ast.WhenCase.LiteralCase node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.ast.WhenCase.LiteralCase node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.ast.WhenCase.LiteralCase node) {
    prepareChildVisit(Property.LiteralCase.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.ast.WhenCase.LiteralCase node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.BindClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.BindClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.BindClause node) {
    prepareChildVisit(Property.BindClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.BindClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.BindExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.BindExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.BindExpr node) {
    prepareChildVisit(Property.BindExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.BindExpr.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.BindExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.CaseExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.CaseExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.CaseExpr node) {
    prepareChildVisit(Property.CaseExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.CaseExpr.WHENBRANCHES);
    ofNullable(node.whenBranches).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.CaseExpr.ELSEBRANCH);
    node.elseBranch.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.CaseExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.CaseOp node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.CaseOp node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.CaseOp node) {
    prepareChildVisit(Property.CaseOp.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.CaseOp node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.ColonExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.ColonExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.ColonExpr node) {
    prepareChildVisit(Property.ColonExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.ColonExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.DataCategory node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DataCategory node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.DataCategory node) {
    prepareChildVisit(Property.DataCategory.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
    prepareChildVisit(Property.DataCategory.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.DataCategory.CATEGORIES);
    ofNullable(node.categories).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.DataCategory node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.DataCategoryOperator node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAbove node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAbove node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAbove node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAboveOrBelow node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAboveOrBelow node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAboveOrBelow node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAt node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAt node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.DataCategoryOperator.DataCategoryAt node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.DataCategoryOperator.DataCategoryBelow node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DataCategoryOperator.DataCategoryBelow node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.DataCategoryOperator.DataCategoryBelow node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.DistanceFunctionExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.DistanceFunctionExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.DistanceFunctionExpr node) {
    prepareChildVisit(Property.DistanceFunctionExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.DistanceFunctionExpr.LOCATION);
    ofNullable(node.location).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.DistanceFunctionExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.ElseExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.ElseExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.ElseExpr node) {
    prepareChildVisit(Property.ElseExpr.IDENTIFIERS);
    ofNullable(node.identifiers).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.ElseExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.Field node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.Field node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.Field node) {
    prepareChildVisit(Property.Field.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.Field.FUNCTION1);
    node.function1.ifPresent(this::_case);
    prepareChildVisit(Property.Field.FUNCTION2);
    node.function2.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.Field node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.FieldIdentifier node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.FieldIdentifier node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.FieldIdentifier node) {
    prepareChildVisit(Property.FieldIdentifier.ENTITY);
    node.entity.ifPresent(this::_case);
    prepareChildVisit(Property.FieldIdentifier.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.FieldIdentifier node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.FromClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.FromClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.FromClause node) {
    prepareChildVisit(Property.FromClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.FromClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.FromExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.FromExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.FromExpr node) {
    prepareChildVisit(Property.FromExpr.TABLE);
    ofNullable(node.table).ifPresent(this::_case);
    prepareChildVisit(Property.FromExpr.ALIAS);
    node.alias.ifPresent(this::_case);
    prepareChildVisit(Property.FromExpr.USING);
    node.using.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.FromExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.Geolocation node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.Geolocation.GeolocationExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.Geolocation.GeolocationExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.Geolocation.GeolocationExpr node) {
    prepareChildVisit(Property.GeolocationExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.Geolocation.GeolocationExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.GroupByClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.GroupByClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.GroupByClause node) {
    prepareChildVisit(Property.GroupByClause.TYPE);
    node.type.ifPresent(this::_case);
    prepareChildVisit(Property.GroupByClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.GroupByClause.HAVING);
    node.having.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.GroupByClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.GroupByExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.GroupByExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.GroupByExpr node) {
    prepareChildVisit(Property.GroupByExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.GroupByExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.GroupByType node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.GroupByType.GroupByCube node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.GroupByType.GroupByCube node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.GroupByType.GroupByCube node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.GroupByType.GroupByRollUp node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.GroupByType.GroupByRollUp node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.GroupByType.GroupByRollUp node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.HavingClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.HavingClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.HavingClause node) {
    prepareChildVisit(Property.HavingClause.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.HavingClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.LimitClause node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.LimitClause.LimitExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.LimitClause.LimitExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.LimitClause.LimitExpr node) {
    prepareChildVisit(Property.LimitExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.LimitClause.LimitExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.LimitClause.LimitValue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.LimitClause.LimitValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.LimitClause.LimitValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.OffsetClause node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.OffsetClause.OffsetExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OffsetClause.OffsetExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.OffsetClause.OffsetExpr node) {
    prepareChildVisit(Property.OffsetExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.OffsetClause.OffsetExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.OffsetClause.OffsetValue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OffsetClause.OffsetValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.OffsetClause.OffsetValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.Order node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.Order.OrderAsc node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.Order.OrderAsc node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.Order.OrderAsc node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.Order.OrderDesc node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.Order.OrderDesc node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.Order.OrderDesc node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.OrderByClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OrderByClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.OrderByClause node) {
    prepareChildVisit(Property.OrderByClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.OrderByClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.OrderByExpr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.OrderByExpr.OrderByDistance node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OrderByExpr.OrderByDistance node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.OrderByExpr.OrderByDistance node) {
    prepareChildVisit(Property.OrderByDistance.DISTANCE);
    ofNullable(node.distance).ifPresent(this::_case);
    prepareChildVisit(Property.OrderByDistance.ORDER);
    ofNullable(node.order).ifPresent(this::_case);
    prepareChildVisit(Property.OrderByDistance.NULLORDER);
    ofNullable(node.nullOrder).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.OrderByExpr.OrderByDistance node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.OrderByExpr.OrderByValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OrderByExpr.OrderByValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.OrderByExpr.OrderByValue node) {
    prepareChildVisit(Property.OrderByValue.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.OrderByValue.ORDER);
    ofNullable(node.order).ifPresent(this::_case);
    prepareChildVisit(Property.OrderByValue.NULLORDER);
    ofNullable(node.nullOrder).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.OrderByExpr.OrderByValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.OrderNull node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.OrderNull.OrderNullFirst node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OrderNull.OrderNullFirst node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.OrderNull.OrderNullFirst node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.OrderNull.OrderNullLast node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.OrderNull.OrderNullLast node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.OrderNull.OrderNullLast node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.Query node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.Query node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.Query node) {
    prepareChildVisit(Property.Query.SELECT);
    ofNullable(node.select).ifPresent(this::_case);
    prepareChildVisit(Property.Query.FROM);
    ofNullable(node.from).ifPresent(this::_case);
    prepareChildVisit(Property.Query.WHERE);
    node.where.ifPresent(this::_case);
    prepareChildVisit(Property.Query.WITH);
    node.with.ifPresent(this::_case);
    prepareChildVisit(Property.Query.WITHIDENTIFIERS);
    ofNullable(node.withIdentifiers).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.Query.GROUPBY);
    node.groupBy.ifPresent(this::_case);
    prepareChildVisit(Property.Query.ORDERBY);
    node.orderBy.ifPresent(this::_case);
    prepareChildVisit(Property.Query.LIMIT);
    node.limit.ifPresent(this::_case);
    prepareChildVisit(Property.Query.OFFSET);
    node.offset.ifPresent(this::_case);
    prepareChildVisit(Property.Query.BIND);
    node.bind.ifPresent(this::_case);
    prepareChildVisit(Property.Query.TRACKING);
    node.tracking.ifPresent(this::_case);
    prepareChildVisit(Property.Query.UPDATESTATS);
    node.updateStats.ifPresent(this::_case);
    prepareChildVisit(Property.Query.OPTIONS);
    node.options.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.Query node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.QueryExpr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.QueryExpr.ApexExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryExpr.ApexExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.QueryExpr.ApexExpr node) {
    prepareChildVisit(Property.ApexExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.QueryExpr.ApexExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.QueryExpr.LiteralExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryExpr.LiteralExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.QueryExpr.LiteralExpr node) {
    prepareChildVisit(Property.LiteralExpr.LITERAL);
    ofNullable(node.literal).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.QueryExpr.LiteralExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.QueryLiteral node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryDate node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryDate node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryDate node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryDateFormula node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryDateFormula node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryDateFormula node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryDateTime node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryDateTime node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryDateTime node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryFalse node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryFalse node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryFalse node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryMultiCurrency node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryMultiCurrency node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryMultiCurrency node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryNull node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryNull node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryNull node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryNumber node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryNumber node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryNumber node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryString node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryString node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryString node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryTime node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryTime node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryTime node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryLiteral.QueryTrue node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryLiteral.QueryTrue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryLiteral.QueryTrue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryDoubleEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryDoubleEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryDoubleEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryExcludes node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryExcludes node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryExcludes node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryGreaterThan node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryGreaterThan node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryGreaterThan node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryGreaterThanEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryGreaterThanEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryGreaterThanEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryIn node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryIn node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryIn node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryIncludes node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryIncludes node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryIncludes node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryLessThan node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryLessThan node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryLessThan node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryLessThanEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryLessThanEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryLessThanEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryLike node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryLike node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryLike node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryNotEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryNotEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryNotEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryNotIn node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryNotIn node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryNotIn node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryNotTripleEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryNotTripleEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryNotTripleEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOp.QueryTripleEqual node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOp.QueryTripleEqual node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOp.QueryTripleEqual node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOption node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.QueryOption.IncludeDeleted node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOption.IncludeDeleted node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOption.IncludeDeleted node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryOption.LockRows node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryOption.LockRows node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.QueryOption.LockRows node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.QueryUsingClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.QueryUsingClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.QueryUsingClause node) {
    prepareChildVisit(Property.QueryUsingClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.QueryUsingClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.SelectClause node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.SelectClause.SelectColumnClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectClause.SelectColumnClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.SelectClause.SelectColumnClause node) {
    prepareChildVisit(Property.SelectColumnClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.SelectClause.SelectColumnClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.SelectClause.SelectCountClause node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectClause.SelectCountClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.SelectClause.SelectCountClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.SelectExpr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.SelectExpr.SelectCaseExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectExpr.SelectCaseExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.SelectExpr.SelectCaseExpr node) {
    prepareChildVisit(Property.SelectCaseExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.SelectCaseExpr.ALIAS);
    node.alias.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.SelectExpr.SelectCaseExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.SelectExpr.SelectColumnExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectExpr.SelectColumnExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.SelectExpr.SelectColumnExpr node) {
    prepareChildVisit(Property.SelectColumnExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.SelectColumnExpr.ALIAS);
    node.alias.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.SelectExpr.SelectColumnExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.SelectExpr.SelectDistanceExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectExpr.SelectDistanceExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.SelectExpr.SelectDistanceExpr node) {
    prepareChildVisit(Property.SelectDistanceExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
    prepareChildVisit(Property.SelectDistanceExpr.ALIAS);
    node.alias.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.SelectExpr.SelectDistanceExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.SelectExpr.SelectInnerQuery node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.SelectExpr.SelectInnerQuery node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.SelectExpr.SelectInnerQuery node) {
    prepareChildVisit(Property.SelectInnerQuery.QUERY);
    ofNullable(node.query).ifPresent(this::_case);
    prepareChildVisit(Property.SelectInnerQuery.ALIAS);
    node.alias.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.SelectExpr.SelectInnerQuery node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.TrackingType node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.TrackingType.ForReference node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.TrackingType.ForReference node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.TrackingType.ForReference node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.TrackingType.ForView node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.TrackingType.ForView node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.TrackingType.ForView node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.UpdateStatsClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UpdateStatsClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.UpdateStatsClause node) {
    prepareChildVisit(Property.UpdateStatsClause.OPTIONS);
    ofNullable(node.options).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.UpdateStatsClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.UpdateStatsOption node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.UpdateStatsOption.UpdateTracking node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UpdateStatsOption.UpdateTracking node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.UpdateStatsOption.UpdateTracking node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.UpdateStatsOption.UpdateViewStat node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UpdateStatsOption.UpdateViewStat node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.UpdateStatsOption.UpdateViewStat node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.UsingExpr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.UsingExpr.Using node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UsingExpr.Using node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.UsingExpr.Using node) {
    prepareChildVisit(Property.Using.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.Using.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.UsingExpr.Using node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.UsingExpr.UsingEquals node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UsingExpr.UsingEquals node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.UsingExpr.UsingEquals node) {
    prepareChildVisit(Property.UsingEquals.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.UsingEquals.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.UsingExpr.UsingEquals node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.UsingExpr.UsingId node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.UsingExpr.UsingId node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.UsingExpr.UsingId node) {
    prepareChildVisit(Property.UsingId.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.UsingId.ID);
    ofNullable(node.id).ifPresent(this::_case);
    prepareChildVisit(Property.UsingId.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.UsingExpr.UsingId node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhenExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhenExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.WhenExpr node) {
    prepareChildVisit(Property.WhenExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhenExpr.IDENTIFIERS);
    ofNullable(node.identifiers).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.WhenExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhenOp node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhenOp node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhenOp node) {
    prepareChildVisit(Property.WhenOp.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhenOp node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereCalcOp node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WhereCalcOp.WhereCalcMinus node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereCalcOp.WhereCalcMinus node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.WhereCalcOp.WhereCalcMinus node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhereCalcOp.WhereCalcPlus node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereCalcOp.WhereCalcPlus node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.WhereCalcOp.WhereCalcPlus node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhereClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.WhereClause node) {
    prepareChildVisit(Property.WhereClause.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhereCompoundOp node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WhereCompoundOp.QueryAnd node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereCompoundOp.QueryAnd node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.WhereCompoundOp.QueryAnd node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhereCompoundOp.QueryOr node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereCompoundOp.QueryOr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.WhereCompoundOp.QueryOr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WhereExpr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereCalcExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereCalcExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereCalcExpr node) {
    prepareChildVisit(Property.WhereCalcExpr.FIELD1);
    ofNullable(node.field1).ifPresent(this::_case);
    prepareChildVisit(Property.WhereCalcExpr.CALC);
    ofNullable(node.calc).ifPresent(this::_case);
    prepareChildVisit(Property.WhereCalcExpr.FIELD2);
    ofNullable(node.field2).ifPresent(this::_case);
    prepareChildVisit(Property.WhereCalcExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereCalcExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereCalcExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereCompoundExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereCompoundExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereCompoundExpr node) {
    prepareChildVisit(Property.WhereCompoundExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereCompoundExpr.EXPR);
    ofNullable(node.expr).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereCompoundExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereDistanceExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereDistanceExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereDistanceExpr node) {
    prepareChildVisit(Property.WhereDistanceExpr.DISTANCE);
    ofNullable(node.distance).ifPresent(this::_case);
    prepareChildVisit(Property.WhereDistanceExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereDistanceExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereDistanceExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereInnerExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereInnerExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereInnerExpr node) {
    prepareChildVisit(Property.WhereInnerExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.WhereInnerExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereInnerExpr.INNER);
    ofNullable(node.inner).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereInnerExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereOpExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereOpExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereOpExpr node) {
    prepareChildVisit(Property.WhereOpExpr.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.WhereOpExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereOpExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereOpExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereOpExprs node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereOpExprs node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereOpExprs node) {
    prepareChildVisit(Property.WhereOpExprs.FIELD);
    ofNullable(node.field).ifPresent(this::_case);
    prepareChildVisit(Property.WhereOpExprs.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereOpExprs.EXPR);
    ofNullable(node.expr).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereOpExprs node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereExpr.WhereUnaryExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereExpr.WhereUnaryExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WhereExpr.WhereUnaryExpr node) {
    prepareChildVisit(Property.WhereUnaryExpr.OP);
    ofNullable(node.op).ifPresent(this::_case);
    prepareChildVisit(Property.WhereUnaryExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WhereExpr.WhereUnaryExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WhereUnaryOp node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WhereUnaryOp node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.soql.WhereUnaryOp node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WithClause node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WithClause.WithDataCategories node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithClause.WithDataCategories node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.WithClause.WithDataCategories node) {
    prepareChildVisit(Property.WithDataCategories.CATEGORIES);
    ofNullable(node.categories).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.WithClause.WithDataCategories node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WithClause.WithValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithClause.WithValue node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.soql.WithClause.WithValue node) {
    prepareChildVisit(Property.WithValue.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.WithValue.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WithClause.WithValue node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.soql.WithIdentifierClause node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WithIdentifierClause.WithIdentifier node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithIdentifierClause.WithIdentifier node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WithIdentifierClause.WithIdentifier node) {
    prepareChildVisit(Property.WithIdentifier.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WithIdentifierClause.WithIdentifier node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple node) {
    prepareChildVisit(Property.WithIdentifierTuple.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
    prepareChildVisit(Property.WithIdentifierTuple.KEYVALUES);
    ofNullable(node.keyValues).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WithKeyValue node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.soql.WithKeyValue.BooleanKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithKeyValue.BooleanKeyValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WithKeyValue.BooleanKeyValue node) {
    prepareChildVisit(Property.BooleanKeyValue.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WithKeyValue.BooleanKeyValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WithKeyValue.NumberKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithKeyValue.NumberKeyValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WithKeyValue.NumberKeyValue node) {
    prepareChildVisit(Property.NumberKeyValue.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WithKeyValue.NumberKeyValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.soql.WithKeyValue.StringKeyValue node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.soql.WithKeyValue.StringKeyValue node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.soql.WithKeyValue.StringKeyValue node) {
    prepareChildVisit(Property.StringKeyValue.IDENTIFIER);
    ofNullable(node.identifier).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.soql.WithKeyValue.StringKeyValue node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.DivisionValue node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.sosl.DivisionValue.DivisionExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.DivisionValue.DivisionExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.DivisionValue.DivisionExpr node) {
    prepareChildVisit(Property.DivisionExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.DivisionValue.DivisionExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.DivisionValue.DivisionLiteral node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.DivisionValue.DivisionLiteral node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.sosl.DivisionValue.DivisionLiteral node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.FindClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.FindClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.FindClause node) {
    prepareChildVisit(Property.FindClause.SEARCH);
    ofNullable(node.search).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.FindClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.FindValue node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.sosl.FindValue.FindExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.FindValue.FindExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.FindValue.FindExpr node) {
    prepareChildVisit(Property.FindExpr.EXPR);
    ofNullable(node.expr).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.FindValue.FindExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.InClause node) {
    enter(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.InClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void exit(apex.jorje.data.sosl.InClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.ReturningClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.ReturningClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.ReturningClause node) {
    prepareChildVisit(Property.ReturningClause.EXPRS);
    ofNullable(node.exprs).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.sosl.ReturningClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.ReturningExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.ReturningExpr node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.ReturningExpr node) {
    prepareChildVisit(Property.ReturningExpr.NAME);
    ofNullable(node.name).ifPresent(this::_case);
    prepareChildVisit(Property.ReturningExpr.SELECT);
    node.select.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.ReturningExpr node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.ReturningSelectExpr node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.ReturningSelectExpr node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.ReturningSelectExpr node) {
    prepareChildVisit(Property.ReturningSelectExpr.FIELDS);
    ofNullable(node.fields).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.ReturningSelectExpr.USING);
    node.using.ifPresent(this::_case);
    prepareChildVisit(Property.ReturningSelectExpr.WHERE);
    node.where.ifPresent(this::_case);
    prepareChildVisit(Property.ReturningSelectExpr.ORDERBY);
    node.orderBy.ifPresent(this::_case);
    prepareChildVisit(Property.ReturningSelectExpr.LIMIT);
    node.limit.ifPresent(this::_case);
    prepareChildVisit(Property.ReturningSelectExpr.OFFSET);
    node.offset.ifPresent(this::_case);
    prepareChildVisit(Property.ReturningSelectExpr.BIND);
    node.bind.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.ReturningSelectExpr node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.Search node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.Search node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.Search node) {
    prepareChildVisit(Property.Search.FIND);
    ofNullable(node.find).ifPresent(this::_case);
    prepareChildVisit(Property.Search.IN);
    node.in.ifPresent(this::_case);
    prepareChildVisit(Property.Search.RETURNING);
    node.returning.ifPresent(this::_case);
    prepareChildVisit(Property.Search.DIVISION);
    node.division.ifPresent(this::_case);
    prepareChildVisit(Property.Search.DATACATEGORY);
    node.dataCategory.ifPresent(this::_case);
    prepareChildVisit(Property.Search.WITHS);
    ofNullable(node.withs).ifPresent(list -> list.forEach(this::_case));
    prepareChildVisit(Property.Search.USING);
    node.using.ifPresent(this::_case);
    prepareChildVisit(Property.Search.LIMIT);
    node.limit.ifPresent(this::_case);
    prepareChildVisit(Property.Search.UPDATESTATS);
    node.updateStats.ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.Search node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.SearchUsingClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.SearchUsingClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.SearchUsingClause node) {
    prepareChildVisit(Property.SearchUsingClause.TYPE);
    ofNullable(node.type).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.SearchUsingClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.SearchWithClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.SearchWithClause node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.SearchWithClause node) {
    prepareChildVisit(Property.SearchWithClause.NAME);
    ofNullable(node.name).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.SearchWithClause node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.UsingType node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.UsingType node) {
    defaultEnter(node, null);
  }

  protected void visitChildren(apex.jorje.data.sosl.UsingType node) {
    prepareChildVisit(Property.UsingType.FILTER);
    ofNullable(node.filter).ifPresent(this::_case);
    prepareChildVisit(Property.UsingType.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.UsingType node) {
    defaultExit(node, null);
  }

  public void _case(apex.jorje.data.sosl.WithDataCategoryClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.WithDataCategoryClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.WithDataCategoryClause node) {
    prepareChildVisit(Property.WithDataCategoryClause.CATEGORIES);
    ofNullable(node.categories).ifPresent(list -> list.forEach(this::_case));
  }

  protected void exit(apex.jorje.data.sosl.WithDataCategoryClause node) {
    defaultExit(node, valid(node.loc));
  }

  public void _case(apex.jorje.data.sosl.WithDivisionClause node) {
    enter(node);
    visitChildren(node);
    exit(node);
  }

  protected void enter(apex.jorje.data.sosl.WithDivisionClause node) {
    defaultEnter(node, valid(node.loc));
  }

  protected void visitChildren(apex.jorje.data.sosl.WithDivisionClause node) {
    prepareChildVisit(Property.WithDivisionClause.VALUE);
    ofNullable(node.value).ifPresent(this::_case);
  }

  protected void exit(apex.jorje.data.sosl.WithDivisionClause node) {
    defaultExit(node, valid(node.loc));
  }

  public interface Property {
    Class<?> declaringClass();
    String accessor();
    boolean isOptional();
    boolean isList();

    enum AnnotationKeyValue implements Property {
      KEY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue.class; }
        public String accessor()         { return "key"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum FieldMember implements Property {
      VARIABLEDECLS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.FieldMember.class; }
        public String accessor()         { return "variableDecls"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InnerClassMember implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.InnerClassMember.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InnerEnumMember implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.InnerEnumMember.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InnerInterfaceMember implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.InnerInterfaceMember.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum MethodMember implements Property {
      METHODDECL {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.MethodMember.class; }
        public String accessor()         { return "methodDecl"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum PropertyMember implements Property {
      PROPERTYDECL {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.PropertyMember.class; }
        public String accessor()         { return "propertyDecl"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum StaticStmntBlockMember implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.StaticStmntBlockMember.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum StmntBlockMember implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.BlockMember.StmntBlockMember.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum CatchBlock implements Property {
      PARAMETER {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CatchBlock.class; }
        public String accessor()         { return "parameter"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CatchBlock.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ClassDecl implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TYPEARGUMENTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "typeArguments"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return true; }
      },
      MEMBERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "members"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      SUPERCLASS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "superClass"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      INTERFACES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ClassDecl.class; }
        public String accessor()         { return "interfaces"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum AnonymousBlockUnit implements Property {
      MEMBERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit.class; }
        public String accessor()         { return "members"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ClassDeclUnit implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.ClassDeclUnit.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum EnumDeclUnit implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.EnumDeclUnit.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InterfaceDeclUnit implements Property {
      BODY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit.class; }
        public String accessor()         { return "body"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum TriggerDeclUnit implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TARGET {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit.class; }
        public String accessor()         { return "target"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      MEMBERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit.class; }
        public String accessor()         { return "members"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ElseBlock implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ElseBlock.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum EnumDecl implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.EnumDecl.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.EnumDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      MEMBERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.EnumDecl.class; }
        public String accessor()         { return "members"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ArrayExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.ArrayExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      INDEX {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.ArrayExpr.class; }
        public String accessor()         { return "index"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum AssignmentExpr implements Property {
      LEFT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.AssignmentExpr.class; }
        public String accessor()         { return "left"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RIGHT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.AssignmentExpr.class; }
        public String accessor()         { return "right"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum BinaryExpr implements Property {
      LEFT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.BinaryExpr.class; }
        public String accessor()         { return "left"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RIGHT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.BinaryExpr.class; }
        public String accessor()         { return "right"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum BooleanExpr implements Property {
      LEFT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.BooleanExpr.class; }
        public String accessor()         { return "left"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RIGHT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.BooleanExpr.class; }
        public String accessor()         { return "right"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum CastExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.CastExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.CastExpr.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ClassRefExpr implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.ClassRefExpr.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InstanceOf implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.InstanceOf.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.InstanceOf.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum JavaMethodCallExpr implements Property {
      NAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.JavaMethodCallExpr.class; }
        public String accessor()         { return "names"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.JavaMethodCallExpr.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum JavaVariableExpr implements Property {
      NAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.JavaVariableExpr.class; }
        public String accessor()         { return "names"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum MethodCallExpr implements Property {
      DOTTEDEXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.MethodCallExpr.class; }
        public String accessor()         { return "dottedExpr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      NAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.MethodCallExpr.class; }
        public String accessor()         { return "names"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.MethodCallExpr.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NestedExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.NestedExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum NewExpr implements Property {
      CREATOR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.NewExpr.class; }
        public String accessor()         { return "creator"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum PostfixExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.PostfixExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum PrefixExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.PrefixExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum SoqlExpr implements Property {
      QUERY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.SoqlExpr.class; }
        public String accessor()         { return "query"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum SoslExpr implements Property {
      SEARCH {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.SoslExpr.class; }
        public String accessor()         { return "search"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum SuperMethodCallExpr implements Property {
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.SuperMethodCallExpr.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum TernaryExpr implements Property {
      CONDITION {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.TernaryExpr.class; }
        public String accessor()         { return "condition"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TRUEEXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.TernaryExpr.class; }
        public String accessor()         { return "trueExpr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FALSEEXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.TernaryExpr.class; }
        public String accessor()         { return "falseExpr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ThisMethodCallExpr implements Property {
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.ThisMethodCallExpr.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum TriggerVariableExpr implements Property {
      VARIABLE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.TriggerVariableExpr.class; }
        public String accessor()         { return "variable"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum VariableExpr implements Property {
      DOTTEDEXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.VariableExpr.class; }
        public String accessor()         { return "dottedExpr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      NAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Expr.VariableExpr.class; }
        public String accessor()         { return "names"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum FinallyBlock implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.FinallyBlock.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum CStyleForControl implements Property {
      INITS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForControl.CStyleForControl.class; }
        public String accessor()         { return "inits"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      CONDITION {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForControl.CStyleForControl.class; }
        public String accessor()         { return "condition"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      CONTROL {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForControl.CStyleForControl.class; }
        public String accessor()         { return "control"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum EnhancedForControl implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForControl.EnhancedForControl.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      INIT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForControl.EnhancedForControl.class; }
        public String accessor()         { return "init"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ForInit implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForInit.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForInit.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum ForInits implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForInits.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      INITS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ForInits.class; }
        public String accessor()         { return "inits"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum IfBlock implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.IfBlock.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.IfBlock.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum InterfaceDecl implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.InterfaceDecl.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.InterfaceDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      TYPEARGUMENTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.InterfaceDecl.class; }
        public String accessor()         { return "typeArguments"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return true; }
      },
      MEMBERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.InterfaceDecl.class; }
        public String accessor()         { return "members"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      SUPERINTERFACE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.InterfaceDecl.class; }
        public String accessor()         { return "superInterface"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum MapLiteralKeyValue implements Property {
      KEY {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MapLiteralKeyValue.class; }
        public String accessor()         { return "key"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MapLiteralKeyValue.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum MethodDecl implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MethodDecl.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MethodDecl.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MethodDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      PARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MethodDecl.class; }
        public String accessor()         { return "parameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.MethodDecl.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum Annotation implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Modifier.Annotation.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      PARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Modifier.Annotation.class; }
        public String accessor()         { return "parameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NameValueParameter implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NameValueParameter.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NameValueParameter.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum NewKeyValue implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewKeyValue.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      KEYVALUES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewKeyValue.class; }
        public String accessor()         { return "keyValues"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NewListInit implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewListInit.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewListInit.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum NewListLiteral implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewListLiteral.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      VALUES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewListLiteral.class; }
        public String accessor()         { return "values"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NewMapInit implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewMapInit.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewMapInit.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum NewMapLiteral implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewMapLiteral.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      PAIRS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewMapLiteral.class; }
        public String accessor()         { return "pairs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NewSetInit implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewSetInit.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewSetInit.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum NewSetLiteral implements Property {
      TYPES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewSetLiteral.class; }
        public String accessor()         { return "types"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      VALUES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewSetLiteral.class; }
        public String accessor()         { return "values"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum NewStandard implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewStandard.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.NewObject.NewStandard.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ParameterRef implements Property {
      GETMODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ParameterRef.class; }
        public String accessor()         { return "getModifiers()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      GETNAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ParameterRef.class; }
        public String accessor()         { return "getName()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      GETTYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.ParameterRef.class; }
        public String accessor()         { return "getType()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum PropertyDecl implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyDecl.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyDecl.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      GETTER {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyDecl.class; }
        public String accessor()         { return "getter"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      SETTER {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyDecl.class; }
        public String accessor()         { return "setter"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum PropertyGetter implements Property {
      MODIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyGetter.class; }
        public String accessor()         { return "modifier"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertyGetter.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum PropertySetter implements Property {
      MODIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertySetter.class; }
        public String accessor()         { return "modifier"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.PropertySetter.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum BlockStmnt implements Property {
      STMNTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.BlockStmnt.class; }
        public String accessor()         { return "stmnts"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum DmlDeleteStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlDeleteStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlDeleteStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DmlInsertStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlInsertStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlInsertStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DmlMergeStmnt implements Property {
      EXPR1 {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlMergeStmnt.class; }
        public String accessor()         { return "expr1"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR2 {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlMergeStmnt.class; }
        public String accessor()         { return "expr2"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlMergeStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DmlUndeleteStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DmlUpdateStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUpdateStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUpdateStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DmlUpsertStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUpsertStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ID {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUpsertStmnt.class; }
        public String accessor()         { return "id"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      RUNASMODE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DmlUpsertStmnt.class; }
        public String accessor()         { return "runAsMode"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum DoLoop implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DoLoop.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      CONDITION {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.DoLoop.class; }
        public String accessor()         { return "condition"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ExpressionStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.ExpressionStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ForLoop implements Property {
      FORCONTROL {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.ForLoop.class; }
        public String accessor()         { return "forControl"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.ForLoop.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum IfElseBlock implements Property {
      IFBLOCKS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.IfElseBlock.class; }
        public String accessor()         { return "ifBlocks"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      ELSEBLOCK {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.IfElseBlock.class; }
        public String accessor()         { return "elseBlock"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum ReturnStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.ReturnStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum RunAsBlock implements Property {
      INPUTPARAMETERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.RunAsBlock.class; }
        public String accessor()         { return "inputParameters"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.RunAsBlock.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum SwitchStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.SwitchStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      WHENBLOCKS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.SwitchStmnt.class; }
        public String accessor()         { return "whenBlocks"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ThrowStmnt implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.ThrowStmnt.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum TryCatchFinallyBlock implements Property {
      TRYBLOCK {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock.class; }
        public String accessor()         { return "tryBlock"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      CATCHBLOCKS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock.class; }
        public String accessor()         { return "catchBlocks"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      FINALLYBLOCK {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock.class; }
        public String accessor()         { return "finallyBlock"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum VariableDeclStmnt implements Property {
      VARIABLEDECLS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.VariableDeclStmnt.class; }
        public String accessor()         { return "variableDecls"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhileLoop implements Property {
      CONDITION {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.WhileLoop.class; }
        public String accessor()         { return "condition"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.Stmnt.WhileLoop.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum TypeRef implements Property {
      GETTYPEARGUMENTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRef.class; }
        public String accessor()         { return "getTypeArguments()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      GETNAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRef.class; }
        public String accessor()         { return "getNames()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ArrayTypeRef implements Property {
      GETHELDTYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRefs.ArrayTypeRef.class; }
        public String accessor()         { return "getHeldType()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ClassTypeRef implements Property {
      GETTYPEARGUMENTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRefs.ClassTypeRef.class; }
        public String accessor()         { return "getTypeArguments()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      GETNAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRefs.ClassTypeRef.class; }
        public String accessor()         { return "getNames()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum JavaTypeRef implements Property {
      GETTYPEARGUMENTS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRefs.JavaTypeRef.class; }
        public String accessor()         { return "getTypeArguments()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      GETNAMES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.TypeRefs.JavaTypeRef.class; }
        public String accessor()         { return "getNames()"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum VariableDecl implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.VariableDecl.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ASSIGNMENT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.VariableDecl.class; }
        public String accessor()         { return "assignment"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum VariableDecls implements Property {
      MODIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.VariableDecls.class; }
        public String accessor()         { return "modifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.ast.VariableDecls.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      DECLS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.VariableDecls.class; }
        public String accessor()         { return "decls"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ElseWhen implements Property {
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.ElseWhen.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum TypeWhen implements Property {
      TYPEREF {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.TypeWhen.class; }
        public String accessor()         { return "typeRef"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.TypeWhen.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.TypeWhen.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ValueWhen implements Property {
      WHENCASES {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.ValueWhen.class; }
        public String accessor()         { return "whenCases"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      STMNT {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenBlock.ValueWhen.class; }
        public String accessor()         { return "stmnt"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum EnumCase implements Property {
      IDENTIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenCase.EnumCase.class; }
        public String accessor()         { return "identifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum LiteralCase implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.ast.WhenCase.LiteralCase.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum BindClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.BindClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum BindExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.BindExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.BindExpr.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum CaseExpr implements Property {
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.CaseExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      WHENBRANCHES {
        public Class<?> declaringClass() { return apex.jorje.data.soql.CaseExpr.class; }
        public String accessor()         { return "whenBranches"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      ELSEBRANCH {
        public Class<?> declaringClass() { return apex.jorje.data.soql.CaseExpr.class; }
        public String accessor()         { return "elseBranch"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum CaseOp implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.CaseOp.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ColonExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.ColonExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum DataCategory implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.DataCategory.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.DataCategory.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      CATEGORIES {
        public Class<?> declaringClass() { return apex.jorje.data.soql.DataCategory.class; }
        public String accessor()         { return "categories"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum DistanceFunctionExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.DistanceFunctionExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      LOCATION {
        public Class<?> declaringClass() { return apex.jorje.data.soql.DistanceFunctionExpr.class; }
        public String accessor()         { return "location"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ElseExpr implements Property {
      IDENTIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.ElseExpr.class; }
        public String accessor()         { return "identifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum Field implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Field.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FUNCTION1 {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Field.class; }
        public String accessor()         { return "function1"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      FUNCTION2 {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Field.class; }
        public String accessor()         { return "function2"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum FieldIdentifier implements Property {
      ENTITY {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FieldIdentifier.class; }
        public String accessor()         { return "entity"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FieldIdentifier.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum FromClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FromClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum FromExpr implements Property {
      TABLE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FromExpr.class; }
        public String accessor()         { return "table"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ALIAS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FromExpr.class; }
        public String accessor()         { return "alias"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      USING {
        public Class<?> declaringClass() { return apex.jorje.data.soql.FromExpr.class; }
        public String accessor()         { return "using"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum GeolocationExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Geolocation.GeolocationExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum GroupByClause implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.GroupByClause.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.GroupByClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      HAVING {
        public Class<?> declaringClass() { return apex.jorje.data.soql.GroupByClause.class; }
        public String accessor()         { return "having"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum GroupByExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.GroupByExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum HavingClause implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.HavingClause.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum LimitExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.LimitClause.LimitExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum OffsetExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OffsetClause.OffsetExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum OrderByClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum OrderByDistance implements Property {
      DISTANCE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByDistance.class; }
        public String accessor()         { return "distance"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ORDER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByDistance.class; }
        public String accessor()         { return "order"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      NULLORDER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByDistance.class; }
        public String accessor()         { return "nullOrder"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum OrderByValue implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByValue.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ORDER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByValue.class; }
        public String accessor()         { return "order"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      NULLORDER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.OrderByExpr.OrderByValue.class; }
        public String accessor()         { return "nullOrder"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum Query implements Property {
      SELECT {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "select"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FROM {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "from"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      WHERE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "where"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      WITH {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "with"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      WITHIDENTIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "withIdentifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      GROUPBY {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "groupBy"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      ORDERBY {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "orderBy"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      LIMIT {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "limit"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      OFFSET {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "offset"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      BIND {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "bind"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      TRACKING {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "tracking"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      UPDATESTATS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "updateStats"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      OPTIONS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.Query.class; }
        public String accessor()         { return "options"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum ApexExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.QueryExpr.ApexExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum LiteralExpr implements Property {
      LITERAL {
        public Class<?> declaringClass() { return apex.jorje.data.soql.QueryExpr.LiteralExpr.class; }
        public String accessor()         { return "literal"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum QueryUsingClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.QueryUsingClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum SelectColumnClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectClause.SelectColumnClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum SelectCaseExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectCaseExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ALIAS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectCaseExpr.class; }
        public String accessor()         { return "alias"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum SelectColumnExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectColumnExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ALIAS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectColumnExpr.class; }
        public String accessor()         { return "alias"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum SelectDistanceExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectDistanceExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ALIAS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectDistanceExpr.class; }
        public String accessor()         { return "alias"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum SelectInnerQuery implements Property {
      QUERY {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectInnerQuery.class; }
        public String accessor()         { return "query"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ALIAS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.SelectExpr.SelectInnerQuery.class; }
        public String accessor()         { return "alias"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum UpdateStatsClause implements Property {
      OPTIONS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UpdateStatsClause.class; }
        public String accessor()         { return "options"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum Using implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.Using.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.Using.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum UsingEquals implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.UsingEquals.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.UsingEquals.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum UsingId implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.UsingId.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      ID {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.UsingId.class; }
        public String accessor()         { return "id"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.UsingExpr.UsingId.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhenExpr implements Property {
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhenExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      IDENTIFIERS {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhenExpr.class; }
        public String accessor()         { return "identifiers"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum WhenOp implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhenOp.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereClause implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereClause.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereCalcExpr implements Property {
      FIELD1 {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCalcExpr.class; }
        public String accessor()         { return "field1"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      CALC {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCalcExpr.class; }
        public String accessor()         { return "calc"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      FIELD2 {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCalcExpr.class; }
        public String accessor()         { return "field2"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCalcExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCalcExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereCompoundExpr implements Property {
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCompoundExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereCompoundExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum WhereDistanceExpr implements Property {
      DISTANCE {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereDistanceExpr.class; }
        public String accessor()         { return "distance"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereDistanceExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereDistanceExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereInnerExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereInnerExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereInnerExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      INNER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereInnerExpr.class; }
        public String accessor()         { return "inner"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereOpExpr implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExpr.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WhereOpExprs implements Property {
      FIELD {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExprs.class; }
        public String accessor()         { return "field"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExprs.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereOpExprs.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum WhereUnaryExpr implements Property {
      OP {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereUnaryExpr.class; }
        public String accessor()         { return "op"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WhereExpr.WhereUnaryExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WithDataCategories implements Property {
      CATEGORIES {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithClause.WithDataCategories.class; }
        public String accessor()         { return "categories"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum WithValue implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithClause.WithValue.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithClause.WithValue.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WithIdentifier implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithIdentifierClause.WithIdentifier.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WithIdentifierTuple implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      KEYVALUES {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithIdentifierClause.WithIdentifierTuple.class; }
        public String accessor()         { return "keyValues"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum BooleanKeyValue implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithKeyValue.BooleanKeyValue.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum NumberKeyValue implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithKeyValue.NumberKeyValue.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum StringKeyValue implements Property {
      IDENTIFIER {
        public Class<?> declaringClass() { return apex.jorje.data.soql.WithKeyValue.StringKeyValue.class; }
        public String accessor()         { return "identifier"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum DivisionExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.DivisionValue.DivisionExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum FindClause implements Property {
      SEARCH {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.FindClause.class; }
        public String accessor()         { return "search"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum FindExpr implements Property {
      EXPR {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.FindValue.FindExpr.class; }
        public String accessor()         { return "expr"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum ReturningClause implements Property {
      EXPRS {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningClause.class; }
        public String accessor()         { return "exprs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum ReturningExpr implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningExpr.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      SELECT {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningExpr.class; }
        public String accessor()         { return "select"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum ReturningSelectExpr implements Property {
      FIELDS {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "fields"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      USING {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "using"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      WHERE {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "where"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      ORDERBY {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "orderBy"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      LIMIT {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "limit"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      OFFSET {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "offset"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      BIND {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.ReturningSelectExpr.class; }
        public String accessor()         { return "bind"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum Search implements Property {
      FIND {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "find"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      IN {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "in"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      RETURNING {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "returning"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      DIVISION {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "division"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      DATACATEGORY {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "dataCategory"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      WITHS {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "withs"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
      USING {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "using"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      LIMIT {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "limit"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
      UPDATESTATS {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.Search.class; }
        public String accessor()         { return "updateStats"; }
        public boolean isOptional()      { return true; }
        public boolean isList()          { return false; }
      },
    }

    enum SearchUsingClause implements Property {
      TYPE {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.SearchUsingClause.class; }
        public String accessor()         { return "type"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum SearchWithClause implements Property {
      NAME {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.SearchWithClause.class; }
        public String accessor()         { return "name"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum UsingType implements Property {
      FILTER {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.UsingType.class; }
        public String accessor()         { return "filter"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.UsingType.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

    enum WithDataCategoryClause implements Property {
      CATEGORIES {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.WithDataCategoryClause.class; }
        public String accessor()         { return "categories"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return true; }
      },
    }

    enum WithDivisionClause implements Property {
      VALUE {
        public Class<?> declaringClass() { return apex.jorje.data.sosl.WithDivisionClause.class; }
        public String accessor()         { return "value"; }
        public boolean isOptional()      { return false; }
        public boolean isList()          { return false; }
      },
    }

  }

}
