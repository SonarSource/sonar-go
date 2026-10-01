// SonarSource Go
// Copyright (C) SonarSource Sàrl
// mailto:info AT sonarsource DOT com
//
// You can redistribute and/or modify this program under the terms of
// the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
	"strings"

	"github.com/SonarSource/slang/sonar-go-to-slang/proto/slang"
	"google.golang.org/protobuf/proto"
)

// toProtoSlang encodes one file in the wire format of proto/slang/slang.proto: the untyped SlangField
// map of each node becomes the typed message of its SlangType, and the name of the file travels in the
// same slang.File message as its tree, which is how the analyzer reads them back. It is the only
// format the executable writes; the golden files of the tests read the same tree through
// buildProtoTree instead, so that a snapshot cannot describe an AST the analyzer never receives.
func toProtoSlang(fileName string, node *Node, comments []*Node, tokens []*Token, errMsg *string) []byte {
	file := &slang.File{Name: fileName, Tree: buildProtoTree(node, comments, tokens, errMsg)}
	data, err := proto.Marshal(file)
	if err != nil {
		panic(err)
	}
	return data
}

// buildProtoTree assembles the message of one file, before it is serialized.
func buildProtoTree(node *Node, comments []*Node, tokens []*Token, errMsg *string) *slang.Tree {
	tree := &slang.Tree{
		Comments: make([]*slang.Comment, len(comments)),
		Tokens:   make([]*slang.Token, len(tokens)),
		Root:     buildProtoNode(node),
		Error:    errMsg,
	}
	for i, comment := range comments {
		tree.Comments[i] = buildProtoComment(comment)
	}
	for i, token := range tokens {
		tree.Tokens[i] = buildProtoToken(token)
	}
	return tree
}

// buildProtoNode turns one node into the message of its SlangType, reading the fields the type is
// known to have out of the SlangField map. An unknown SlangType panics rather than being dropped:
// the whole point of the typed schema is that a node the analyzer could not read cannot go
// unnoticed.
func buildProtoNode(node *Node) *slang.Node {
	if node == nil {
		return nil
	}

	f := node.SlangField
	result := &slang.Node{CfgId: cfgIdField(f), TextRange: formatTextRange(node.TextRange)}

	if !setProtoExpressionKind(result, node.SlangType, f) && !setProtoStatementKind(result, node.SlangType, f) {
		panic(fmt.Sprintf("toProtoSlang: unsupported SlangType %q", node.SlangType))
	}

	return result
}

