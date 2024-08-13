package org.sonarsource.apex.parser.poc.parser;

import org.sonarsource.apex.parser.poc.lexer.Tokens;

import static java.util.Optional.ofNullable;

public class SimpleConverterGenerated extends SimpleConverterBase implements
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
  apex.jorje.data.ast.WhenCase.SwitchBlock {

  public SimpleConverterGenerated(Tokens tokens) {
    super(tokens);
  }

  public void _case(apex.jorje.data.Identifier node) {
    addNode(type(node), node.getLoc(), () -> {
    });
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter.AnnotationKeyValue node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.key).ifPresent(this::_case);
      ofNullable(node.value).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.AnnotationParameter.AnnotationString node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.AnnotationValue node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.FalseAnnotationValue node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.StringAnnotationValue node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.AnnotationValue.TrueAnnotationValue node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.BlockMember.FieldMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.variableDecls).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerClassMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerEnumMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.InnerInterfaceMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.MethodMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.methodDecl).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.PropertyMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.propertyDecl).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.StaticStmntBlockMember node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.BlockMember.StmntBlockMember node) {
    addNode(type(node), null, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.CatchBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.parameter).ifPresent(this::_case);
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.ClassDecl node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.name).ifPresent(this::_case);
      node.typeArguments.ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
      node.superClass.ifPresent(this::_case);
      ofNullable(node.interfaces).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.CompilationUnit node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.AnonymousBlockUnit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.ClassDeclUnit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.EnumDeclUnit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.InterfaceDeclUnit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.body).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.TriggerDeclUnit node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.target).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.ElseBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.EnumDecl node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Expr.ArrayExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      ofNullable(node.index).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.AssignmentExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.left).ifPresent(this::_case);
      ofNullable(node.right).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.BinaryExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.left).ifPresent(this::_case);
      ofNullable(node.right).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.BooleanExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.left).ifPresent(this::_case);
      ofNullable(node.right).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.CastExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      ofNullable(node.type).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.ClassRefExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.type).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.InstanceOf node) {
    addNode(type(node), null, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      ofNullable(node.type).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.JavaMethodCallExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr.JavaVariableExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr.LiteralExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.MethodCallExpr node) {
    addNode(type(node), null, () -> {
      node.dottedExpr.ifPresent(this::_case);
      ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr.NestedExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.NewExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.creator).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.PackageVersionExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.PostfixExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.PrefixExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.SoqlExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.SoslExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.SuperMethodCallExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr.SuperVariableExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.TernaryExpr node) {
    addNode(type(node), null, () -> {
      ofNullable(node.condition).ifPresent(this::_case);
      ofNullable(node.trueExpr).ifPresent(this::_case);
      ofNullable(node.falseExpr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.ThisMethodCallExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Expr.ThisVariableExpr node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Expr.TriggerVariableExpr node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.variable).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Expr.VariableExpr node) {
    addNode(type(node), null, () -> {
      node.dottedExpr.ifPresent(this::_case);
      ofNullable(node.names).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.FinallyBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.ForControl node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.ForControl.CStyleForControl node) {
    addNode(type(node), null, () -> {
      node.inits.ifPresent(this::_case);
      node.condition.ifPresent(this::_case);
      node.control.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.ForControl.EnhancedForControl node) {
    addNode(type(node), null, () -> {
      node.type.ifPresent(this::_case);
      ofNullable(node.init).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.ForInit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.name).ifPresent(list -> list.forEach(this::_case));
      node.expr.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.ForInits node) {
    addNode(type(node), null, () -> {
      node.type.ifPresent(this::_case);
      ofNullable(node.inits).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.IfBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.InterfaceDecl node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.name).ifPresent(this::_case);
      node.typeArguments.ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.members).ifPresent(list -> list.forEach(this::_case));
      node.superInterface.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.MapLiteralKeyValue node) {
    addNode(type(node), null, () -> {
      ofNullable(node.key).ifPresent(this::_case);
      ofNullable(node.value).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.MethodDecl node) {
    addNode(type(node), null, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      node.type.ifPresent(this::_case);
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.parameters).ifPresent(list -> list.forEach(this::_case));
      node.stmnt.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Modifier node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Modifier.AbstractModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.Annotation node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.parameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.FinalModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.GlobalModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.InheritedSharingModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.OverrideModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.PrivateModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.ProtectedModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.PublicModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.StaticModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.TestMethodModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.TransientModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.VirtualModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.WebServiceModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.WithSharingModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Modifier.WithoutSharingModifier node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.NameValueParameter node) {
    addNode(type(node), null, () -> {
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.value).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.NewObject node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.NewObject.NewKeyValue node) {
    addNode(type(node), null, () -> {
      ofNullable(node.type).ifPresent(this::_case);
      ofNullable(node.keyValues).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewListInit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      node.expr.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewListLiteral node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.values).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewMapInit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      node.expr.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewMapLiteral node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.pairs).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewSetInit node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      node.expr.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewSetLiteral node) {
    addNode(type(node), null, () -> {
      ofNullable(node.types).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.values).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.NewObject.NewStandard node) {
    addNode(type(node), null, () -> {
      ofNullable(node.type).ifPresent(this::_case);
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.ParameterRef node) {
    addNode(type(node), null, () -> {
      ofNullable(node.getModifiers()).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.getName()).ifPresent(this::_case);
      ofNullable(node.getType()).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.PropertyDecl node) {
    addNode(type(node), null, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.type).ifPresent(this::_case);
      ofNullable(node.name).ifPresent(this::_case);
      node.getter.ifPresent(this::_case);
      node.setter.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.PropertyGetter node) {
    addNode(type(node), node.loc, () -> {
      node.modifier.ifPresent(this::_case);
      node.stmnt.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.PropertySetter node) {
    addNode(type(node), node.loc, () -> {
      node.modifier.ifPresent(this::_case);
      node.stmnt.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.Stmnt.BlockStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.stmnts).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.BreakStmnt node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.ContinueStmnt node) {
    addNode(type(node), node.loc, () -> {
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlDeleteStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlInsertStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlMergeStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr1).ifPresent(this::_case);
      ofNullable(node.expr2).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUndeleteStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUpdateStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DmlUpsertStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      node.id.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.DoLoop node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
      ofNullable(node.condition).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.ExpressionStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.ForLoop node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.forControl).ifPresent(this::_case);
      node.stmnt.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.IfElseBlock node) {
    addNode(type(node), null, () -> {
      ofNullable(node.ifBlocks).ifPresent(list -> list.forEach(this::_case));
      node.elseBlock.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.ReturnStmnt node) {
    addNode(type(node), node.loc, () -> {
      node.expr.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.RunAsBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.inputParameters).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.SwitchStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
      ofNullable(node.whenBlocks).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.ThrowStmnt node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.TryCatchFinallyBlock node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.tryBlock).ifPresent(this::_case);
      ofNullable(node.catchBlocks).ifPresent(list -> list.forEach(this::_case));
      node.finallyBlock.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.VariableDeclStmnt node) {
    addNode(type(node), null, () -> {
      ofNullable(node.variableDecls).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.Stmnt.WhileLoop node) {
    addNode(type(node), node.loc, () -> {
      ofNullable(node.condition).ifPresent(this::_case);
      node.stmnt.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.TypeRef node) {
    node.accept(this);
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.ArrayTypeRef node) {
    addNode(type(node), null, () -> {
      ofNullable(node.getTypeArguments()).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.getNames()).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.getHeldType()).ifPresent(this::_case);
    });
    return null;
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.ClassTypeRef node) {
    addNode(type(node), null, () -> {
      ofNullable(node.getTypeArguments()).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.getNames()).ifPresent(list -> list.forEach(this::_case));
    });
    return null;
  }

  public Void visit(apex.jorje.data.ast.TypeRefs.JavaTypeRef node) {
    addNode(type(node), null, () -> {
      ofNullable(node.getTypeArguments()).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.getNames()).ifPresent(list -> list.forEach(this::_case));
    });
    return null;
  }

  public void _case(apex.jorje.data.ast.VariableDecl node) {
    addNode(type(node), null, () -> {
      ofNullable(node.name).ifPresent(this::_case);
      node.assignment.ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.VariableDecls node) {
    addNode(type(node), null, () -> {
      ofNullable(node.modifiers).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.type).ifPresent(this::_case);
      ofNullable(node.decls).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.WhenBlock node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.WhenBlock.ElseWhen node) {
    addNode(type(node), null, () -> {
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.WhenBlock.TypeWhen node) {
    addNode(type(node), null, () -> {
      ofNullable(node.typeRef).ifPresent(this::_case);
      ofNullable(node.name).ifPresent(this::_case);
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.WhenBlock.ValueWhen node) {
    addNode(type(node), null, () -> {
      ofNullable(node.whenCases).ifPresent(list -> list.forEach(this::_case));
      ofNullable(node.stmnt).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.ast.WhenCase node) {
    node._switch(this);
  }

  public void _case(apex.jorje.data.ast.WhenCase.EnumCase node) {
    addNode(type(node), null, () -> {
      ofNullable(node.identifiers).ifPresent(list -> list.forEach(this::_case));
    });
  }

  public void _case(apex.jorje.data.ast.WhenCase.LiteralCase node) {
    addNode(type(node), null, () -> {
      ofNullable(node.expr).ifPresent(this::_case);
    });
  }

  public void _case(apex.jorje.data.soql.FieldIdentifier node) {
    addNode(type(node), null, () -> {
      node.entity.ifPresent(this::_case);
      ofNullable(node.field).ifPresent(this::_case);
    });
  }

}
