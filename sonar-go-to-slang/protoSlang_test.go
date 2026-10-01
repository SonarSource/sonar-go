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
	"bytes"
	"context"
	"encoding/binary"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"testing"

	"github.com/SonarSource/slang/sonar-go-to-slang/proto/slang"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/reflect/protoreflect"
)

// protoKindOfSlangType is the schema of proto/slang/slang.proto seen from the mapper: which oneof
// field of Node each SlangType is encoded as. Spelling out the pairs here rather than deriving them
// makes a case encoded as the wrong kind a test failure instead of a silent change of the AST the
// analyzer receives.
var protoKindOfSlangType = map[string]string{
	"ArrayType":               "array_type",
	"AssignmentExpression":    "assignment_expression",
	"BinaryExpression":        "binary_expression",
	"Block":                   "block",
	"ClassDeclaration":        "class_declaration",
	"CompositeLiteral":        "composite_literal",
	"Ellipsis":                "ellipsis",
	"ExpressionStatement":     "expression_statement",
	"Field":                   "field",
	"FieldList":               "field_list",
	"FloatLiteral":            "float_literal",
	"FunctionDeclaration":     "function_declaration",
	"FunctionInvocation":      "function_invocation",
	"GoStatement":             "go_statement",
	"Identifier":              "identifier",
	"If":                      "if_statement",
	"ImaginaryLiteral":        "imaginary_literal",
	"ImportDeclaration":       "import_declaration",
	"ImportSpecification":     "import_specification",
	"IndexExpression":         "index_expression",
	"IndexListExpression":     "index_list_expression",
	"IntegerLiteral":          "integer_literal",
	"Jump":                    "jump",
	"KeyValue":                "key_value",
	"LeftRightHandSide":       "left_right_hand_side",
	"Literal":                 "literal",
	"Loop":                    "loop",
	"MapType":                 "map_type",
	"Match":                   "match",
	"MatchCase":               "match_case",
	"MemberSelect":            "member_select",
	"Native":                  "native",
	"PackageDeclaration":      "package_declaration",
	"Parameter":               "parameter",
	"ParenthesizedExpression": "parenthesized_expression",
	"PlaceHolder":             "place_holder",
	"RangeClause":             "range_clause",
	"Return":                  "return_statement",
	"Slice":                   "slice",
	"StarExpression":          "star_expression",
	"StringLiteral":           "string_literal",
	"TopLevel":                "top_level",
	"TypeAssertionExpression": "type_assertion_expression",
	"UnaryExpression":         "unary_expression",
	"VariableDeclaration":     "variable_declaration",
}

// nodeSummary is what a node is compared by: encoding it as the wrong kind, at the wrong position,
// or under the wrong control flow id all show up as a difference.
type nodeSummary struct {
	kind      string
	textRange string
	cfgId     int32
}

// TestToProtoSlangEncodesEveryNodeOfEveryFixture walks the generic tree and the encoded one of every
// fixture of resources/ast, and compares the nodes they hold. The comparison is made on sorted
// summaries rather than in tree order, as the fields of a generic node are a map, which Go iterates
// in no particular order.
func TestToProtoSlangEncodesEveryNodeOfEveryFixture(t *testing.T) {
	for _, file := range getAllGoFiles("resources/ast") {
		t.Run(filepath.Base(file), func(t *testing.T) {
			source, err := os.ReadFile(file)
			require.NoError(t, err)
			fileName := strings.Replace(filepath.Base(file), ".source", "", 1)
			root, comments, tokens, errMsg := slangFromString(fileName, string(source), "ModuleNameForTest")
			require.Nil(t, errMsg)

			tree := unmarshalTree(t, toProtoSlang(fileName, root, comments, tokens, errMsg))

			assert.Equal(t, summarizeGenericNodes(t, root), summarizeProtoNodes(t, tree.GetRoot()))
		})
	}
}

