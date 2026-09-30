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
	"go/token"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"google.golang.org/protobuf/encoding/prototext"
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
