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
	"go/ast"
	"go/parser"
	"go/token"
	"os"
	"sort"
	"strconv"
	"strings"
	"testing"

	"github.com/SonarSource/slang/sonar-go-to-slang/proto/slang"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// nodeConstructors are the functions that give a node its SlangType. Every SlangType the mapper can
// produce is the slangType argument of one of their call sites, so scanning those calls yields the
// whole set without anyone having to keep a list of it by hand.
var nodeConstructors = []string{"createNode", "createNodeWithChildren", "createExpectedNode", "createLeafNode"}

// TestEverySlangTypeOfTheMapperIsEncoded is the completeness check the Go encoder cannot get from the
// compiler. buildProtoNode dispatches on a string, so a SlangType that protoSlang.go does not handle
// is not a build error: it panics at run time, and encodeFile turns that panic into "cannot encode the
// tree of X", which drops every issue of every file holding the construct. The Java side is safe by
// construction — its switch over KindCase has no default, so the compiler catches a missing kind — and
// this test is what links the two: add a SlangType to the mapper without a case in protoSlang.go, a
// message in slang.proto and a branch in ProtoTree.tree, and it fails here instead of in production.
func TestEverySlangTypeOfTheMapperIsEncoded(t *testing.T) {
	produced := slangTypesProducedByMapper(t)
	require.NotEmpty(t, produced, "the scan found no SlangType, so it no longer reads the mapper")

	var unencoded []string
	for _, slangType := range produced {
		if !isEncodedSlangType(slangType) {
			unencoded = append(unencoded, slangType)
		}
	}
	assert.Empty(t, unencoded, "the mapper produces SlangTypes that toProtoSlang cannot encode; each one needs a case in "+
		"setProtoExpressionKind or setProtoStatementKind, a message in proto/slang/slang.proto and a branch in ProtoTree.tree")
}

// TestProtoKindOfSlangTypeMatchesTheMapper keeps the table the other tests compare against honest: it
// has to name exactly the SlangTypes the mapper produces, no more and no less.
func TestProtoKindOfSlangTypeMatchesTheMapper(t *testing.T) {
	declared := make([]string, 0, len(protoKindOfSlangType))
	for slangType := range protoKindOfSlangType {
		declared = append(declared, slangType)
	}
	sort.Strings(declared)

	assert.Equal(t, slangTypesProducedByMapper(t), declared)
}

// TestEveryEncodedSlangTypeSetsItsDeclaredKind encodes a bare node of every SlangType and checks the
// oneof it lands in, so that a case added to the wrong one of the two dispatch functions, or wired to
// the wrong message, fails here rather than silently changing the AST the analyzer receives.
func TestEveryEncodedSlangTypeSetsItsDeclaredKind(t *testing.T) {
	for slangType, kind := range protoKindOfSlangType {
		t.Run(slangType, func(t *testing.T) {
			node := &Node{SlangType: slangType, SlangField: map[string]interface{}{}}

			assert.Equal(t, kind, protoKindOf(t, buildProtoNode(node)))
		})
	}
}

// isEncodedSlangType reports whether buildProtoNode can encode a node of that SlangType, which is what
// the two dispatch functions answer between them.
func isEncodedSlangType(slangType string) bool {
	empty := map[string]interface{}{}
	return setProtoExpressionKind(&slang.Node{}, slangType, empty) || setProtoStatementKind(&slang.Node{}, slangType, empty)
}

// slangTypesProducedByMapper parses the sources of the package and returns, sorted, every SlangType
// passed to one of the nodeConstructors. The index of the slangType parameter is read from each
// declaration rather than hard-coded, so that reordering a signature cannot make the scan look at the
// wrong argument and silently find nothing.
func slangTypesProducedByMapper(t *testing.T) []string {
	t.Helper()
	fileSet := token.NewFileSet()
	files := parseMapperSources(t, fileSet)

	slangTypeIndexes := slangTypeParameterIndexes(files)
	require.Len(t, slangTypeIndexes, len(nodeConstructors), "a node constructor was renamed or lost its slangType parameter")
	scan := &mapperScan{
		t:                t,
		fileSet:          fileSet,
		constants:        stringConstants(files),
		slangTypeIndexes: slangTypeIndexes,
		produced:         map[string]bool{},
	}

	for _, file := range files {
		ast.Inspect(file, scan.visit)
	}

	result := make([]string, 0, len(scan.produced))
	for slangType := range scan.produced {
		result = append(result, slangType)
	}
	sort.Strings(result)
	return result
}

// mapperScan carries what one scan over the mapper sources needs, so that the walk can be read one
// node kind at a time rather than as a single closure.
type mapperScan struct {
	t                *testing.T
	fileSet          *token.FileSet
	constants        map[string]string
	slangTypeIndexes map[string]int
	produced         map[string]bool
}

// visit is the ast.Inspect callback: it records the SlangType of the node, when the node names one.
func (s *mapperScan) visit(n ast.Node) bool {
	// A mapper that picks the type of a node from its token, such as mapBasicLitImpl, assigns it
	// to a slangType variable and passes that; the literals it can hold are the types it produces.
	if assigned, isAssignment := assignedSlangType(n); isAssignment {
		if slangType, ok := literalValue(assigned); ok {
			s.produced[slangType] = true
		}
	} else if call, isCall := n.(*ast.CallExpr); isCall {
		s.visitCall(call)
	}
	return true
}

