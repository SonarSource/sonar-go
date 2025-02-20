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

// This program generates 'goparser_generated.go'. It can be invoked by running "go generate"

package main

import (
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"go/ast"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func slangFromString(source string, nodeQueryPath string) (*Node, []*Node, []*Token) {
	fileSet, astNode := astFromString(source)
	info := new(types.Info)
	return toSlangTree(fileSet, astNode, source, info)
}

func astFromString(source string) (fileSet *token.FileSet, astFile *ast.File) {
	fileSet = token.NewFileSet()
	astFile, err := readAstString(fileSet, "main.go", source)
	if err != nil {
		panic(err)
	}
	return
}

// Update all .txt files in resources/ast from all .go.source files
// Add "Test_" before to run in IDE
func fix_all_go_files_test_automatically(t *testing.T) {
	for _, file := range getAllGoFiles("resources/ast") {
		source, err := os.ReadFile(file)
		if err != nil {
			panic(err)
		}

		actual := toJsonSlang(slangFromString(string(source), ""))
		d1 := []byte(actual)
		errWrite := os.WriteFile(strings.Replace(file, "go.source", "json", 1), d1, 0644)
		if errWrite != nil {
			panic(errWrite)
		}
	}
}

func Test_all_go_files(t *testing.T) {
	for _, file := range getAllGoFiles("resources/ast") {
		source, err := os.ReadFile(file)
		if err != nil {
			panic(err)
		}
		actual := toJsonSlang(slangFromString(string(source), ""))

		var jsonActual interface{}
		err1 := json.Unmarshal([]byte(actual), &jsonActual)
		if err1 != nil {
			panic(err1)
		}

		expectedData, err := os.ReadFile(strings.Replace(file, "go.source", "json", 1))
		if err != nil {
			panic(err)
		}
		var jsonExpected map[string]interface{}

		err2 := json.Unmarshal(expectedData, &jsonExpected)
		if err2 != nil {
			panic(err2)
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
