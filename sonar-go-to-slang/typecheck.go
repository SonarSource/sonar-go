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
	moduleName      string
	debugTypeCheck  bool
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
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Search for local Gc Export Data for \"%s\" package\n", path)
	}
	if li.gcExportDataDir != "" {
		var pkgToMerge []*types.Package
		for _, filePath := range li.getGcExportDataFilesForPackage(path) {
			file, err := os.Open(filePath)
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error while opening file \"%s\": %s\n", filePath, err)
				return getEmptyPackage(path), nil
			}
			pkg, _ := getPackageFromFile(file, path)
			if pkg != nil {
				if li.debugTypeCheck {
					fmt.Fprintf(os.Stderr, "- Found package \"%s\" in file \"%s\", number of elements: %d\n", pkg, filePath, pkg.Scope().Len())
				}
				pkgToMerge = append(pkgToMerge, pkg)
			}
		}
		return mergedPackage(pkgToMerge, path)
	}
	return getEmptyPackage(path), nil
}

func mergedPackage(pkgToMerge []*types.Package, path string) (*types.Package, error) {
	if len(pkgToMerge) == 0 {
		return getEmptyPackage(path), nil
	}
	if len(pkgToMerge) == 1 {
		return pkgToMerge[0], nil
	}
	result := pkgToMerge[0]

	for _, pkg := range pkgToMerge[1:] {
		for _, name := range pkg.Scope().Names() {
			if result.Scope().Lookup(name) == nil {
				result.Scope().Insert(pkg.Scope().Lookup(name))
			} else {
				fmt.Fprintf(os.Stderr, "There is a colision in package: \"%s\" for name: \"%s\"\n", path, name)
			}
		}
	}
	return result, nil
}

func (li *localImporter) getGcExportDataFilesForPackage(packagePath string) []string {
	var files []string
	dir := filepath.Join(li.gcExportDataDir, packagePath)
	err := filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		lastPathSep := strings.LastIndex(path, string(os.PathSeparator))
		if lastPathSep == len(dir) && strings.HasSuffix(path, ".o") {
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

func typeCheckAst(fileSet *token.FileSet, astFiles map[string]AstFileOrError, debugTypeCheck bool, gcExportDataDir string, moduleName string) (*types.Info, []error) {
	astFilesPerPackage := groupFilesPerPackageName(astFiles)
	errors := make([]error, 0)

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

	for packageName, files := range astFilesPerPackage {
		fmt.Fprintf(os.Stderr, "Processing package: \"%s\"\n", packageName)
		conf := types.Config{
			Importer: &localImporter{
				gcExportDataDir: gcExportDataDir,
				moduleName:      moduleName,
				debugTypeCheck:  debugTypeCheck,
			},
			Error: func(err error) {
				if debugTypeCheck {
					fmt.Fprintf(os.Stderr, "Warning while type checking for package: \"%s\": %s\n", packageName, err)
				}
				// Our current logic type checks only the types that are used in the rules, and "ignores" the rest.
				// It means that we expect many errors in the type checking process (missing types, undefined variables, etc).
				// In theory, we would like to log only errors that are related to the types that we support, in order to spot potential issues.
				// In practise, the message is often not enough to determine if the error is relevant or not.
				// Therefore, we don't log any error at the moment.
			},
		}

		// We pass the file name which correspond to the name of the package, in order to have local type/package
		// named after this package names.
		_, err := conf.Check(packageName, fileSet, mapToSlice(files), info)
		if err != nil {
			errors = append(errors, err)
		}
	}

	return info, errors
}

func groupFilesPerPackageName(astFiles map[string]AstFileOrError) map[string]map[string]AstFileOrError {
	filesPerPackage := make(map[string]map[string]AstFileOrError)
	for k, v := range astFiles {
		if v.ast != nil {
			if v.ast.Name != nil && v.ast.Name.Name != "" {
				packageName := v.ast.Name.Name
				if _, ok := filesPerPackage[packageName]; !ok {
					filesPerPackage[packageName] = make(map[string]AstFileOrError)
				}
				filesPerPackage[packageName][k] = v
			}
		}
	}
	return filesPerPackage
}

func mapToSlice(astFiles map[string]AstFileOrError) []*ast.File {
	files := make([]*ast.File, 0, len(astFiles))
	for _, v := range astFiles {
		if v.ast != nil {
			files = append(files, v.ast)
		}
	}
	return files
}
