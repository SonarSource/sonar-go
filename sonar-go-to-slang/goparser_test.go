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

// The following directive is necessary to make the package coherent:

// This program generates 'goparser_generated.go'. It can be invoked by running "go generate"

package main

import (
	"context"
	"go/ast"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/encoding/prototext"
	"google.golang.org/protobuf/proto"
)

// goldenProtoText is how every snapshot under resources/ is written. The trees are rendered from the
// protobuf messages the executable sends, so that a snapshot cannot describe an AST the analyzer
// never receives. Indent is pinned rather than left to prototext, which picks its own.
var goldenProtoText = prototext.MarshalOptions{Multiline: true, Indent: "  "}

// fieldSeparator matches the colon that ends a field name at the start of a line, with the spaces
// that follow it. Anchoring it there keeps it off the ": " of a string value, which is written on
// the same line, escaped, after the separator this matches.
var fieldSeparator = regexp.MustCompile(`(?m)^(\s*[a-z0-9_]+:) +`)

// protoTextOfTree renders the tree of one file the way the golden files hold it.
//
// prototext writes either one or two spaces after a field name, picked once per build of the
// program from a hash of the binary, on purpose, to discourage depending on its exact output. A
// golden file needs exactly that, so the separator is normalized to one space here: without it
// every snapshot would differ between, say, a plain build and a -tags sonartrace one.
func protoTextOfTree(node *Node, comments []*Node, tokens []*Token, errMsg *string) string {
	text, err := goldenProtoText.Marshal(buildProtoTree(node, comments, tokens, errMsg))
	if err != nil {
		panic(err)
	}
	return fieldSeparator.ReplaceAllString(string(text), "$1 ")
}

// goldenFileOf names the snapshot of a .go.source file.
func goldenFileOf(sourceFile string) string {
	return strings.Replace(sourceFile, "go.source", "prototxt", 1)
}

func slangFromString(filename, source, moduleName string) (*Node, []*Node, []*Token, *string) {
	fileSet, astFileOrErrors := astFromString(filename, source)
	info, _ := typeCheckAst(context.Background(), fileSet, astFileOrErrors, true, "", "ModuleNameForTest", ".", GcExporter{})
	astFileOrError := astFileOrErrors[filename]
	slangTree, comments, tokens, errMsg, _ := toSlangTree(fileSet, &astFileOrError, source, info, moduleName, buildUsesByPos(info))
	return slangTree, comments, tokens, errMsg
}

func astFromString(filename, source string) (fileSet *token.FileSet, astFileOrErrors map[string]AstFileOrError) {
	fileSet = token.NewFileSet()
	fileNameToContent := make(map[string]string)
	fileNameToContent[filename] = source
	astFileOrErrors = readAstString(context.Background(), fileSet, fileNameToContent)
	return
}

func astFromStrings(fileNameToContent map[string]string) (fileSet *token.FileSet, astFileOrErrors map[string]AstFileOrError) {
	fileSet = token.NewFileSet()
	astFileOrErrors = readAstString(context.Background(), fileSet, fileNameToContent)
	return
}

