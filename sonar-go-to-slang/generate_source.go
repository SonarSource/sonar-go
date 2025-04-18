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

// The following directive is necessary to make the package coherent:
//go:build ignore
// +build ignore

// This program generates 'goparser_generated.go'. It can be invoked by running "go generate"
package main

import (
	"bytes"
	"go/ast"
	"io/ioutil"
	"reflect"
)

func main() {
	var out bytes.Buffer

	out.WriteString(`// Code generated by 'generate_source.go' using 'go run generate_source.go'; DO NOT EDIT.
package main

import (
	"go/ast"
	"go/token"
	"strconv"
)
`)

	context := AstContext{
		Out: &out,
		ArrayFieldCreatingNode: map[string]bool{
			// By default when a field is an array, an intermediate node is not created to store the array elements.
			// Array elements are appended directly to the parent. And it's not possible to define "kinds" like
			// "CaseClause#List" because there's no matching node, but only "CaseClause#List[i]".
			// But adding an entry below, change the default behavior and create an intermediate node, and this node
			// support "kinds" defined in "KindsPerName".
			"File#Decls":        true,
			"GenDecl#Specs":     true,
			"Field#Names":       true,
			"FieldResult#Names": true,
			"FieldParam#Names":  true,
			"ValueSpec#Names":   true,
			"ValueSpec#Values":  true,
			"AssignStmt#Lhs":    true,
			"AssignStmt#Rhs":    true,
			"CaseClause#Body":   true,
			"CommClause#Body":   true,
			"CallExpr#Args":     true,
			"CompositeLit#Elts": true,
		},
		InsertBeforeField: map[string]string{
			// Additional code can be placed before the mapping of the referenced field
			"EmptyStmt#Semicolon": "if astNode.Implicit {\n\t\treturn nil\n\t}",
			"FuncDecl#Recv": "children = t.appendNode(children, " +
				"t.createTokenFromPosAstToken(astNode.Type.Func, token.FUNC, \"Type.Func\"))",
		},
		OverrideField: map[string]string{
			// The mapping of each field can be replaced by some custom code. Put function definitions in 'goparser.go'
			"File#Package": "children = t.appendNode(children, t.mapPackageDecl(astNode))",
			"File#Name":    "",
			// unknown "case" or "default"
			"CaseClause#Case": "children = t.handleSwitchCase(astNode.Case, len(astNode.List) == 0, children)",
			// FIXME this is wrong as it creates DEFAULT_CASE in the body of SELECT
			"CommClause#Case": "children = t.handleSwitchCase(astNode.Case, astNode.Comm == nil, children)",
			"IfStmt#Init":     "",
			"IfStmt#Cond": "children = t.appendNode(children, " +
				"t.createAdditionalInitAndCond(astNode.Init, astNode.Cond))",
			"File#Imports":      "",
			"File#Unresolved":   "",
			"ChanType#Begin":    "",
			"ChanType#Arrow":    "",
			"ChanType#Dir":      "",
			"ImportSpec#EndPos": "",
			"FuncTypeDecl#Func": "",
		},
		ForceLeafNode: map[string]bool{
			// Fields from the given struct types are ignored, this produce terminal node with token
			"Ident":   true,
			"BadStmt": true,
			"BadDecl": true,
			"BadExpr": true,
		},
		FieldToIgnore: map[string]bool{
			"File#FileStart":  true,
			"File#FileEnd":    true,
			"File#GoVersion":  true,
			"RangeStmt#Range": true,
		},
		TypeToIgnore: map[string]bool{
			// All fields of ast.* structs with the following types are ignored
			"*ast.CommentGroup":      true,
			"*ast.Object":            true,
			"*ast.Scope":             true,
			"bool":                   true,
			"[]*ast.CommentGroup":    true,
			"map[string]*ast.File":   true,
			"map[string]*ast.Object": true,
		},
		TokenFieldWithPos: map[string]bool{
			// There's a common pattern in the Go ast where 2 fields define one terminal token.
			// The first field is a "token.Pos" and the second a "token.Token" with the same name without "Pos" suffix.
			// Reference below the "token.Pos" field, and the "token.Token" field will be associated to produce one
			// token.
			"GenDecl#TokPos":    true,
			"AssignStmt#TokPos": true,
			"BranchStmt#TokPos": true,
			"IncDecStmt#TokPos": true,
			"RangeStmt#TokPos":  true,
			"BinaryExpr#OpPos":  true,
			"UnaryExpr#OpPos":   true,
		},
		StructVariations: map[string][]string{
			// By default one mapping function is generated for each ast.* struct, e.g. "mapFile" for "ast.File"
			// But if the caller of the mapping function needs a specific behavior (like specific: uast kinds,
			// fields override, token value, ...) then several mapping functions can be generated using the
			// given suffixes. For example providing the suffix "Result" for the type "Field" will generate
			// the mapping function "mapFieldResult". And an alias of "Field" called "FieldResult" can be used
			// to customize this mapping function. For example a field can be referenced by "FieldResult#Names"

			// variations to distinguish delimiters '('/'{' and kinds PARAMETER/RESULT/...
			"FieldList": []string{"Params", "Results", "Brace", "Receiver", "TypeParams"},

			// variations for kinds PARAMETER/RESULT
			"Field": []string{"", "Result", "Param"},

			// "Decl" variation ignore "func" keyword and is called by FuncDecl, because FuncDecl already map the
			// field FuncDecl.Type.Func.
			// The default variation called from FuncLit or Expr has no specificity.
			"FuncType": []string{"", "Decl"},
		},
		FieldVariationMap: map[string]string{
			// Add a suffix to the mapping function of the given field. (suffixes are defined in StructVariations)
			// For example, the field "StructType#Fields" has a type "ast.FieldList", so by default the mapping
			// function is "mapFieldList". But by defining a suffix "Brace", then the mapping function will be
			// "mapFieldListBrace".
			"StructType#Fields":        "Brace",
			"InterfaceType#Methods":    "Brace",
			"FuncType#Params":          "Params",
			"FuncType#Results":         "Results",
			"FuncType#TypeParams":      "TypeParams",
			"FuncTypeDecl#Params":      "Params",
			"FuncTypeDecl#Results":     "Results",
			"FuncTypeDecl#TypeParams":  "TypeParams",
			"FuncDecl#Type":            "Decl",
			"FuncDecl#Recv":            "Receiver",
			"FieldListParams#List[i]":  "Param",
			"FieldListResults#List[i]": "Result",
			"Field#Tag":                "Tag",
			"FieldResult#Tag":          "Tag",
			"FieldParam#Tag":           "Tag",
			"TypeSpec#TypeParams":      "TypeParams",
		},
		MatchingTokenPos: map[string]string{
			// Some ast.* struct fields with type "token.Pos" has no "token.Token" fields to specify their string
			// value. The below list do the mapping. A field can be referenced just by "<field name>" (like "Lbrace")
			// and will apply to all struct containing such field. Or by "<type name>#<field name>" like "IfStmt#If".
			// Or by "<type name><variation>#<field name>" like "FieldListParams#Opening".
			"Lbrace":                      "token.LBRACE",
			"Rbrace":                      "token.RBRACE",
			"Lbrack":                      "token.LBRACK",
			"Rbrack":                      "token.RBRACK",
			"Lparen":                      "token.LPAREN",
			"Rparen":                      "token.RPAREN",
			"Colon":                       "token.COLON",
			"Semicolon":                   "token.SEMICOLON",
			"Star":                        "token.MUL",
			"TypeSpec#Assign":             "token.ASSIGN",
			"FieldListParams#Opening":     "token.LPAREN",
			"FieldListParams#Closing":     "token.RPAREN",
			"FieldListResults#Opening":    "token.LPAREN",
			"FieldListResults#Closing":    "token.RPAREN",
			"FieldListBrace#Opening":      "token.LBRACE",
			"FieldListBrace#Closing":      "token.RBRACE",
			"FieldListReceiver#Opening":   "token.LPAREN",
			"FieldListReceiver#Closing":   "token.RPAREN",
			"FieldListTypeParams#Opening": "token.LBRACK",
			"FieldListTypeParams#Closing": "token.RBRACK",
			"GoStmt#Go":                   "token.GO",
			"IfStmt#If":                   "token.IF",
			"SendStmt#Arrow":              "token.ARROW",
			"Ellipsis":                    "token.ELLIPSIS",
			"ForStmt#For":                 "token.FOR",
			"RangeStmt#For":               "token.FOR",
			"MapType#Map":                 "token.MAP",
			"FuncType#Func":               "token.FUNC",
			"DeferStmt#Defer":             "token.DEFER",
			"ReturnStmt#Return":           "token.RETURN",
			"SelectStmt#Select":           "token.SELECT",
			"SwitchStmt#Switch":           "token.SWITCH",
			"TypeSwitchStmt#Switch":       "token.SWITCH",
			"StructType#Struct":           "token.STRUCT",
			"InterfaceType#Interface":     "token.INTERFACE",
		},
		TypeQueue: []reflect.Type{
			// This queue is used to generate all the ast.* struct types. The generation is initiated by pushing
			// the ast.File type. Other types will be discovered by reflection.
			typeOf((*ast.File)(nil)),
		},
		TypeProcessed: map[reflect.Type]bool{
			//Node already implemented inside goparser.go
			typeOf((*ast.BasicLit)(nil)): true,
		},
		AllAstStruct: typeOfList(
			// "Go" does not provide a way to enumerate struct types that inherit from a given interface.
			// But filtering a list of struct types using "struct.Implements(interface)" method is possible.
			// Generated by: grep 'struct {' go/binary/1.10/go/src/go/ast/ast.go | sed -r 's/^\t*(type )*([^ ]+).*/(*ast.\2)(nil),/' | sort
			(*ast.ArrayType)(nil), (*ast.AssignStmt)(nil), (*ast.BadDecl)(nil),
			(*ast.BadExpr)(nil), (*ast.BadStmt)(nil), (*ast.BasicLit)(nil), (*ast.BinaryExpr)(nil),
			(*ast.BlockStmt)(nil), (*ast.BranchStmt)(nil), (*ast.CallExpr)(nil), (*ast.CaseClause)(nil),
			(*ast.ChanType)(nil), (*ast.CommClause)(nil), (*ast.CommentGroup)(nil), (*ast.Comment)(nil),
			(*ast.CompositeLit)(nil), (*ast.DeclStmt)(nil), (*ast.DeferStmt)(nil), (*ast.Ellipsis)(nil),
			(*ast.EmptyStmt)(nil), (*ast.ExprStmt)(nil), (*ast.FieldList)(nil), (*ast.Field)(nil), (*ast.File)(nil),
			(*ast.ForStmt)(nil), (*ast.FuncDecl)(nil), (*ast.FuncLit)(nil), (*ast.FuncType)(nil), (*ast.GenDecl)(nil),
			(*ast.GoStmt)(nil), (*ast.Ident)(nil), (*ast.IfStmt)(nil), (*ast.ImportSpec)(nil), (*ast.IncDecStmt)(nil),
			(*ast.IndexExpr)(nil), (*ast.IndexListExpr)(nil), (*ast.InterfaceType)(nil), (*ast.KeyValueExpr)(nil), (*ast.LabeledStmt)(nil),
			(*ast.MapType)(nil), (*ast.Package)(nil), (*ast.ParenExpr)(nil), (*ast.RangeStmt)(nil),
			(*ast.ReturnStmt)(nil), (*ast.SelectorExpr)(nil), (*ast.SelectStmt)(nil), (*ast.SendStmt)(nil),
			(*ast.SliceExpr)(nil), (*ast.StarExpr)(nil), (*ast.StructType)(nil), (*ast.SwitchStmt)(nil),
			(*ast.TypeAssertExpr)(nil), (*ast.TypeSpec)(nil), (*ast.TypeSwitchStmt)(nil), (*ast.UnaryExpr)(nil),
			(*ast.ValueSpec)(nil),
		),
	}
	context.execute()

	err := ioutil.WriteFile("goparser_generated.go", out.Bytes(), 0644)
	if err != nil {
		panic(err)
	}
}

