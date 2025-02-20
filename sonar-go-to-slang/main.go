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
	"flag"
	"fmt"
	"go/ast"
	"go/importer"
	"go/token"
	"go/types"
	"os"
)

type Params struct {
	dumpAst bool
	path    string
}

func parseArgs() Params {
	flag.Usage = func() {
		fmt.Printf("Usage: %s [options] source.go\n\n", os.Args[0])
		flag.PrintDefaults()
	}

	dumpAstFlag := flag.Bool("d", false, "dump ast (instead of JSON)")
	flag.Parse()
	var path string
	if len(flag.Args()) == 1 {
		path = flag.Args()[0]
	}

	return Params{
		dumpAst: *dumpAstFlag,
		path:    path,
	}
}

func main() {
	params := parseArgs()

	fileSet := token.NewFileSet()
	astFile, fileContent, _ := readAstFile(fileSet, params.path)

	var errs []error
	defaultConf := types.Config{
		Importer: importer.ForCompiler(fileSet, "gc", nil),
		Error: func(err error) {
			errs = append(errs, err)
		},
	}

	_, info, _ := checkTypes(defaultConf, params, fileSet, astFile)

	if len(errs) > 0 {
		fmt.Fprintf(os.Stderr, "Found some errors during type check")
		for _, e := range errs {
			fmt.Fprintf(os.Stderr, "%s", e.Error())
		}
	}

	if params.dumpAst {
		fmt.Println(render(astFile))
	} else {
		slangTree, comments, tokens := toSlangTree(fileSet, astFile, fileContent, info)
		fmt.Println(toJsonSlang(slangTree, comments, tokens))
	}
}

func checkTypes(defaultConf types.Config, params Params, fileSet *token.FileSet, astFile *ast.File) (*types.Package, *types.Info, error) {
	info := &types.Info{
		Types:        make(map[ast.Expr]types.TypeAndValue),
		Defs:         make(map[*ast.Ident]types.Object),
		Uses:         make(map[*ast.Ident]types.Object),
		Implicits:    make(map[ast.Node]types.Object),
		Selections:   make(map[*ast.SelectorExpr]*types.Selection),
		Scopes:       make(map[ast.Node]*types.Scope),
		InitOrder:    []*types.Initializer{},
		Instances:    make(map[*ast.Ident]types.Instance),
		FileVersions: make(map[*ast.File]string),
	}
	packages, err := defaultConf.Check(params.path, fileSet, []*ast.File{astFile}, info)
	return packages, info, err
}
