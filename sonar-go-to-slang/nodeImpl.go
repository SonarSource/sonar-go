// SonarQube Go Plugin
// Copyright (C) 2018-2025 SonarSource SA
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the Sonar Source-Available License for more details.
//
// You should have received a copy of the Sonar Source-Available License
// along with this program; if not, see https://sonarsource.com/license/ssal/

package main

import (
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"strconv"
)

const keywordField = "keyword"
const identifierField = "identifier"
const identifiersField = "identifiers"
const operatorField = "operator"
const operandField = "operand"
const conditionField = "condition"
const expressionField = "expression"
const expressionsField = "expressions"
const lParentKind = "Lparen"
const rParentKind = "Rparen"
const lBraceKind = "Lbrace"
const rBraceKind = "Rbrace"
const lBrackKind = "Lbrack"
const rBrackKind = "Rbrack"

func (t *SlangMapper) mapReturnStmtImpl(stmt *ast.ReturnStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})
	returnToken := t.createTokenFromPosAstToken(stmt.Return, token.RETURN, "Return")
	slangField[keywordField] = returnToken.Token.TextRange
	children = t.appendNode(children, returnToken)

	var returnBodyList []*Node
	for i := 0; i < len(stmt.Results); i++ {
		expr := t.mapExpr(stmt.Results[i], "["+strconv.Itoa(i)+"]")
		returnBodyList = append(returnBodyList, expr)
		children = t.appendNode(children, expr)
	}
	slangField[expressionsField] = returnBodyList

	return t.createNode(stmt, children, fieldName+"(ReturnStmt)", "Return", slangField)
}

func (t *SlangMapper) mapIdentImpl(ident *ast.Ident, fieldName string) *Node {
	slangField := make(map[string]interface{})
	var slangType string

	var children []*Node
	switch ident.Name {
	case "true", "false", "nil":
		slangType = "Literal"
		slangField["value"] = ident.Name
	case "_":
		slangType = "PlaceHolder"
		placeHolderToken := t.createExpectedToken(ident.NamePos, "_", "PlaceHolder", "KEYWORD")
		children = t.appendNode(children, placeHolderToken)
		slangField["placeHolderToken"] = placeHolderToken.TextRange
	default:
		slangType = "Identifier"
		slangField["name"] = ident.Name
		if t.info != nil {
			identifierInfo := t.getIdentifierInfo(ident)
			slangField["id"] = identifierInfo.Id
			slangField["type"] = identifierInfo.Type
			slangField["package"] = identifierInfo.Package
		}
	}

	return t.createNode(ident, children, fieldName+"(Ident)", slangType, slangField)
}