func typeOf(pointer interface{}) reflect.Type {
	return reflect.TypeOf(pointer).Elem()
}

func typeOfList(pointerList ...interface{}) []reflect.Type {
	types := make([]reflect.Type, len(pointerList))
	for i, structPointer := range pointerList {
		types[i] = reflect.TypeOf(structPointer).Elem()
	}
	return types
}

type AstContext struct {
	Out                    *bytes.Buffer
	TypeToIgnore           map[string]bool
	ForceLeafNode          map[string]bool
	FieldToIgnore          map[string]bool
	TokenFieldWithPos      map[string]bool
	MatchingTokenPos       map[string]string
	ArrayFieldCreatingNode map[string]bool
	KindsPerTypeException  map[reflect.Type]reflect.Type
	KindsPerName           map[string]string
	InsertBeforeField      map[string]string
	OverrideField          map[string]string
	StructVariations       map[string][]string
	FieldVariationMap      map[string]string
	TypeQueue              []reflect.Type
	TypeProcessed          map[reflect.Type]bool
	AllAstStruct           []reflect.Type
}

func (t *AstContext) execute() {
	for len(t.TypeQueue) > 0 {
		nextType := t.TypeQueue[0]
		t.TypeQueue = t.TypeQueue[1:]
		t.TypeProcessed[nextType] = true
		t.visitType(nextType)
	}
}