func TestToProtoSlangEncodesCommentsAndTokens(t *testing.T) {
	source := `package main

// A line comment.
/* A block comment. */
func main() {}
`
	root, comments, tokens, errMsg := slangFromString("main.go", source, "ModuleNameForTest")
	require.Nil(t, errMsg)

	tree := unmarshalTree(t, toProtoSlang("main.go", root, comments, tokens, errMsg))

	require.Len(t, tree.GetComments(), 2)
	assert.Equal(t, "// A line comment.", tree.GetComments()[0].GetText())
	assert.Equal(t, " A line comment.", tree.GetComments()[0].GetContentText())
	assert.Equal(t, "3:0::18", tree.GetComments()[0].GetRange())
	assert.Equal(t, "3:2::18", tree.GetComments()[0].GetContentRange())
	assert.Equal(t, "/* A block comment. */", tree.GetComments()[1].GetText())
	assert.Equal(t, " A block comment. ", tree.GetComments()[1].GetContentText())
	assert.Equal(t, "4:0::22", tree.GetComments()[1].GetRange())
	assert.Equal(t, "4:2::20", tree.GetComments()[1].GetContentRange())

	// The type is only written when it is not the most frequent one, which the Java side defaults to.
	assert.Contains(t, tree.GetTokens(), &slang.Token{Text: "package", TextRange: "1:0::7", Type: keywordKind})
	assert.Contains(t, tree.GetTokens(), &slang.Token{Text: "main", TextRange: "1:8::12"})
	assert.Empty(t, tree.GetError())
}

func TestToProtoSlangEncodesAParseErrorWithoutATree(t *testing.T) {
	errMsg := "main.go:1:1: expected 'package', found xpackage"

	tree := unmarshalTree(t, toProtoSlang("main.go", nil, nil, nil, &errMsg))

	assert.Equal(t, errMsg, tree.GetError())
	assert.Nil(t, tree.GetRoot())
}

func TestToProtoSlangPanicsOnAnUnknownSlangType(t *testing.T) {
	node := &Node{SlangType: "NotASlangType", SlangField: map[string]interface{}{}}

	assert.PanicsWithValue(t, `toProtoSlang: unsupported SlangType "NotASlangType"`, func() { buildProtoNode(node) })
}

func TestToProtoSlangPanicsOnAFieldOfAnotherTypeThanTheSchema(t *testing.T) {
	node := &Node{SlangType: "Block", SlangField: map[string]interface{}{"statementOrExpressions": "not a node list"}}

	assert.PanicsWithValue(t, `toProtoSlang: expected []*Node for field "statementOrExpressions", got string`, func() { buildProtoNode(node) })
}

// TestToProtoSlangEncodesTheControlFlowGraph checks that the graph of a function refers to the nodes
// of that same function: the ids are stamped by a counter that is reset for every top-level function,
// so the encoding is only usable as long as each graph keeps pointing at its own function's nodes.
func TestToProtoSlangEncodesTheControlFlowGraph(t *testing.T) {
	// The two functions have the same body on purpose: they reuse the same ids, and a graph taking
	// them from the other function would go unnoticed if their bodies differed.
	source := `package main

func first() {
	x := 1
	if x > 0 {
		x = 2
	}
}

func second() {
	y := 1
	if y > 0 {
		y = 2
	}
}
`
	root, comments, tokens, errMsg := slangFromString("main.go", source, "ModuleNameForTest")
	require.Nil(t, errMsg)

	tree := unmarshalTree(t, toProtoSlang("main.go", root, comments, tokens, errMsg))

	var functions []*slang.FunctionDeclaration
	for _, declaration := range tree.GetRoot().GetTopLevel().GetDeclarations() {
		if function := declaration.GetFunctionDeclaration(); function != nil {
			functions = append(functions, function)
		}
	}
	require.Len(t, functions, 2)
	for _, function := range functions {
		cfg := function.GetCfg()
		require.NotEmpty(t, cfg.GetBlocks())

		ownIds := make(map[int32]bool)
		for _, node := range protoNodes(t, function.GetBody()) {
			ownIds[node.GetCfgId()] = true
		}
		for blockIndex, block := range cfg.GetBlocks() {
			for _, id := range block.GetNodeIds() {
				// 0 is a node of the graph that the mapper stamped no id on, and that the Java side drops.
				assert.True(t, id == 0 || ownIds[id], "block %d refers to the id %d, which is not one of %s's own nodes",
					blockIndex, id, function.GetName().GetIdentifier().GetName())
			}
			for _, successor := range block.GetSuccessors() {
				assert.Less(t, int(successor), len(cfg.GetBlocks()))
			}
		}
	}
}