func (t *SlangMapper) mapFileImpl(file *ast.File, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})
	var declarations []*Node

	packageDecl := t.mapPackageDecl(file)
	children = t.appendNode(children, packageDecl)
	declarations = append(declarations, packageDecl)

	var nodeListDecls []*Node
	for i := 0; i < len(file.Decls); i++ {
		nodeListDecls = t.appendNode(nodeListDecls, t.mapDecl(file.Decls[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNodeList(children, nodeListDecls, "Decls([]Decl)")
	declarations = append(declarations, t.filterOutComments(nodeListDecls)...)

	slangField["declarations"] = declarations
	slangField["firstCpdToken"] = nil
	return t.createNode(file, children, fieldName, "TopLevel", slangField)
}

func (t *SlangMapper) mapDeclImpl(decl ast.Decl, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBadDeclImpl(decl *ast.BadDecl, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncDeclImpl(decl *ast.FuncDecl, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Type.Func, token.FUNC, "Type.Func"))

	receiver := t.mapFieldListReceiver(decl.Recv, "Recv")
	children = t.appendNode(children, receiver)
	slangField["receiver"] = receiver

	funcName := t.mapIdent(decl.Name, "Name")
	children = t.appendNode(children, funcName)
	slangField["name"] = funcName

	typeParams := t.mapFieldListTypeParams(decl.Type.TypeParams, "TypeParams")
	children = t.appendNode(children, typeParams)
	slangField["typeParameters"] = typeParams

	parameters := t.mapFieldListParams(decl.Type.Params, "Params")
	children = t.appendNode(children, parameters)
	formalParameters := t.getFormalParameter(parameters)
	slangField["formalParameters"] = formalParameters

	funcResults := t.mapFieldListResults(decl.Type.Results, "Results")
	children = t.appendNode(children, funcResults)
	slangField["returnType"] = funcResults

	funcBody := t.mapBlockStmt(decl.Body, "Body")
	children = t.appendNode(children, funcBody)
	slangField["body"] = funcBody

	slangField["cfg"] = t.extractCfg(decl.Body)
	t.reinitCfg()

	return t.createNode(decl, children, fieldName+"(FuncDecl)", "FunctionDeclaration", slangField)
}

func (t *SlangMapper) mapFuncLitImpl(lit *ast.FuncLit, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(lit.Type.Func, token.FUNC, "Type.Func"))

	parameters := t.mapFieldListParams(lit.Type.Params, "Params")
	children = t.appendNode(children, parameters)
	slangField["formalParameters"] = t.getFormalParameter(parameters)

	funcResults := t.mapFieldListResults(lit.Type.Results, "Results")
	children = t.appendNode(children, funcResults)
	slangField["returnType"] = funcResults

	typeParams := t.mapFieldListTypeParams(lit.Type.TypeParams, "TypeParams")
	children = t.appendNode(children, typeParams)
	slangField["typeParameters"] = typeParams

	funcBody := t.mapBlockStmt(lit.Body, "Body")
	children = t.appendNode(children, funcBody)
	slangField["body"] = funcBody

	//FuncLit does not have a receiver
	slangField["receiver"] = nil

	slangField["cfg"] = t.extractCfg(lit.Body)

	return t.createNode(lit, children, fieldName+"(FuncLit)", "FunctionDeclaration", slangField)
}

func (t *SlangMapper) getFormalParameter(node *Node) []*Node {
	var formalParameters []*Node
	//Get all FieldListParams lists
	childrenWithoutComment := t.filterOutComments(node.Children)
	for i := 1; i < len(childrenWithoutComment)-1; i++ {
		//Get all params inside this list (excluding comma)
		currentList := t.filterOutComments(childrenWithoutComment[i].Children)
		for j := 0; j < len(currentList); j = j + 2 {
			formalParameters = append(formalParameters, currentList[j])
		}
	}
	return formalParameters
}

func (t *SlangMapper) mapGenDeclImport(decl *ast.GenDecl, fieldName string) *Node {
	var children []*Node
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.TokPos, decl.Tok, "Tok"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Lparen, token.LPAREN, lParentKind))

	for i := 0; i < len(decl.Specs); i++ {
		children = t.appendNode(children, t.mapSpec(decl.Specs[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Rparen, token.RPAREN, rParentKind))

	slangField := make(map[string]interface{})
	slangField["children"] = t.filterOutComments(children)

	return t.createNode(decl, children, fieldName+"(ImportSpec)", "ImportDeclaration", slangField)
}

func (t *SlangMapper) mapGenDeclType(decl *ast.GenDecl, fieldName string) *Node {
	if len(decl.Specs) != 1 {
		//The node can not be mapped to a typed Slang node, create a native node
		return nil
	}

	spec, ok := decl.Specs[0].(*ast.TypeSpec)
	if !ok {
		// The spec of this declaration is not a TypeSpec, we map it to native
		return nil
	}

	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.TokPos, decl.Tok, "Tok"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Lparen, token.LPAREN, lParentKind))

	specName := t.mapIdent(spec.Name, "Name")
	children = t.appendNode(children, specName)
	slangField[identifierField] = specName.TextRange

	children = t.appendNode(children, t.mapFieldListTypeParams(spec.TypeParams, "TypeParams"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(spec.Assign, token.ASSIGN, "Assign"))
	children = t.appendNode(children, t.mapExpr(spec.Type, "Type"))

	//ClassTree in SLang contains everything (including identifier), we create a new node for this purpose
	classTree := t.createNativeNode(spec, children, fieldName+"(TypeSpecWrapped)")

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Rparen, token.RPAREN, rParentKind))

	slangField["classTree"] = classTree
	return t.createNode(spec, []*Node{classTree}, fieldName+"(TypeSpec)", "ClassDeclaration", slangField)
}

func (t *SlangMapper) mapGenDeclImpl(decl *ast.GenDecl, fieldName string) *Node {
	slangField := make(map[string]interface{})

	switch decl.Tok {
	case token.CONST:
		slangField["isVal"] = true
	case token.VAR:
		slangField["isVal"] = false
	case token.TYPE:
		if decl.Lparen == token.NoPos {
			// token type with parenthesis has no identifier, we map it to Native
			return t.mapGenDeclType(decl, fieldName)
		} else {
			return nil
		}
	case token.IMPORT:
		return t.mapGenDeclImport(decl, fieldName)
	}

	if len(decl.Specs) != 1 {
		//The node can not be mapped to a typed Slang node, create a native node
		return nil
	}

	valueSpec, ok := decl.Specs[0].(*ast.ValueSpec)
	if !ok {
		// The spec of this declaration is not a valueSpec, we map it to native
		return nil
	}

	var children []*Node
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.TokPos, decl.Tok, "Tok"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Lparen, token.LPAREN, lParentKind))

	var identifiers []*Node
	for i := 0; i < len(valueSpec.Names); i++ {
		identifier := t.mapIdent(valueSpec.Names[i], "["+strconv.Itoa(i)+"]")
		identifiers = append(identifiers, identifier)
		children = t.appendNode(children, identifier)
	}
	slangField[identifiersField] = identifiers

	typ := t.mapExpr(valueSpec.Type, "Type")
	children = t.appendNode(children, typ)
	slangField["type"] = typ

	var initializers []*Node

	for i := 0; i < len(valueSpec.Values); i++ {
		initializer := t.mapExpr(valueSpec.Values[i], "["+strconv.Itoa(i)+"]")
		initializers = append(initializers, initializer)
		children = t.appendNode(children, initializer)
	}

	slangField["initializers"] = initializers

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Rparen, token.RPAREN, rParentKind))

	return t.createNode(decl, children, fieldName+"(GenDecl)", "VariableDeclaration", slangField)
}