func TestIdentifierInfoWithCompositeLiteralKeys(t *testing.T) {
	testCases := []struct {
		name            string
		source          string
		identifier      string
		omitTypeInfo    bool
		expectedBinding string
		expectedType    string
	}{
		{
			name: "unresolved literal type", source: "func f() { value := 1; _ = MissingStruct{value: 2} }",
			identifier: "value", expectedType: "UNKNOWN",
		},
		{
			name: "nonexistent field", source: "type S struct{ A int }; func f() { B := 6; _ = S{B: 7} }",
			identifier: "B", expectedType: "UNKNOWN",
		},
		{
			name: "nested literal with elided type", source: "func f() { value := 1; _ = []MissingStruct{{value: 2}} }",
			identifier: "value", expectedType: "UNKNOWN",
		},
		{
			name: "resolved field overrides parser binding", source: "type S struct{ B int }; func f() { B := 6; _ = S{B: 7} }",
			identifier: "B", expectedBinding: "B int", expectedType: "int",
		},
		{
			name: "resolved map key", source: "func f() { value := 1; _ = map[int]int{value: 2} }",
			identifier: "value", expectedBinding: "value :=", expectedType: "int",
		},
		{
			name: "resolved array index", source: "func f() { const index = 1; _ = [3]int{index: 2} }",
			identifier: "index", expectedBinding: "index =", expectedType: "untyped int",
		},
		{
			name: "unresolved map key without type info", source: "func f() { value := 1; _ = map[int]int{value: 2} }",
			identifier: "value", omitTypeInfo: true, expectedType: "UNKNOWN",
		},
		{
			name: "ordinary variable fallback", source: "func f() { value := 1; _ = value }",
			identifier: "value", omitTypeInfo: true, expectedBinding: "value :=", expectedType: "UNKNOWN",
		},
		{
			name: "literal value fallback", source: "func f() { value := 1; _ = MissingStruct{field: value} }",
			identifier: "value", omitTypeInfo: true, expectedBinding: "value :=", expectedType: "UNKNOWN",
		},
		{
			name: "nonvariable fallback excluded", source: "func f() { const value = 1; _ = value }",
			identifier: "value", omitTypeInfo: true, expectedType: "UNKNOWN",
		},
	}
	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			source := "package p; " + testCase.source
			fileSet, astFiles := astFromString("test.go", source)
			astFile := astFiles["test.go"]
			require.NoError(t, astFile.err)
			info := &types.Info{Defs: make(map[*ast.Ident]types.Object), Uses: make(map[*ast.Ident]types.Object)}
			if !testCase.omitTypeInfo {
				config := types.Config{Error: func(error) {}}
				// Type errors are intentional: identifier recovery must also work for invalid code.
				_, _ = config.Check("p", fileSet, []*ast.File{astFile.ast}, info)
			}
			mapper := NewSlangMapper(fileSet, astFile.ast, source, info, "", buildUsesByPos(info))
			identifierPos := token.Pos(strings.LastIndex(source, testCase.identifier) + 1)
			var identifier *ast.Ident
			ast.Inspect(astFile.ast, func(node ast.Node) bool {
				if ident, ok := node.(*ast.Ident); ok && ident.Pos() == identifierPos {
					identifier = ident
				}
				return true
			})
			require.NotNil(t, identifier)
			expectedID := 0
			if testCase.expectedBinding != "" {
				expectedID = strings.Index(source, testCase.expectedBinding) + 1
			}
			identifierInfo := mapper.getIdentifierInfo(identifier)
			assert.Equal(t, expectedID, identifierInfo.Id)
			assert.Equal(t, testCase.expectedType, identifierInfo.Type)
			assert.Equal(t, "UNKNOWN", identifierInfo.Package)
		})
	}
}

// Update all .prototxt files in resources/ast from all .go.source files
// Add "Test_" before to run in IDE
func fix_all_go_files_test_automatically(t *testing.T) {
	for _, file := range getAllGoFiles("resources/ast") {
		source, err := os.ReadFile(file)
		if err != nil {
			panic(err)
		}
		filename := strings.Replace(filepath.Base(file), ".source", "", 1)
		node, comment, tokens, errMsg := slangFromString(filename, string(source), "ModuleNameForTest")
		actual := protoTextOfTree(node, comment, tokens, errMsg)
		errWrite := os.WriteFile(goldenFileOf(file), []byte(actual), 0644)
		if errWrite != nil {
			panic(errWrite)
		}
	}
	t.Fatal("This test is only for local development and should not run in CI build.")
}

func Test_all_go_files(t *testing.T) {
	for _, file := range getAllGoFiles("resources/ast") {
		source, err := os.ReadFile(file)
		if err != nil {
			panic(err)
		}
		filename := strings.Replace(filepath.Base(file), ".source", "", 1)
		node, comment, tokens, errMsg := slangFromString(filename, string(source), "ModuleNameForTest")
		actual := protoTextOfTree(node, comment, tokens, errMsg)

		expectedData, err := os.ReadFile(goldenFileOf(file))
		if err != nil {
			panic(err)
		}

		assert.Equal(t, string(expectedData), actual, "Failed to match expected results for file: %#v\n", file)
	}
}