func (t *AstContext) pushType(structType reflect.Type) {
	if !t.TypeProcessed[structType] && !t.TypeToIgnore[structType.String()] {
		t.TypeProcessed[structType] = true
		t.TypeQueue = append(t.TypeQueue, structType)
	}
}

func (t *AstContext) writeLn(text string) {
	t.Out.WriteString(text)
	t.Out.WriteString("\n")
}

func (t *AstContext) visitType(nextType reflect.Type) {
	switch nextType.Kind() {
	case reflect.Interface:
		t.visitInterfaceType(nextType)
	case reflect.Struct:
		t.visitStructType(nextType)
	default:
		panic("Unsupported Kind " + nextType.Kind().String() + " for type " + nextType.String())
	}
}

func (t *AstContext) visitInterfaceType(interfaceType reflect.Type) {
	if interfaceType.Kind() != reflect.Interface {
		panic("Expect a Interface")
	}
	t.writeLn("")
	methodName := "map" + interfaceType.Name()
	arguments := "astNode " + interfaceType.String() + ", nativeNode string"
	t.writeLn("func (t *SlangMapper) " + methodName + "(" + arguments + ") *Node {")
	t.writeLn("\tswitch node := astNode.(type) {")
	for _, astStruct := range t.getStructTypesThatImplement(interfaceType) {
		if !t.TypeToIgnore[astStruct.String()] {
			t.writeLn("\tcase *" + astStruct.String() + ":")
			t.writeLn("\t\treturn t.map" + astStruct.Name() + "(node, nativeNode)")
			t.pushType(astStruct)
		} else {
			t.writeLn("\t// ignore " + astStruct.String() + " intentionally")
		}
	}
	t.writeLn("\tdefault:")
	t.writeLn("\t\treturn nil")
	t.writeLn("\t}")
	t.writeLn("}")
}