func TestToSlangProtoFramesEveryFileOfTheBatch(t *testing.T) {
	fileContents := map[string]string{"first.go": "package main", "second.go": "package main"}
	fileSet, astFiles := astFromStrings(fileContents)
	info, _ := typeCheckAst(context.Background(), fileSet, astFiles, false, "", "ModuleNameForTest", ".", GcExporter{})

	var output bytes.Buffer
	toSlangProto(context.Background(), fileSet, astFiles, fileContents, info, "ModuleNameForTest", &output)

	files := decodeFiles(t, output.Bytes())
	require.Len(t, files, 2)
	var names []string
	for _, file := range files {
		names = append(names, file.GetName())
		assert.Equal(t, "1:0::12", file.GetTree().GetRoot().GetTextRange(), "the tree of %s covers its whole source", file.GetName())
	}
	assert.ElementsMatch(t, []string{"first.go", "second.go"}, names)
}

// decodeTrees rebuilds every tree of one response, the way the analyzer does, so that a test can
// assert on the AST it received rather than on the bytes that carried it.
func decodeTrees(t *testing.T, response string) map[string]*slang.Tree {
	t.Helper()
	trees := map[string]*slang.Tree{}
	for _, file := range decodeFiles(t, []byte(response)) {
		trees[file.GetName()] = file.GetTree()
	}
	return trees
}

// identifierFact is what the type checker filled in on one identifier, in a form a test can compare
// whole: the proto messages themselves carry internal state that does not compare by value.
type identifierFact struct {
	name   string
	typeOf string
	pkg    string
	id     int32
}

// identifierFacts returns one fact per identifier of a response, which is where the type checker
// records what it resolved.
func identifierFacts(t *testing.T, response string) []identifierFact {
	t.Helper()
	var facts []identifierFact
	for _, tree := range decodeTrees(t, response) {
		for _, node := range protoNodes(t, tree.GetRoot()) {
			if identifier := node.GetIdentifier(); identifier != nil {
				facts = append(facts, identifierFact{identifier.GetName(), identifier.GetType(), identifier.GetPackage(), identifier.GetId()})
			}
		}
	}
	return facts
}

// identifierTypes projects the resolved type of every identifier of a response, for the assertions
// that cannot name the identifier holding it.
func identifierTypes(t *testing.T, response string) []string {
	t.Helper()
	var types []string
	for _, fact := range identifierFacts(t, response) {
		types = append(types, fact.typeOf)
	}
	return types
}

// unmarshalTree reads back the message toProtoSlang writes, of which only the tree is under test here.
func unmarshalTree(t *testing.T, payload []byte) *slang.Tree {
	t.Helper()
	return unmarshalFile(t, payload).GetTree()
}

func unmarshalFile(t *testing.T, payload []byte) *slang.File {
	t.Helper()
	file := &slang.File{}
	require.NoError(t, proto.Unmarshal(payload, file))
	return file
}