// setProtoExpressionKind sets the kind of a node that stands for an expression or a literal, and
// reports whether its SlangType is one of those. Together with setProtoStatementKind it covers every
// SlangType the mapper produces; the dispatch is split in two only because one switch over all of
// them is too long to read.
func setProtoExpressionKind(result *slang.Node, slangType string, f map[string]interface{}) bool {
	switch slangType {
	case "Identifier":
		result.Kind = &slang.Node_Identifier{Identifier: &slang.Identifier{
			Name:    stringField(f, "name"),
			Type:    stringField(f, "type"),
			Package: stringField(f, "package"),
			Id:      int32(intField(f, "id")),
		}}
	case "IntegerLiteral":
		result.Kind = &slang.Node_IntegerLiteral{IntegerLiteral: &slang.IntegerLiteral{
			Value: stringField(f, "value"),
		}}
	case "StringLiteral":
		result.Kind = &slang.Node_StringLiteral{StringLiteral: &slang.StringLiteral{
			Value:   stringField(f, "value"),
			Content: stringField(f, "content"),
		}}
	case "FunctionInvocation":
		result.Kind = &slang.Node_FunctionInvocation{FunctionInvocation: &slang.FunctionInvocation{
			MemberSelect: buildProtoNode(nodeField(f, "memberSelect")),
			Arguments:    buildProtoNodes(nodeListField(f, "arguments")),
			ReturnType:   stringListField(f, "returnType"),
		}}
	case "MemberSelect":
		result.Kind = &slang.Node_MemberSelect{MemberSelect: &slang.MemberSelect{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Identifier: buildProtoNode(nodeField(f, identifierField)),
		}}
	case "BinaryExpression":
		result.Kind = &slang.Node_BinaryExpression{BinaryExpression: &slang.BinaryExpression{
			Operator:      stringField(f, operatorField),
			OperatorToken: textRangeField(f, "operatorToken"),
			LeftOperand:   buildProtoNode(nodeField(f, "leftOperand")),
			RightOperand:  buildProtoNode(nodeField(f, "rightOperand")),
		}}
	case "AssignmentExpression":
		result.Kind = &slang.Node_AssignmentExpression{AssignmentExpression: &slang.AssignmentExpression{
			Operator:              stringField(f, operatorField),
			LeftHandSide:          buildProtoNode(nodeField(f, "leftHandSide")),
			StatementOrExpression: buildProtoNode(nodeField(f, "statementOrExpression")),
		}}
	case "CompositeLiteral":
		result.Kind = &slang.Node_CompositeLiteral{CompositeLiteral: &slang.CompositeLiteral{
			Type:     buildProtoNode(nodeField(f, "type")),
			Elements: buildProtoNodes(nodeListField(f, "elements")),
		}}
	case "PlaceHolder":
		result.Kind = &slang.Node_PlaceHolder{PlaceHolder: &slang.PlaceHolder{
			PlaceHolderToken: textRangeField(f, "placeHolderToken"),
		}}
	case "IndexExpression":
		result.Kind = &slang.Node_IndexExpression{IndexExpression: &slang.IndexExpression{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Index:      buildProtoNode(nodeField(f, "index")),
		}}
	case "KeyValue":
		result.Kind = &slang.Node_KeyValue{KeyValue: &slang.KeyValue{
			Key:   buildProtoNode(nodeField(f, "key")),
			Value: buildProtoNode(nodeField(f, "value")),
		}}
	case "StarExpression":
		result.Kind = &slang.Node_StarExpression{StarExpression: &slang.StarExpression{
			Expression: buildProtoNode(nodeField(f, expressionField)),
		}}
	case "Literal":
		result.Kind = &slang.Node_Literal{Literal: &slang.Literal{
			Value: stringField(f, "value"),
		}}
	case "UnaryExpression":
		result.Kind = &slang.Node_UnaryExpression{UnaryExpression: &slang.UnaryExpression{
			Operator: stringField(f, operatorField),
			Operand:  buildProtoNode(nodeField(f, operandField)),
		}}
	case "ParenthesizedExpression":
		result.Kind = &slang.Node_ParenthesizedExpression{ParenthesizedExpression: &slang.ParenthesizedExpression{
			Expression:       buildProtoNode(nodeField(f, expressionField)),
			LeftParenthesis:  textRangeField(f, "leftParenthesis"),
			RightParenthesis: textRangeField(f, "rightParenthesis"),
		}}
	case "LeftRightHandSide":
		result.Kind = &slang.Node_LeftRightHandSide{LeftRightHandSide: &slang.LeftRightHandSide{
			Children: buildProtoNodes(nodeListField(f, childrenField)),
		}}
	case "FloatLiteral":
		result.Kind = &slang.Node_FloatLiteral{FloatLiteral: &slang.FloatLiteral{
			Value: stringField(f, "value"),
		}}
	case "Slice":
		result.Kind = &slang.Node_Slice{Slice: &slang.Slice{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Low:        buildProtoNode(nodeField(f, "low")),
			High:       buildProtoNode(nodeField(f, "high")),
			Max:        buildProtoNode(nodeField(f, "max")),
			Slice3:     boolField(f, "slice3"),
		}}
	case "IndexListExpression":
		result.Kind = &slang.Node_IndexListExpression{IndexListExpression: &slang.IndexListExpression{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Indices:    buildProtoNodes(nodeListField(f, "indices")),
		}}
	case "TypeAssertionExpression":
		result.Kind = &slang.Node_TypeAssertionExpression{TypeAssertionExpression: &slang.TypeAssertionExpression{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Type:       buildProtoNode(nodeField(f, "type")),
		}}
	case "Ellipsis":
		result.Kind = &slang.Node_Ellipsis{Ellipsis: &slang.Ellipsis{
			Ellipsis: textRangeField(f, "ellipsis"),
			Element:  buildProtoNode(nodeField(f, "element")),
		}}
	case "ImaginaryLiteral":
		result.Kind = &slang.Node_ImaginaryLiteral{ImaginaryLiteral: &slang.ImaginaryLiteral{
			Value: stringField(f, "value"),
		}}
	default:
		return false
	}
	return true
}