func (t *AstContext) visitStructType(structType reflect.Type) {
	if structType.Kind() != reflect.Struct {
		panic("Expect a Struct")
	}
	variations := t.StructVariations[structType.Name()]
	if len(variations) == 0 {
		variations = []string{""}
	}
	for _, variation := range variations {
		t.writeLn("")
		methodName := "map" + structType.Name() + variation
		arguments := "astNode *" + structType.String() + ", fieldName string"
		t.writeLn("func (t *SlangMapper) " + methodName + "(" + arguments + ") *Node {")
		t.writeLn("\tif astNode == nil {")
		t.writeLn("\t\treturn nil")
		t.writeLn("\t}")
		t.writeLn("\tnodeImpl := t." + methodName + "Impl(astNode, fieldName)")
		t.writeLn("\tif nodeImpl != nil {")
		t.writeLn("\t\treturn nodeImpl")
		t.writeLn("\t}")
		t.writeLn("\tvar children []*Node")
		if !t.ForceLeafNode[structType.Name()] {
			for i := 0; i < structType.NumField(); i++ {
				field := structType.Field(i)
				fullName := structType.Name() + variation + "#" + field.Name
				t.visitField(fullName, structType, field)
			}
		}
		arguments = "astNode, children, fieldName + \"(" + structType.Name() + ")\""
		t.writeLn("\treturn t.createNativeNode(" + arguments + ")")
		t.writeLn("}")
	}
}