func (t *SlangMapper) mapFieldListParamsImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListResultsImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListReceiverImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListBraceImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncTypeImpl(funcType *ast.FuncType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListTypeParamsImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncTypeDeclImpl(funcType *ast.FuncType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBlockStmtImpl(blockStmt *ast.BlockStmt, fieldName string) *Node {
	var children []*Node
	children = t.appendNode(children, t.createTokenFromPosAstToken(blockStmt.Lbrace, token.LBRACE, lBraceKind))
	for i := 0; i < len(blockStmt.List); i++ {
		children = t.appendNode(children, t.mapStmt(blockStmt.List[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNode(children, t.createTokenFromPosAstToken(blockStmt.Rbrace, token.RBRACE, rBraceKind))

	slangField := make(map[string]interface{})

	// children without the braces
	slangField["statementOrExpressions"] = t.filterOutComments(children[1 : len(children)-1])

	return t.createNode(blockStmt, children, fieldName+"(BlockStmt)", "Block", slangField)
}

func (t *SlangMapper) mapSpecImpl(spec ast.Spec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldImpl(field *ast.Field, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldResultImpl(field *ast.Field, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldParamImpl(field *ast.Field, fieldName string) *Node {
	var children []*Node

	nNames := len(field.Names)

	if nNames <= 0 {
		return nil
	}
	//Go parameter can share the type with multiple identifier ex: f(a, b int)
	//We will create a parameter node without type for the firsts and with type for the last
	for i := 0; i < nNames-1; i++ {
		paramterIdent := t.mapIdent(field.Names[i], fieldName+"["+strconv.Itoa(i)+"]")
		parameter := t.createParameter(field.Names[i], paramterIdent, nil, fieldName)
		children = t.appendNode(children, parameter)
	}
	lastParameterIdent := t.mapIdent(field.Names[nNames-1], fieldName+"["+strconv.Itoa(nNames-1)+"]")
	lastParameterType := t.mapExpr(field.Type, "Type")

	lastParameter := t.createParameter(field.Names[nNames-1], lastParameterIdent, lastParameterType, fieldName)
	children = t.appendNode(children, lastParameter)

	return t.createNativeNode(field, children, fieldName+"(Field)")
}

func (t *SlangMapper) createParameter(ident *ast.Ident, parameterIdent, typ *Node, fieldName string) *Node {
	slangField := make(map[string]interface{})
	children := []*Node{parameterIdent}
	if typ != nil {
		children = t.appendNode(children, typ)
	}
	slangField[identifierField] = parameterIdent
	slangField["type"] = typ
	return t.createNode(ident, children, fieldName+"(Parameter)", "Parameter", slangField)
}

func (t *SlangMapper) mapStmtImpl(stmt ast.Stmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapImportSpecImpl(spec *ast.ImportSpec, fieldName string) *Node {
	slangField := make(map[string]interface{})
	var children []*Node

	name := t.mapIdent(spec.Name, "Name")
	children = t.appendNode(children, name)
	slangField["name"] = name

	path := t.mapBasicLit(spec.Path, "Path")
	children = t.appendNode(children, path)
	slangField["path"] = path

	return t.createNode(spec, children, fieldName+"(ImportSpec)", "ImportSpecification", slangField)
}

func (t *SlangMapper) mapTypeSpecImpl(spec *ast.TypeSpec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapValueSpecImpl(spec *ast.ValueSpec, fieldName string) *Node {
	// ValueSpec represents declaration, but they will be mapped inside mapGenDeclImpl to know if the node is a const or not.
	return nil
}

func (t *SlangMapper) mapExprImpl(expr ast.Expr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapAssignStmtImpl(stmt *ast.AssignStmt, fieldName string) *Node {
	if stmt.Tok == token.DEFINE {
		return t.createVariableDeclaration(stmt, fieldName)
	}

	operator := t.getOperatorForAssignStmt(stmt)

	if operator == nil {
		fmt.Errorf("nil operator for operator %s", stmt.Tok.String())
		return nil
	}

	var children []*Node

	leftHandSide := t.mapLeftRightHandSide(stmt.Lhs)
	children = t.appendNode(children, leftHandSide)

	children = t.appendNode(children, t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"))

	rightHandSide := t.mapLeftRightHandSide(stmt.Rhs)
	children = t.appendNode(children, rightHandSide)

	slangField := make(map[string]interface{})
	slangField[operatorField] = operator
	slangField["leftHandSide"] = leftHandSide
	slangField["statementOrExpression"] = rightHandSide
	return t.createNode(stmt, children, fieldName+"(AssignStmt)", "AssignmentExpression", slangField)
}

func (t *SlangMapper) getOperatorForAssignStmt(stmt *ast.AssignStmt) *string {
	var operator string
	switch stmt.Tok {
	case token.ASSIGN:
		operator = "EQUAL"
	case token.ADD_ASSIGN:
		operator = "PLUS_EQUAL"
	case token.SUB_ASSIGN:
		operator = "SUB_ASSIGN"
	case token.MUL_ASSIGN:
		operator = "TIMES_ASSIGN"
	case token.QUO_ASSIGN:
		operator = "DIVIDED_BY_ASSIGN"
	case token.REM_ASSIGN:
		operator = "MODULO_ASSIGN"
	case token.AND_ASSIGN:
		operator = "BITWISE_AND_ASSIGN"
	case token.OR_ASSIGN:
		operator = "BITWISE_OR_ASSIGN"
	case token.XOR_ASSIGN:
		operator = "BITWISE_XOR_ASSIGN"
	case token.SHL_ASSIGN:
		operator = "BITWISE_SHL_ASSIGN"
	case token.SHR_ASSIGN:
		operator = "BITWISE_SHR_ASSIGN"
	case token.AND_NOT_ASSIGN:
		operator = "BITWISE_AND_NOT_ASSIGN"
	default:
		return nil
	}
	return &operator
}

func (t *SlangMapper) mapLeftRightHandSide(exprs []ast.Expr) *Node {
	var handSide []*Node
	var handSideWrapper *Node
	if len(exprs) > 1 {
		for i := 0; i < len(exprs); i++ {
			nodeListLhs := t.mapExpr(exprs[i], "["+strconv.Itoa(i)+"]")
			handSide = t.appendNode(handSide, nodeListLhs)
		}
		slangField := make(map[string]interface{})
		slangField["children"] = handSide
		handSideWrapper = t.createNodeWithChildren(nil, handSide, "LeftRightHandSide", slangField)
	} else {
		handSideWrapper = t.mapExpr(exprs[0], "[0]")
	}
	return handSideWrapper
}

func (t *SlangMapper) createVariableDeclaration(stmt *ast.AssignStmt, fieldName string) *Node {
	slangField := make(map[string]interface{})
	var children []*Node

	slangField["isVal"] = false
	var identifiers []*Node
	for i := 0; i < len(stmt.Lhs); i++ {
		identifier := t.mapExpr(stmt.Lhs[i], "["+strconv.Itoa(i)+"]")
		identifiers = append(identifiers, identifier)
		children = t.appendNode(children, identifier)
	}
	slangField[identifiersField] = identifiers
	slangField["type"] = nil

	children = t.appendNode(children, t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"))

	var initializers []*Node
	for i := 0; i < len(stmt.Rhs); i++ {
		initializer := t.mapExpr(stmt.Rhs[i], "["+strconv.Itoa(i)+"]")
		initializers = append(initializers, initializer)
		children = t.appendNode(children, initializer)
	}
	slangField["initializers"] = initializers

	return t.createNode(stmt, children, fieldName+"(AssignDefineStmt)", "VariableDeclaration", slangField)
}

func (t *SlangMapper) mapBadStmtImpl(stmt *ast.BadStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBranchStmtImpl(stmt *ast.BranchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	var jumpKind string

	switch stmt.Tok {
	case token.BREAK:
		jumpKind = "BREAK"
	case token.CONTINUE:
		jumpKind = "CONTINUE"
	default:
		return nil
	}

	branchToken := t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"+jumpKind)
	children = t.appendNode(children, branchToken)
	slangField[keywordField] = branchToken.TextRange
	slangField["kind"] = jumpKind

	label := t.mapIdent(stmt.Label, "Label")
	children = t.appendNode(children, label)
	slangField["label"] = label

	return t.createNode(stmt, children, fieldName+"(BranchStmt)", "Jump", slangField)
}

func (t *SlangMapper) mapCaseClauseImpl(clause *ast.CaseClause, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.handleSwitchCase(clause.Case, len(clause.List) == 0, children)

	var clauseList []*Node
	for i := 0; i < len(clause.List); i++ {
		clauseList = t.appendNode(clauseList, t.mapExpr(clause.List[i], "["+strconv.Itoa(i)+"]"))
	}
	//SLang requires a tree as expression and not a list, we wrap it in a native node
	caseExpression := t.createNativeNodeWithChildren(clauseList, "CaseExprList")
	children = t.appendNode(children, caseExpression)
	slangField[expressionField] = caseExpression

	children = t.appendNode(children, t.createTokenFromPosAstToken(clause.Colon, token.COLON, "Colon"))

	var nodeListBody []*Node
	for i := 0; i < len(clause.Body); i++ {
		nodeListBody = t.appendNode(nodeListBody, t.mapStmt(clause.Body[i], "["+strconv.Itoa(i)+"]"))
	}

	//SLang requires a tree as body and not a list, we wrap it in a block
	nodeListBodyWithoutComment := t.filterOutComments(nodeListBody)
	var caseBody *Node

	if len(nodeListBodyWithoutComment) == 1 && nodeListBodyWithoutComment[0].SlangType == "Block" {
		caseBody = nodeListBodyWithoutComment[0]
	} else {
		slangFieldBlock := make(map[string]interface{})
		slangFieldBlock["statementOrExpressions"] = nodeListBodyWithoutComment
		caseBody = t.createNode(nil, nodeListBody, fieldName+"(BlockStmt)", "Block", slangFieldBlock)
	}

	children = t.appendNode(children, caseBody)
	slangField["body"] = caseBody

	return t.createNode(clause, children, fieldName+"(CaseClause)", "MatchCase", slangField)
}

func (t *SlangMapper) mapCommClauseImpl(clause *ast.CommClause, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapDeclStmtImpl(stmt *ast.DeclStmt, fieldName string) *Node {
	// In case of a GenDecl statement, we map the declaration itself to avoid an extra native node layer
	genDecl, ok := stmt.Decl.(*ast.GenDecl)
	if !ok {
		return nil
	}
	return t.mapGenDecl(genDecl, fieldName)
}

func (t *SlangMapper) mapDeferStmtImpl(stmt *ast.DeferStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapEmptyStmtImpl(stmt *ast.EmptyStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapExprStmtImpl(stmt *ast.ExprStmt, fieldName string) *Node {
	var children []*Node
	expr := t.mapExpr(stmt.X, "X")
	children = t.appendNode(children, expr)
	slangField := make(map[string]interface{})
	slangField["expression"] = expr
	return t.createNode(stmt, children, fieldName+"(ExprStmt)", "ExpressionStatement", slangField)
}

func (t *SlangMapper) mapForStmtImpl(stmt *ast.ForStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	hasInitOrPost := stmt.Init != nil || stmt.Post != nil

	forToken := t.createTokenFromPosAstToken(stmt.For, token.FOR, "For")
	children = t.appendNode(children, forToken)
	slangField[keywordField] = forToken.TextRange

	var condition *Node
	var kind string

	if !hasInitOrPost {
		condition = t.mapExpr(stmt.Cond, "Cond")
		children = t.appendNode(children, condition)
		kind = "WHILE"
	} else {
		var forHeaderList []*Node
		forHeaderList = t.appendNode(forHeaderList, t.mapStmt(stmt.Init, "Init"))
		forHeaderList = t.appendNode(forHeaderList, t.mapExpr(stmt.Cond, "Cond"))
		forHeaderList = t.appendNode(forHeaderList, t.mapStmt(stmt.Post, "Post"))

		//Wrap the 3 elements of the for loop header into one single node
		condition = t.createNativeNodeWithChildren(forHeaderList, "ForHeader")
		children = t.appendNode(children, condition)
		kind = "FOR"
	}
	slangField[conditionField] = condition
	slangField["kind"] = kind

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["body"] = body

	return t.createNode(stmt, children, fieldName+"(ForStmt)", "Loop", slangField)
}

func (t *SlangMapper) mapGoStmtImpl(stmt *ast.GoStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapIfStmtImpl(ifStmt *ast.IfStmt, fieldName string) *Node {
	var children []*Node
	ifToken := t.createTokenFromPosAstToken(ifStmt.If, token.IF, "If")
	children = t.appendNode(children, ifToken)

	var condition *Node
	if ifStmt.Init != nil {
		condition = t.createAdditionalInitAndCond(ifStmt.Init, ifStmt.Cond)
	} else {
		condition = t.mapExpr(ifStmt.Cond, "Cond")
	}

	children = t.appendNode(children, condition)

	thenBranch := t.mapBlockStmt(ifStmt.Body, "Body")
	children = t.appendNode(children, thenBranch)

	elseBranch := t.mapStmt(ifStmt.Else, "Else")
	children = t.appendNode(children, elseBranch)

	slangField := make(map[string]interface{})

	slangField["ifKeyword"] = ifToken.TextRange
	slangField[conditionField] = condition
	slangField["thenBranch"] = thenBranch
	slangField["elseKeyword"] = nil
	slangField["elseBranch"] = elseBranch

	childrenWithoutComments := t.filterOutComments(children)
	if elseBranch != nil {
		for i := len(childrenWithoutComments) - 1; i >= 0; i-- {
			if childrenWithoutComments[i] == elseBranch {
				// else keyword is necessarily before, and has been added to the children when calling "appendNode"
				slangField["elseKeyword"] = childrenWithoutComments[i-1].TextRange
				break
			}
		}
	}

	return t.createNode(ifStmt, children, fieldName+"(IfStmt)", "If", slangField)
}

func (t *SlangMapper) mapIncDecStmtImpl(stmt *ast.IncDecStmt, fieldName string) *Node {
	var operatorName = "DECREMENT"
	if token.INC == stmt.Tok {
		operatorName = "INCREMENT"
	}

	var children []*Node
	slangField := make(map[string]interface{})

	operand := t.mapExpr(stmt.X, "X")
	children = t.appendNode(children, operand)
	slangField[operandField] = operand

	operator := t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok")
	children = t.appendNode(children, operator)
	slangField[operatorField] = operatorName

	return t.createNode(stmt, children, fieldName+"(UnaryExpression)", "UnaryExpression", slangField)
}

func (t *SlangMapper) mapLabeledStmtImpl(stmt *ast.LabeledStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapRangeStmtImpl(stmt *ast.RangeStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	forToken := t.createTokenFromPosAstToken(stmt.For, token.FOR, "For")
	children = t.appendNode(children, forToken)
	slangField[keywordField] = forToken.TextRange

	var rangeHeaderList []*Node

	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.Key, "Key"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.Value, "Value"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.X, "X"))

	//Wrap all element of the range loop into one single node
	condition := t.createNativeNodeWithChildren(rangeHeaderList, "RangeHeader")
	children = t.appendNode(children, condition)
	slangField[conditionField] = condition

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["body"] = body

	slangField["kind"] = "FOR"

	return t.createNode(stmt, children, fieldName+"(RangeStmt)", "Loop", slangField)
}

func (t *SlangMapper) mapSelectStmtImpl(stmt *ast.SelectStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSendStmtImpl(stmt *ast.SendStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSwitchStmtImpl(stmt *ast.SwitchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	keywordToken := t.createTokenFromPosAstToken(stmt.Switch, token.SWITCH, "Switch")
	children = t.appendNode(children, keywordToken)
	slangField[keywordField] = keywordToken.TextRange

	var expressionList []*Node
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Init, "Init"))
	expressionList = t.appendNode(expressionList, t.mapExpr(stmt.Tag, "Tag"))

	//Wrap the tag and init into one native node
	expression := t.createNativeNodeWithChildren(expressionList, "InitAndTag")
	children = t.appendNode(children, expression)
	slangField[expressionField] = expression

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["cases"] = t.getMatchCases(body)

	return t.createNode(stmt, children, fieldName+"(SwitchStmt)", "Match", slangField)
}

func (t *SlangMapper) mapTypeSwitchStmtImpl(stmt *ast.TypeSwitchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	keywordToken := t.createTokenFromPosAstToken(stmt.Switch, token.SWITCH, "Switch")
	children = t.appendNode(children, keywordToken)
	slangField[keywordField] = keywordToken.TextRange

	var expressionList []*Node
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Init, "Init"))
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Assign, "Assign"))

	//Wrap the init and Assign into one native node
	expression := t.createNativeNodeWithChildren(expressionList, "InitAndAssign")
	children = t.appendNode(children, expression)
	slangField[expressionField] = expression

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["cases"] = t.getMatchCases(body)

	return t.createNode(stmt, children, fieldName+"(TypeSwitchStmt)", "Match", slangField)
}

func (t *SlangMapper) getMatchCases(node *Node) []*Node {
	bodyWithoutComment := t.filterOutComments(node.Children)
	var matchCases []*Node
	for _, child := range bodyWithoutComment {
		if child.SlangType == "MatchCase" {
			matchCases = append(matchCases, child)
		}
	}
	return matchCases
}

func (t *SlangMapper) mapArrayTypeImpl(arrayType *ast.ArrayType, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(arrayType.Lbrack, token.LBRACK, lBrackKind))
	if arrayType.Len != nil {
		length := t.mapExpr(arrayType.Len, "Len")
		slangField["length"] = length
		children = t.appendNode(children, length)
	} else {
		slangField["length"] = nil
	}

	element := t.mapExpr(arrayType.Elt, "Elt")
	slangField["element"] = element
	children = t.appendNode(children, element)

	return t.createNode(arrayType, children, fieldName+"(ArrayType)", "ArrayType", slangField)
}

func (t *SlangMapper) mapBadExprImpl(expr *ast.BadExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBinaryExprImpl(expr *ast.BinaryExpr, fieldName string) *Node {

	var operatorName = ""
	switch expr.Op {
	case token.ADD:
		operatorName = "PLUS"
	case token.SUB:
		operatorName = "MINUS"
	case token.MUL:
		operatorName = "TIMES"
	case token.QUO:
		operatorName = "DIVIDED_BY"
	case token.EQL:
		operatorName = "EQUAL_TO"
	case token.NEQ:
		operatorName = "NOT_EQUAL_TO"
	case token.GTR:
		operatorName = "GREATER_THAN"
	case token.GEQ:
		operatorName = "GREATER_THAN_OR_EQUAL_TO"
	case token.LSS:
		operatorName = "LESS_THAN"
	case token.LEQ:
		operatorName = "LESS_THAN_OR_EQUAL_TO"
	case token.LAND:
		operatorName = "CONDITIONAL_AND"
	case token.LOR:
		operatorName = "CONDITIONAL_OR"
	case token.AND:
		operatorName = "BITWISE_AND"
	case token.OR:
		operatorName = "BITWISE_OR"
	case token.XOR:
		operatorName = "BITWISE_XOR"
	case token.SHL:
		operatorName = "BITWISE_SHL"
	case token.SHR:
		operatorName = "BITWISE_SHR"
	case token.AND_NOT:
		operatorName = "BITWISE_AND_NOT"
	default:
		// all the other binary operators are not mapped
		return nil
	}

	var children []*Node
	slangField := make(map[string]interface{})

	leftOperand := t.mapExpr(expr.X, operandField)
	children = t.appendNode(children, leftOperand)
	slangField["leftOperand"] = leftOperand

	operator := t.createTokenFromPosAstToken(expr.OpPos, expr.Op, "Op")
	children = t.appendNode(children, operator)
	slangField[operatorField] = operatorName
	slangField["operatorToken"] = operator.TextRange

	rightOperand := t.mapExpr(expr.Y, operandField)
	children = t.appendNode(children, rightOperand)
	slangField["rightOperand"] = rightOperand

	return t.createNode(expr, children, fieldName+"(BinaryExpr)", "BinaryExpression", slangField)
}

func (t *SlangMapper) mapCallExprImpl(expr *ast.CallExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	functionExpression := t.mapExpr(expr.Fun, "Fun")
	children = t.appendNode(children, functionExpression)
	slangField["memberSelect"] = functionExpression
	slangField["returnType"] = t.findReturnTypes(expr)

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Lparen, token.LPAREN, lParentKind))

	var matchCases []*Node
	for i := 0; i < len(expr.Args); i++ {
		argument := t.mapExpr(expr.Args[i], "["+strconv.Itoa(i)+"]")
		// We do not call appendNode here because it would add the missing token (comma) between arguments.
		matchCases = append(matchCases, argument)
		children = t.appendNode(children, argument)
	}
	slangField["arguments"] = t.filterOutComments(matchCases)

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Ellipsis, token.ELLIPSIS, "Ellipsis"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Rparen, token.RPAREN, lParentKind))

	return t.createNode(expr, children, fieldName+"(CallExpr)", "FunctionInvocation", slangField)
}

func (t *SlangMapper) findReturnTypes(expr *ast.CallExpr) []string {
	result := make([]string, 0)
	identifier, ok := expr.Fun.(*ast.Ident)
	if ok {
		return t.findReturnTypesForIdentifier(identifier)
	}
	selectorExpr, ok := expr.Fun.(*ast.SelectorExpr)
	if ok {
		return t.findReturnTypesForIdentifier(selectorExpr.Sel)
	}
	return result
}

func (t *SlangMapper) findReturnTypesForIdentifier(identifier *ast.Ident) []string {
	result := make([]string, 0)
	for ident, obj := range t.info.Uses {
		if ident.NamePos == identifier.NamePos {
			signature, ok := obj.Type().(*types.Signature)
			if ok {
				result = make([]string, signature.Results().Len())
				for i := 0; i < signature.Results().Len(); i++ {
					result[i] = signature.Results().At(i).Origin().Type().String()
				}
			}
		}
	}
	return result
}

func (t *SlangMapper) mapChanTypeImpl(chanType *ast.ChanType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapCompositeLitImpl(lit *ast.CompositeLit, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	typeNode := t.mapExpr(lit.Type, "Type")
	children = t.appendNode(children, typeNode)
	slangField["type"] = typeNode

	children = t.appendNode(children, t.createTokenFromPosAstToken(lit.Lbrace, token.LBRACE, lBraceKind))
	var elements []*Node
	for i := 0; i < len(lit.Elts); i++ {
		element := t.mapExpr(lit.Elts[i], "["+strconv.Itoa(i)+"]")
		elements = append(elements, element)
		children = t.appendNode(children, element)
	}
	slangField["elements"] = t.filterOutComments(elements)

	children = t.appendNode(children, t.createTokenFromPosAstToken(lit.Rbrace, token.RBRACE, rBraceKind))

	return t.createNode(lit, children, fieldName+"(CompositeLit)", "CompositeLiteral", slangField)
}

func (t *SlangMapper) mapEllipsisImpl(ellipsis *ast.Ellipsis, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	elipsisToken := t.createTokenFromPosAstToken(ellipsis.Ellipsis, token.ELLIPSIS, "Ellipsis")
	children = t.appendNode(children, elipsisToken)
	slangField["ellipsis"] = elipsisToken.TextRange

	nestedExpr := t.mapExpr(ellipsis.Elt, "Elt")
	children = t.appendNode(children, nestedExpr)
	slangField["element"] = nestedExpr

	return t.createNode(ellipsis, children, fieldName+"(Ellipsis)", "Ellipsis", slangField)
}

func (t *SlangMapper) mapIndexExprImpl(expr *ast.IndexExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	expression := t.mapExpr(expr.X, "X")
	slangField["expression"] = expression
	children = t.appendNode(children, expression)

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Lbrack, token.LBRACK, "Lbrack"))

	index := t.mapExpr(expr.Index, "Index")
	slangField["index"] = index
	children = t.appendNode(children, index)

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Rbrack, token.RBRACK, "Rbrack"))

	return t.createNode(expr, children, fieldName+"(IndexExpr)", "IndexExpression", slangField)
}

func (t *SlangMapper) mapIndexListExprImpl(astNode *ast.IndexListExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	expression := t.mapExpr(astNode.X, "X")
	slangField["expression"] = expression
	children = t.appendNode(children, expression)

	children = t.appendNode(children, t.createTokenFromPosAstToken(astNode.Lbrack, token.LBRACK, "Lbrack"))

	var indices []*Node
	for i := 0; i < len(astNode.Indices); i++ {
		indice := t.mapExpr(astNode.Indices[i], "["+strconv.Itoa(i)+"]")
		indices = append(indices, indice)
		children = t.appendNode(children, indice)
	}
	slangField["indices"] = t.filterOutComments(indices)

	children = t.appendNode(children, t.createTokenFromPosAstToken(astNode.Rbrack, token.RBRACK, "Rbrack"))

	return t.createNode(astNode, children, fieldName+"(IndexListExpr)", "IndexListExpression", slangField)
}

func (t *SlangMapper) mapInterfaceTypeImpl(interfaceType *ast.InterfaceType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapKeyValueExprImpl(expr *ast.KeyValueExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	key := t.mapExpr(expr.Key, "Key")
	slangField["key"] = key
	children = t.appendNode(children, key)

	colon := t.createTokenFromPosAstToken(expr.Colon, token.COLON, "colon")
	children = t.appendNode(children, colon)

	value := t.mapExpr(expr.Value, "Value")
	slangField["value"] = value
	children = t.appendNode(children, value)

	return t.createNode(expr, children, fieldName+"(KeyValueExpr)", "KeyValue", slangField)
}

func (t *SlangMapper) mapMapTypeImpl(mapType *ast.MapType, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(mapType.Map, token.MAP, "Map"))

	key := t.mapExpr(mapType.Key, "Key")
	children = t.appendNode(children, key)
	slangField["key"] = key

	value := t.mapExpr(mapType.Value, "Value")
	children = t.appendNode(children, value)
	slangField["value"] = value

	return t.createNode(mapType, children, fieldName+"(MapType)", "MapType", slangField)
}

func (t *SlangMapper) mapParenExprImpl(expr *ast.ParenExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	leftParen := t.createTokenFromPosAstToken(expr.Lparen, token.LPAREN, lParentKind)
	slangField["leftParenthesis"] = leftParen.TextRange
	children = t.appendNode(children, leftParen)

	nestedExpr := t.mapExpr(expr.X, "X")
	slangField[expressionField] = nestedExpr
	children = t.appendNode(children, nestedExpr)

	rightParen := t.createTokenFromPosAstToken(expr.Rparen, token.RPAREN, rParentKind)
	children = t.appendNode(children, rightParen)
	slangField["rightParenthesis"] = rightParen.TextRange

	return t.createNode(expr, children, fieldName+"(ParenExpr)", "ParenthesizedExpression", slangField)
}

func (t *SlangMapper) mapSelectorExprImpl(expr *ast.SelectorExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	expression := t.mapExpr(expr.X, "X")
	children = t.appendNode(children, expression)
	slangField["expression"] = expression

	identifier := t.mapIdent(expr.Sel, "Sel")
	children = t.appendNode(children, identifier)
	slangField["identifier"] = identifier

	return t.createNode(expr, children, fieldName+"(SelectorExpr)", "MemberSelect", slangField)
}

func (t *SlangMapper) mapSliceExprImpl(expr *ast.SliceExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	xNode := t.mapExpr(expr.X, "X")
	children = t.appendNode(children, xNode)
	slangField[expressionField] = xNode

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Lbrack, token.LBRACK, lBrackKind))

	lowNode := t.mapExpr(expr.Low, "Low")
	children = t.appendNode(children, lowNode)
	slangField["low"] = lowNode

	highNode := t.mapExpr(expr.High, "High")
	children = t.appendNode(children, highNode)
	slangField["high"] = highNode

	maxNode := t.mapExpr(expr.Max, "Max")
	children = t.appendNode(children, maxNode)
	slangField["max"] = maxNode

	children = t.appendNode(children, t.createTokenFromPosAstToken(expr.Rbrack, token.RBRACK, rBrackKind))

	slangField["slice3"] = expr.Slice3

	return t.createNode(expr, children, fieldName+"(SliceExpr)", "Slice", slangField)
}

func (t *SlangMapper) mapStarExprImpl(expr *ast.StarExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	star := t.createTokenFromPosAstToken(expr.Star, token.MUL, "Star")
	children = t.appendNode(children, star)

	expression := t.mapExpr(expr.X, "X")
	children = t.appendNode(children, expression)
	slangField["expression"] = expression

	return t.createNode(expr, children, fieldName+"(StarExpr)", "StarExpression", slangField)
}

func (t *SlangMapper) mapStructTypeImpl(structType *ast.StructType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapTypeAssertExprImpl(typeExpr *ast.TypeAssertExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	expression := t.mapExpr(typeExpr.X, "X")
	children = t.appendNode(children, expression)
	slangField["expression"] = expression

	children = t.appendNode(children, t.createTokenFromPosAstToken(typeExpr.Lparen, token.LPAREN, "Lparen"))

	typePart := t.mapExpr(typeExpr.Type, "Type")
	children = t.appendNode(children, typePart)
	slangField["type"] = typePart

	children = t.appendNode(children, t.createTokenFromPosAstToken(typeExpr.Rparen, token.RPAREN, "Rparen"))

	return t.createNode(typeExpr, children, fieldName+"(TypeAssertExpr)", "TypeAssertionExpression", slangField)
}

func (t *SlangMapper) mapUnaryExprImpl(expr *ast.UnaryExpr, fieldName string) *Node {
	var operatorName = ""
	switch expr.Op {
	case token.ADD:
		operatorName = "PLUS"
	case token.SUB:
		operatorName = "MINUS"
	case token.NOT:
		operatorName = "NEGATE"
	case token.AND:
		operatorName = "ADDRESS_OF"
	case token.ARROW:
		operatorName = "ARROW"
	case token.XOR:
		operatorName = "BITWISE_COMPLEMENT"
	default:
		// token.Mul (*) will be handle as a StarExpr.
		// According to the documentation (https://go.dev/ref/spec#Operators), we cover all the possible unary expressions.
		// While this should not be reachable, we keep this for defensive programming.
		return nil
	}

	var children []*Node
	slangField := make(map[string]interface{})

	operator := t.createTokenFromPosAstToken(expr.OpPos, expr.Op, "Op")
	children = t.appendNode(children, operator)
	slangField[operatorField] = operatorName

	operand := t.mapExpr(expr.X, "X")
	children = t.appendNode(children, operand)
	slangField[operandField] = operand

	return t.createNode(expr, children, fieldName+"(UnaryExpression)", "UnaryExpression", slangField)
}