// setProtoStatementKind sets the kind of a node that stands for a statement, a declaration, a type
// or a native wrapper, and reports whether its SlangType is one of those.
func setProtoStatementKind(result *slang.Node, slangType string, f map[string]interface{}) bool {
	switch slangType {
	case nativeSlangType:
		result.Kind = &slang.Node_Native{Native: &slang.Native{
			NativeKind: stringField(f, nativeKind),
			Children:   buildProtoNodes(nodeListField(f, childrenField)),
		}}
	case "VariableDeclaration":
		result.Kind = &slang.Node_VariableDeclaration{VariableDeclaration: &slang.VariableDeclaration{
			Identifiers:  buildProtoNodes(nodeListField(f, identifiersField)),
			Type:         buildProtoNode(nodeField(f, "type")),
			Initializers: buildProtoNodes(nodeListField(f, "initializers")),
			IsVal:        boolField(f, "isVal"),
		}}
	case "Block":
		result.Kind = &slang.Node_Block{Block: &slang.Block{
			StatementOrExpressions: buildProtoNodes(nodeListField(f, "statementOrExpressions")),
		}}
	case "FunctionDeclaration":
		result.Kind = &slang.Node_FunctionDeclaration{FunctionDeclaration: &slang.FunctionDeclaration{
			ReturnType:       buildProtoNode(nodeField(f, "returnType")),
			Receiver:         buildProtoNode(nodeField(f, "receiver")),
			Name:             buildProtoNode(nodeField(f, "name")),
			FormalParameters: buildProtoNodes(nodeListField(f, "formalParameters")),
			TypeParameters:   buildProtoNode(nodeField(f, "typeParameters")),
			Body:             buildProtoNode(nodeField(f, "body")),
			Cfg:              buildProtoCfg(cfgField(f, "cfg")),
		}}
	case "ExpressionStatement":
		result.Kind = &slang.Node_ExpressionStatement{ExpressionStatement: &slang.ExpressionStatement{
			Expression: buildProtoNode(nodeField(f, expressionField)),
		}}
	case "Parameter":
		result.Kind = &slang.Node_Parameter{Parameter: &slang.Parameter{
			Identifier: buildProtoNode(nodeField(f, identifierField)),
			Type:       buildProtoNode(nodeField(f, "type")),
		}}
	case "ArrayType":
		result.Kind = &slang.Node_ArrayType{ArrayType: &slang.ArrayType{
			Element: buildProtoNode(nodeField(f, "element")),
			Length:  buildProtoNode(nodeField(f, "length")),
		}}
	case "TopLevel":
		result.Kind = &slang.Node_TopLevel{TopLevel: &slang.TopLevel{
			Declarations:  buildProtoNodes(nodeListField(f, "declarations")),
			FirstCpdToken: textRangeField(f, "firstCpdToken"),
		}}
	case "PackageDeclaration":
		result.Kind = &slang.Node_PackageDeclaration{PackageDeclaration: &slang.PackageDeclaration{
			Children: buildProtoNodes(nodeListField(f, childrenField)),
		}}
	case "ClassDeclaration":
		result.Kind = &slang.Node_ClassDeclaration{ClassDeclaration: &slang.ClassDeclaration{
			Identifier:     textRangeField(f, identifierField),
			ClassTree:      buildProtoNode(nodeField(f, "classTree")),
			TypeParameters: textRangeField(f, typeParametersField),
		}}
	case "ImportSpecification":
		result.Kind = &slang.Node_ImportSpecification{ImportSpecification: &slang.ImportSpecification{
			Name: buildProtoNode(nodeField(f, "name")),
			Path: buildProtoNode(nodeField(f, "path")),
		}}
	case "Return":
		result.Kind = &slang.Node_ReturnStatement{ReturnStatement: &slang.Return{
			Expressions: buildProtoNodes(nodeListField(f, expressionsField)),
			Keyword:     textRangeField(f, keywordField),
		}}
	case "ImportDeclaration":
		result.Kind = &slang.Node_ImportDeclaration{ImportDeclaration: &slang.ImportDeclaration{
			Children: buildProtoNodes(nodeListField(f, childrenField)),
		}}
	case "Loop":
		result.Kind = &slang.Node_Loop{Loop: &slang.Loop{
			Condition: buildProtoNode(nodeField(f, conditionField)),
			Body:      buildProtoNode(nodeField(f, "body")),
			Kind:      stringField(f, "kind"),
			Keyword:   textRangeField(f, keywordField),
		}}
	case "RangeClause":
		result.Kind = &slang.Node_RangeClause{RangeClause: &slang.RangeClause{
			Key:              buildProtoNode(nodeField(f, "key")),
			Value:            buildProtoNode(nodeField(f, "value")),
			RangedExpression: buildProtoNode(nodeField(f, "rangedExpression")),
			IsDeclaration:    boolField(f, "isDeclaration"),
		}}
	case "FieldList":
		result.Kind = &slang.Node_FieldList{FieldList: &slang.FieldList{
			Fields: buildProtoNodes(nodeListField(f, "fields")),
		}}
	case "Field":
		result.Kind = &slang.Node_Field{Field: &slang.Field{
			Names: buildProtoNodes(nodeListField(f, "names")),
			Type:  buildProtoNode(nodeField(f, "type")),
		}}
	case "MapType":
		result.Kind = &slang.Node_MapType{MapType: &slang.MapType{
			Key:   buildProtoNode(nodeField(f, "key")),
			Value: buildProtoNode(nodeField(f, "value")),
		}}
	case "MatchCase":
		result.Kind = &slang.Node_MatchCase{MatchCase: &slang.MatchCase{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Body:       buildProtoNode(nodeField(f, "body")),
		}}
	case "If":
		result.Kind = &slang.Node_IfStatement{IfStatement: &slang.If{
			Condition:   buildProtoNode(nodeField(f, conditionField)),
			ThenBranch:  buildProtoNode(nodeField(f, "thenBranch")),
			ElseBranch:  buildProtoNode(nodeField(f, "elseBranch")),
			IfKeyword:   textRangeField(f, "ifKeyword"),
			ElseKeyword: textRangeField(f, "elseKeyword"),
		}}
	case "Match":
		result.Kind = &slang.Node_Match{Match: &slang.Match{
			Expression: buildProtoNode(nodeField(f, expressionField)),
			Cases:      buildProtoNodes(nodeListField(f, "cases")),
			Keyword:    textRangeField(f, keywordField),
		}}
	case "Jump":
		result.Kind = &slang.Node_Jump{Jump: &slang.Jump{
			Label:   buildProtoNode(nodeField(f, "label")),
			Keyword: textRangeField(f, keywordField),
			Kind:    stringField(f, "kind"),
		}}
	case "GoStatement":
		result.Kind = &slang.Node_GoStatement{GoStatement: &slang.GoStatement{
			GoToken:            textRangeField(f, "goToken"),
			FunctionInvocation: buildProtoNode(nodeField(f, "functionInvocation")),
		}}
	default:
		return false
	}
	return true
}