func (t *AstContext) visitField(fullName string, structType reflect.Type, field reflect.StructField) {
	codeBefore := t.InsertBeforeField[fullName]
	if len(codeBefore) > 0 {
		t.writeLn("\t" + codeBefore)
	}

	overrideCode, isOverridden := t.OverrideField[fullName]
	if isOverridden && len(overrideCode) > 0 {
		t.writeLn("\t" + overrideCode)
	}

	fieldType := field.Type

	if !t.TypeToIgnore[fieldType.String()] && !t.FieldToIgnore[fullName] {
		if !isOverridden && !t.TypeToIgnore[fieldType.String()] && !t.FieldToIgnore[fullName] {
			switch fieldType.Kind() {
			case reflect.Ptr, reflect.Struct, reflect.Interface:
				t.visitStructField(fullName, field.Name, fieldType)
			case reflect.Slice:
				t.visitSliceField(fullName, field.Name, fieldType)
			case reflect.Int:
				t.visitIntField(fullName, field, fieldType)
			default:
				panic("Unsupported Kind " + fieldType.Kind().String() + " for type " + fieldType.String())
			}
		}
	}
}

func (t *AstContext) visitStructField(fullName, name string, fieldType reflect.Type) {
	mappedField := t.mapField(fullName, fieldType, "astNode."+name, name)
	t.writeLn("\tchildren = t.appendNode(children, " + mappedField + ")")
}

func (t *AstContext) visitSliceField(fullName, name string, sliceType reflect.Type) {
	elemType := sliceType.Elem()
	parentListName := "children"
	isParentList := t.ArrayFieldCreatingNode[fullName]
	if isParentList {
		parentListName = "nodeList" + name
		t.writeLn("\tvar " + parentListName + " []*Node")
	}
	t.writeLn("\tfor i := 0; i < len(astNode." + name + "); i++ {")
	fieldMapping := t.mapField(fullName+"[i]", elemType, "astNode."+name+"[i]", "[\" + strconv.Itoa(i) + \"]")
	t.writeLn("\t\t" + parentListName + " = t.appendNode(" + parentListName + ", " + fieldMapping + ")")
	t.writeLn("\t}")
	if isParentList {
		var typeName string
		if elemType.Kind() == reflect.Ptr {
			typeName = "*" + elemType.Elem().Name()
		} else {
			typeName = elemType.Name()
		}
		arguments := "children, " + parentListName + ", \"" + name + "([]" + typeName + ")\""
		t.writeLn("\tchildren = t.appendNodeList(" + arguments + ")")
	}
}

func (t *AstContext) visitIntField(fullName string, field reflect.StructField, fieldType reflect.Type) {
	if fieldType.String() == "token.Pos" && t.TokenFieldWithPos[fullName] {
		// ignore, will be added by the "token.Token" field
		return
	}
	var tokenPos string
	var tokenValue string
	if fieldType.String() == "token.Token" && t.TokenFieldWithPos[fullName+"Pos"] {
		tokenPos = "astNode." + field.Name + "Pos"
		tokenValue = "astNode." + field.Name
	} else if fieldType.String() == "token.Pos" {
		tokenPos = "astNode." + field.Name
		tokenValue = t.MatchingTokenPos[fullName]
		if len(tokenValue) == 0 {
			tokenValue = t.MatchingTokenPos[field.Name]
		}
	}
	if len(tokenValue) > 0 {
		arguments := tokenPos + ", " + tokenValue + ", \"" + field.Name + "\""
		mappedField := "t.createTokenFromPosAstToken(" + arguments + ")"
		t.writeLn("\tchildren = t.appendNode(children, " + mappedField + ")")
	} else {
		panic("Unsupported Int Kind " + fullName + " " + fieldType.String())
	}
}

func (t *AstContext) mapField(fullName string, fieldType reflect.Type, name, fieldName string) string {
	addressPrefix := "&"
	if fieldType.Kind() == reflect.Ptr {
		fieldType = fieldType.Elem()
		addressPrefix = ""
	}
	if fieldType.Kind() == reflect.Interface {
		addressPrefix = ""
	} else if fieldType.Kind() != reflect.Struct {
		panic("Unsupported Kind " + fieldType.Kind().String() + " for " + name + " " + fieldType.String())
	}
	t.pushType(fieldType)
	methodMane := "t.map" + fieldType.Name() + t.FieldVariationMap[fullName]
	return methodMane + "(" + addressPrefix + name + ", \"" + fieldName + "\")"
}

func (t *AstContext) getStructTypesThatImplement(interfaceType reflect.Type) []reflect.Type {
	list := []reflect.Type{}
	for _, astStruct := range t.AllAstStruct {
		if reflect.PtrTo(astStruct).Implements(interfaceType) {
			list = append(list, astStruct)
		}
	}
	return list
}
