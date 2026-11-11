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
	"context"
	"go/ast"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIdentifierInfoPreservesBindingsInUnresolvedCalls(t *testing.T) {
	source := `package main
func f() {
	var callbacks struct{ check MissingCallback }
	value := 1
	{
		value := 2
		callbacks.check(value)
	}
	callbacks.check(value)
}`
	fileSet, files := astFromString("test.go", source)
	info, _ := typeCheckAst(context.Background(), fileSet, files, false, "", "", ".", GcExporter{})
	file := files["test.go"].ast
	mapper := NewSlangMapper(fileSet, file, source, info, "", buildUsesByPos(info))
	var values []*ast.Ident
	ast.Inspect(file, func(node ast.Node) bool {
		if identifier, ok := node.(*ast.Ident); ok && identifier.Name == "value" {
			values = append(values, identifier)
		}
		return true
	})
	if !assert.Len(t, values, 4) {
		return
	}
	assert.NotEqual(t, mapper.getIdentifierInfo(values[0]).Id, mapper.getIdentifierInfo(values[1]).Id)
	assert.Equal(t, mapper.getIdentifierInfo(values[1]).Id, mapper.getIdentifierInfo(values[2]).Id)
	assert.Equal(t, mapper.getIdentifierInfo(values[0]).Id, mapper.getIdentifierInfo(values[3]).Id)
	assert.Equal(t, "UNKNOWN", mapper.getIdentifierInfo(values[3]).Type)
}

func TestIdentifierInfoDoesNotBindUnresolvedSelectorNames(t *testing.T) {
	source := `package main
func f() {
	value := 1
	missing.value()
}`
	fileSet, files := astFromString("test.go", source)
	info, _ := typeCheckAst(context.Background(), fileSet, files, false, "", "", ".", GcExporter{})
	file := files["test.go"].ast
	mapper := NewSlangMapper(fileSet, file, source, info, "", buildUsesByPos(info))
	ast.Inspect(file, func(node ast.Node) bool {
		if selector, ok := node.(*ast.SelectorExpr); ok {
			assert.Zero(t, mapper.getIdentifierInfo(selector.Sel).Id)
		}
		return true
	})
}