func buildProtoNodes(nodes []*Node) []*slang.Node {
	if len(nodes) == 0 {
		return nil
	}
	result := make([]*slang.Node, len(nodes))
	for i, node := range nodes {
		result[i] = buildProtoNode(node)
	}
	return result
}

// buildProtoCfg carries the graph over as it is: the blocks keep their order, so that the successor
// indices keep pointing at the same ones, and the node ids keep matching the cfg_id of the nodes of
// the enclosing function.
func buildProtoCfg(cfg *CfgToJava) *slang.Cfg {
	if cfg == nil {
		return nil
	}
	blocks := make([]*slang.CfgBlock, len(cfg.Blocks))
	for i, block := range cfg.Blocks {
		blocks[i] = &slang.CfgBlock{
			NodeIds:    block.Node,
			Successors: block.Successors,
		}
	}
	return &slang.Cfg{Blocks: blocks}
}

// commentContent strips the markers of a comment and narrows its range to what is left.
func commentContent(comment *Node) (string, TextRange) {
	text := comment.Token.Value
	contentRange := TextRange(*comment.TextRange)
	contentRange.StartColumn = contentRange.StartColumn + 2

	if strings.HasPrefix(text, "//") {
		return text[2:], contentRange
	}
	if strings.HasPrefix(text, "/*") {
		contentRange.EndColumn = contentRange.EndColumn - 2
		return text[2 : len(text)-2], contentRange
	}
	panic("Unknown comment content: " + text)
}

func buildProtoComment(comment *Node) *slang.Comment {
	contentText, contentRange := commentContent(comment)
	return &slang.Comment{
		Text:         comment.Token.Value,
		ContentText:  contentText,
		Range:        formatTextRange(comment.TextRange),
		ContentRange: formatTextRange(&contentRange),
	}
}

