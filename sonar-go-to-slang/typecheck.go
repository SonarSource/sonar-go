package main

import (
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"io/fs"
	"os"

	"golang.org/x/tools/go/gcexportdata"
)

// PackageExportDataDir is also hardcoded in the go:embed directive below.
const PackageExportDataDir = "packages"

// This compiler directive embeds the package export data (i.e. the "packages" directory) into the Go executable
//
//go:embed packages
var packages embed.FS

type localImporter struct{}

func (fi *localImporter) Import(path string) (*types.Package, error) {
	if exportDataFileName, ok := packageExportData[path]; ok {
		return getPackageFromExportData(exportDataFileName, path)
	}
	return getEmptyPackage(path), nil
}

func getPackageFromExportData(exportDataFileName string, path string) (*types.Package, error) {
	file, err := packages.Open(PackageExportDataDir + "/" + exportDataFileName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", exportDataFileName, err)
		return getEmptyPackage(path), nil
	}
	defer func(file fs.File) {
		_ = file.Close()
	}(file)

	imports := make(map[string]*types.Package)
	pkg, err := gcexportdata.Read(file, nil, imports, path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while reading %s export data: %s\n", path, err)
		return getEmptyPackage(path), nil
	}

	return pkg, nil
}

func getEmptyPackage(path string) *types.Package {
	pkg := types.NewPackage(path, path)
	pkg.MarkComplete()
	return pkg
}

func typeCheckAst(path string, fileSet *token.FileSet, astFile *ast.File, debugTypeCheck bool) (*types.Info, error) {
	conf := types.Config{
		Importer: &localImporter{},
		Error: func(err error) {
			if debugTypeCheck {
				fmt.Fprintf(os.Stderr, "Warning while type checking '%s': %s\n", path, err)
			}
			// Our current logic type checks only the types that are used in the rules, and "ignores" the rest.
			// It means that we expect many errors in the type checking process (missing types, undefined variables, etc).
			// In theory, we would like to log only errors that are related to the types that we support, in order to spot potential issues.
			// In practise, the message is often not enough to determine if the error is relevant or not.
			// Therefore, we don't log any error at the moment.
		},
	}

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

	_, err := conf.Check(path, fileSet, []*ast.File{astFile}, info)

	return info, err
}