func getAllGoFiles(folder string) []string {
	var files []string

	err := filepath.Walk(folder, func(path string, info os.FileInfo, err error) error {
		if strings.HasSuffix(path, ".go.source") {
			files = append(files, path)
		}
		return nil
	})
	if err != nil {
		panic(err)
	}
	return files
}

func TestReadAstStringParsesFilesInSortedOrder(t *testing.T) {
	// More entries than a single map group, inserted in reverse order, so an unsorted map
	// iteration cannot coincidentally yield sorted order.
	fileNames := []string{"a.go", "b.go", "c.go", "d.go", "e.go", "f.go",
		"g.go", "h.go", "i.go", "j.go", "k.go", "l.go"}
	fileNameToContent := make(map[string]string)
	for i := len(fileNames) - 1; i >= 0; i-- {
		fileNameToContent[fileNames[i]] = "package main"
	}

	fileSet, astFileOrErrors := astFromStrings(fileNameToContent)

	previousBase := 0
	for _, fileName := range fileNames {
		astFileOrError := astFileOrErrors[fileName]
		if assert.NoError(t, astFileOrError.err) {
			base := fileSet.File(astFileOrError.ast.Pos()).Base()
			assert.Greater(t, base, previousBase, "positions of %s should follow the previous file in sorted order", fileName)
			previousBase = base
		}
	}
}

func TestIdentifierTypesResolveAliases(t *testing.T) {
	source := `package checks
type Value struct{}
type Alias = Value
type ChainedAlias = Alias
type PointerAlias = *Alias
type SliceAlias = []Alias
type KeyAlias = string
type Defined Value
type DefinedAlias = Defined
func example(value Alias, chained ChainedAlias, pointer *Alias, pointerAlias PointerAlias,
    slice []Alias, sliceAlias SliceAlias, array [2]Alias, mapping map[KeyAlias]*Alias,
    defined DefinedAlias) {}
`
	fileSet, files := astFromString("aliases.go", source)
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", "ModuleNameForTest", ".", GcExporter{})
	assert.Empty(t, errors)
	mapper := SlangMapper{info: info}
	actual := make(map[string]string)
	for identifier, object := range info.Defs {
		if _, ok := object.(*types.Var); ok {
			actual[identifier.Name] = mapper.getIdentifierInfo(identifier).Type
		}
	}
	assert.Equal(t, map[string]string{
		"value":        "checks.Value",
		"chained":      "checks.Value",
		"pointer":      "*checks.Value",
		"pointerAlias": "*checks.Value",
		"slice":        "[]checks.Value",
		"sliceAlias":   "[]checks.Value",
		"array":        "[2]checks.Value",
		"mapping":      "map[string]*checks.Value",
		"defined":      "checks.Defined",
	}, actual)
}

func TestImportPathConversion(t *testing.T) {
	tests := []struct {
		name    string
		literal string
		content string
	}{
		{"interpreted", `"fmt"`, "fmt"},
		{"raw", "`fmt`", "fmt"},
		{"hexadecimal", `"\x66mt"`, "fmt"},
		{"octal", `"\146mt"`, "fmt"},
		{"unicode", `"example.com/caf\u00e9"`, "example.com/café"},
		{"utf8_bytes", `"example.com/caf\xc3\xa9"`, "example.com/café"},
		{"raw_backslash", "`\\x66mt`", `\x66mt`},
		{"invalid_utf8", `"\x80\x80"`, "�"},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			root, comments, tokens, errMsg := slangFromString("imports.go", "package main\nimport "+test.literal+"\n", "ModuleNameForTest")
			if !assert.Nil(t, errMsg) {
				return
			}
			tree := buildProtoTree(root, comments, tokens, errMsg)
			declarations := tree.GetRoot().GetTopLevel().GetDeclarations()
			path := declarations[1].GetImportDeclaration().GetChildren()[1].GetImportSpecification().GetPath().GetStringLiteral()
			if assert.NotNil(t, path, "import paths must be string-literal nodes") {
				assert.Equal(t, test.literal, path.GetValue())
				assert.Equal(t, test.content, path.GetContent())
			}
			_, err := proto.Marshal(tree)
			assert.NoError(t, err, "decoded import paths must remain serializable")
		})
	}
}