func buildProtoToken(token *Token) *slang.Token {
	protoToken := &slang.Token{
		Text:      token.Value,
		TextRange: formatTextRange(token.TextRange),
	}
	if token.TokenType != other {
		protoToken.Type = token.TokenType
	}
	return protoToken
}

// formatTextRange writes a range the way RangeConverter reads it on the Java side.
//
// A missing range is written as the empty string, the same as a node the mapper gave none. The Java
// side rejects an empty range as an invalid one, for that one file, which names the problem, where
// dereferencing it here would panic and be reported as a memory fault of the whole file instead.
func formatTextRange(textRange *TextRange) string {
	if textRange == nil {
		return ""
	}
	if textRange.StartLine == textRange.EndLine {
		return fmt.Sprintf("%d:%d::%d", textRange.StartLine, textRange.StartColumn-1, textRange.EndColumn-1)
	}
	return fmt.Sprintf("%d:%d:%d:%d", textRange.StartLine, textRange.StartColumn-1, textRange.EndLine, textRange.EndColumn-1)
}

// The accessors below read one field of a SlangField map, and are the only place where the untyped
// map is left behind. A field that is absent, or that holds an explicit nil, is the zero value of
// its type; a field of another type than the schema declares panics, as the two have then drifted
// apart.

func nodeField(slangField map[string]interface{}, key string) *Node {
	value, ok := slangField[key]
	if !ok || value == nil {
		return nil
	}
	node, ok := value.(*Node)
	if !ok {
		panic(fieldTypeError(key, "*Node", value))
	}
	return node
}

func nodeListField(slangField map[string]interface{}, key string) []*Node {
	value, ok := slangField[key]
	if !ok || value == nil {
		return nil
	}
	nodes, ok := value.([]*Node)
	if !ok {
		panic(fieldTypeError(key, "[]*Node", value))
	}
	return nodes
}

// stringField also reads a *string, which is what the mapper stores for the operator of an
// assignment.
func stringField(slangField map[string]interface{}, key string) string {
	value, ok := slangField[key]
	if !ok || value == nil {
		return ""
	}
	switch text := value.(type) {
	case string:
		return text
	case *string:
		if text == nil {
			return ""
		}
		return *text
	default:
		panic(fieldTypeError(key, "string", value))
	}
}

func stringListField(slangField map[string]interface{}, key string) []string {
	value, ok := slangField[key]
	if !ok || value == nil {
		return nil
	}
	texts, ok := value.([]string)
	if !ok {
		panic(fieldTypeError(key, "[]string", value))
	}
	return texts
}

func boolField(slangField map[string]interface{}, key string) bool {
	value, ok := slangField[key]
	if !ok || value == nil {
		return false
	}
	flag, ok := value.(bool)
	if !ok {
		panic(fieldTypeError(key, "bool", value))
	}
	return flag
}

func intField(slangField map[string]interface{}, key string) int {
	value, ok := slangField[key]
	if !ok || value == nil {
		return 0
	}
	number, ok := value.(int)
	if !ok {
		panic(fieldTypeError(key, "int", value))
	}
	return number
}

// textRangeField reads a field that only refers to a token by its position, such as a keyword or an
// operator, which the Java side resolves against the tokens of the file.
func textRangeField(slangField map[string]interface{}, key string) string {
	value, ok := slangField[key]
	if !ok || value == nil {
		return ""
	}
	textRange, ok := value.(*TextRange)
	if !ok {
		panic(fieldTypeError(key, "*TextRange", value))
	}
	return formatTextRange(textRange)
}

// cfgIdField reads the id that createNodeWithChildren and createLeafNode stamp on the nodes a Go
// AST node maps to. The nodes without one are not part of any control flow graph, and get 0.
func cfgIdField(slangField map[string]interface{}) int32 {
	value, ok := slangField["__cfgId"]
	if !ok || value == nil {
		return 0
	}
	id, ok := value.(int32)
	if !ok {
		panic(fieldTypeError("__cfgId", "int32", value))
	}
	return id
}

func cfgField(slangField map[string]interface{}, key string) *CfgToJava {
	value, ok := slangField[key]
	if !ok || value == nil {
		return nil
	}
	cfg, ok := value.(*CfgToJava)
	if !ok {
		panic(fieldTypeError(key, "*CfgToJava", value))
	}
	return cfg
}

func fieldTypeError(key, expectedType string, value interface{}) string {
	return fmt.Sprintf("toProtoSlang: expected %s for field %q, got %T", expectedType, key, value)
}
