package main

import (
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"strings"
)

// PackageExportDataDir is also hardcoded in the go:embed directive below.
const PackageExportDataDir = "packages"

// This compiler directive embeds the package export data (i.e. the "packages" directory) into the Go executable
//
//go:embed packages
var packages embed.FS

type localImporter struct {
	gcExportDataDir string
}

func (li *localImporter) Import(path string) (*types.Package, error) {
	if exportDataFileName, ok := packageExportData[path]; ok {
		// In embedded filesystem, the path separator is always '/', even on Windows.
		return getPackageFromExportData(PackageExportDataDir+"/"+exportDataFileName, path)
	} else {
		return li.getPackageFromLocalCodeExportData(path)
	}
}

func (li *localImporter) getPackageFromLocalCodeExportData(path string) (*types.Package, error) {
	if li.gcExportDataDir != "" {
		//TODO SONARGO-576 Go executable should read all gcexportdata of user's code
		for _, filePath := range li.getFiles() {
			file, err := os.Open(filePath)
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", filePath, err)
				return getEmptyPackage(path), nil
			}
			pkg, _ := getPackageFromFile(file, path)
			if pkg != nil {
				if pkg.Name() == path {
					return pkg, nil
				}
			}
			return pkg, nil
		}
	}
	return getEmptyPackage(path), nil
}

func (li *localImporter) getFiles() []string {
	var files []string

	err := filepath.Walk(li.gcExportDataDir, func(path string, info os.FileInfo, err error) error {
		if strings.HasSuffix(path, ".o") {
			files = append(files, path)
		}
		return nil
	})
	if err != nil {
		panic(err)
	}
	return files
}

func getPackageFromExportData(exportDataFileName string, path string) (*types.Package, error) {
	file, err := packages.Open(exportDataFileName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", exportDataFileName, err)
		return getEmptyPackage(path), nil
	}
	return getPackageFromFile(file, path)
}

func getEmptyPackage(path string) *types.Package {
	pkg := types.NewPackage(path, path)
	pkg.MarkComplete()
	return pkg
}

func typeCheckAst(path string, fileSet *token.FileSet, astFile *ast.File, debugTypeCheck bool, gcExportDataDir string) (*types.Info, error) {
	conf := types.Config{
		Importer: &localImporter{
			gcExportDataDir: gcExportDataDir,
		},
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

	// We pass the file name which correspond to the name of the package, in order to have local type/package
	// named after this package names.
	_, err := conf.Check(astFile.Name.Name, fileSet, []*ast.File{astFile}, info)

	return info, err
}