// decodeFiles reads back the framing of toSlangProto, the way ProtoTree does on the Java side: one
// length-prefixed slang.File per file of the batch.
func decodeFiles(t *testing.T, output []byte) []*slang.File {
	t.Helper()
	var files []*slang.File
	for offset := 0; offset < len(output); {
		require.LessOrEqual(t, offset+4, len(output))
		length := int(binary.LittleEndian.Uint32(output[offset:]))
		offset += 4
		require.LessOrEqual(t, offset+length, len(output))
		files = append(files, unmarshalFile(t, output[offset:offset+length]))
		offset += length
	}
	return files
}

func summarizeGenericNodes(t *testing.T, root *Node) []nodeSummary {
	t.Helper()
	var summaries []nodeSummary
	var walk func(node *Node)
	walk = func(node *Node) {
		if node == nil {
			return
		}
		kind, known := protoKindOfSlangType[node.SlangType]
		require.True(t, known, "the SlangType %q is not encoded by any kind of Node", node.SlangType)
		summaries = append(summaries, nodeSummary{kind, textRangeOf(node), cfgIdField(node.SlangField)})
		for _, value := range node.SlangField {
			switch child := value.(type) {
			case *Node:
				walk(child)
			case []*Node:
				for _, listed := range child {
					walk(listed)
				}
			}
		}
	}
	walk(root)
	return sortSummaries(summaries)
}

func summarizeProtoNodes(t *testing.T, root *slang.Node) []nodeSummary {
	t.Helper()
	var summaries []nodeSummary
	for _, node := range protoNodes(t, root) {
		summaries = append(summaries, nodeSummary{protoKindOf(t, node), node.GetTextRange(), node.GetCfgId()})
	}
	return sortSummaries(summaries)
}

// protoNodes collects the given node and all of its descendants, walking the messages by reflection
// so that a field the schema gains is covered without changing this helper.
func protoNodes(t *testing.T, root *slang.Node) []*slang.Node {
	t.Helper()
	var nodes []*slang.Node
	var walk func(message proto.Message)
	walk = func(message proto.Message) {
		if node, ok := message.(*slang.Node); ok {
			nodes = append(nodes, node)
		}
		message.ProtoReflect().Range(func(field protoreflect.FieldDescriptor, value protoreflect.Value) bool {
			if field.Kind() != protoreflect.MessageKind {
				return true
			}
			if field.IsList() {
				list := value.List()
				for i := range list.Len() {
					walk(list.Get(i).Message().Interface())
				}
			} else {
				walk(value.Message().Interface())
			}
			return true
		})
	}
	if root != nil {
		walk(root)
	}
	return nodes
}

func protoKindOf(t *testing.T, node *slang.Node) string {
	t.Helper()
	message := node.ProtoReflect()
	field := message.WhichOneof(message.Descriptor().Oneofs().ByName("kind"))
	require.NotNil(t, field, "the node at %q has no kind", node.GetTextRange())
	return string(field.Name())
}

func textRangeOf(node *Node) string {
	if node.TextRange == nil {
		return ""
	}
	return formatTextRange(node.TextRange)
}

func sortSummaries(summaries []nodeSummary) []nodeSummary {
	sort.Slice(summaries, func(i, j int) bool {
		if summaries[i].textRange != summaries[j].textRange {
			return summaries[i].textRange < summaries[j].textRange
		}
		if summaries[i].kind != summaries[j].kind {
			return summaries[i].kind < summaries[j].kind
		}
		return summaries[i].cfgId < summaries[j].cfgId
	})
	return summaries
}

// TestFormatTextRangeWritesAMissingRangeAsEmpty pins what a node, comment or token without a range is
// encoded as. Dereferencing it instead would panic, and encodeFile would report the file as a memory
// fault rather than as the invalid range the Java side can name.
func TestFormatTextRangeWritesAMissingRangeAsEmpty(t *testing.T) {
	assert.Empty(t, formatTextRange(nil))
	assert.Empty(t, buildProtoNode(&Node{SlangType: "Block", SlangField: map[string]interface{}{}}).GetTextRange())
	assert.Empty(t, buildProtoToken(&Token{Value: "func", TokenType: "KEYWORD"}).GetTextRange())
}