// visitCall records the slangType a call to a node constructor passes, and fails the test on one the
// scan cannot see.
func (s *mapperScan) visitCall(call *ast.CallExpr) {
	index, isConstructor := s.slangTypeIndexes[calleeName(call.Fun)]
	if !isConstructor || index >= len(call.Args) {
		return
	}
	argument := call.Args[index]
	if slangType, resolved := stringValue(argument, s.constants); resolved {
		s.produced[slangType] = true
	} else if !isNamedSlangType(argument) {
		// A slangType variable, and a constructor forwarding its own slangType parameter, carry
		// no new value: what they can hold is collected above, or at the call site they were
		// given it at. Anything else is a value the scan cannot see, which would leave this
		// whole check silently incomplete.
		s.t.Errorf("%s: the slangType of this call is neither a literal nor a slangType variable, so the scan cannot see it",
			s.fileSet.Position(call.Pos()))
	}
}

// parseMapperSources parses the non-test sources of the package, in the order the directory lists
// them, which is enough as the scan looks at every file anyway.
func parseMapperSources(t *testing.T, fileSet *token.FileSet) []*ast.File {
	t.Helper()
	entries, err := os.ReadDir(".")
	require.NoError(t, err)
	var files []*ast.File
	for _, entry := range entries {
		name := entry.Name()
		if entry.IsDir() || !strings.HasSuffix(name, ".go") || strings.HasSuffix(name, "_test.go") {
			continue
		}
		file, err := parser.ParseFile(fileSet, name, nil, 0)
		require.NoError(t, err)
		files = append(files, file)
	}
	return files
}

// slangTypeParameterIndexes returns, for each node constructor, the position of its slangType
// parameter among the flattened parameters.
func slangTypeParameterIndexes(files []*ast.File) map[string]int {
	wanted := map[string]bool{}
	for _, name := range nodeConstructors {
		wanted[name] = true
	}
	indexes := map[string]int{}
	for _, file := range files {
		for _, decl := range file.Decls {
			funcDecl, isFunc := decl.(*ast.FuncDecl)
			if !isFunc || !wanted[funcDecl.Name.Name] {
				continue
			}
			if index, found := slangTypeParameterIndex(funcDecl); found {
				indexes[funcDecl.Name.Name] = index
			}
		}
	}
	return indexes
}

// slangTypeParameterIndex returns the position of the slangType parameter of one declaration among
// its flattened parameters.
func slangTypeParameterIndex(funcDecl *ast.FuncDecl) (int, bool) {
	index := 0
	for _, field := range funcDecl.Type.Params.List {
		for _, name := range field.Names {
			if name.Name == "slangType" {
				return index, true
			}
			index++
		}
	}
	return 0, false
}

// stringConstants collects the package-level string constants, so that a slangType given as a named
// constant, such as nativeSlangType, is resolved like a literal.
func stringConstants(files []*ast.File) map[string]string {
	constants := map[string]string{}
	for _, file := range files {
		for _, decl := range file.Decls {
			if genDecl, isGen := decl.(*ast.GenDecl); isGen && genDecl.Tok == token.CONST {
				addStringConstants(constants, genDecl)
			}
		}
	}
	return constants
}

// addStringConstants records every string literal one const declaration binds to a name.
func addStringConstants(constants map[string]string, genDecl *ast.GenDecl) {
	for _, spec := range genDecl.Specs {
		valueSpec, isValue := spec.(*ast.ValueSpec)
		if !isValue {
			continue
		}
		for i, name := range valueSpec.Names {
			if i >= len(valueSpec.Values) {
				continue
			}
			if value, ok := literalValue(valueSpec.Values[i]); ok {
				constants[name.Name] = value
			}
		}
	}
}

// assignedSlangType returns the value a statement assigns to a variable named slangType, if that is
// what it does. Both "slangType = x" and "slangType := x" count, as the mappers use either.
func assignedSlangType(n ast.Node) (ast.Expr, bool) {
	assignment, isAssignment := n.(*ast.AssignStmt)
	if !isAssignment || len(assignment.Lhs) != len(assignment.Rhs) {
		return nil, false
	}
	for i, target := range assignment.Lhs {
		if isNamedSlangType(target) {
			return assignment.Rhs[i], true
		}
	}
	return nil, false
}

func isNamedSlangType(expr ast.Expr) bool {
	ident, isIdent := expr.(*ast.Ident)
	return isIdent && ident.Name == "slangType"
}

func calleeName(fun ast.Expr) string {
	switch callee := fun.(type) {
	case *ast.Ident:
		return callee.Name
	case *ast.SelectorExpr:
		return callee.Sel.Name
	default:
		return ""
	}
}

func stringValue(expr ast.Expr, constants map[string]string) (string, bool) {
	if value, ok := literalValue(expr); ok {
		return value, true
	}
	if ident, isIdent := expr.(*ast.Ident); isIdent {
		value, known := constants[ident.Name]
		return value, known
	}
	return "", false
}

func literalValue(expr ast.Expr) (string, bool) {
	literal, isLiteral := expr.(*ast.BasicLit)
	if !isLiteral || literal.Kind != token.STRING {
		return "", false
	}
	value, err := strconv.Unquote(literal.Value)
	return value, err == nil
}
