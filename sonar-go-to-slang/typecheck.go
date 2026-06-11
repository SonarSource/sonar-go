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
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"io/fs"
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
	gcExportDataDir   string
	moduleName        string
	moduleBaseDir     string
	debugTypeCheck    bool
	gcExporter        GcExporter
	importPathToOFile map[string]string
	importCache       map[string]*types.Package
}

func (li *localImporter) Import(path string) (*types.Package, error) {
	if pkg, ok := li.importCache[path]; ok && pkg.Complete() {
		return pkg, nil
	}
	if exportDataFileName, ok := packageExportData[path]; ok {
		// In embedded filesystem, the path separator is always '/', even on Windows.
		return li.getPackageFromExportData(PackageExportDataDir+"/"+exportDataFileName, path), nil
	}
	return li.getPackageFromLocalCodeExportData(path), nil
}

func (li *localImporter) getPackageFromLocalCodeExportData(path string) *types.Package {
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Search for local Gc Export Data for \"%s\" package\n", path)
	}
	if li.gcExportDataDir == "" {
		return getEmptyPackage(path)
	}

	oFileName := getPackageName(path) + ".o"

	sameModulePath := filepath.Join(li.gcExportDataDir, li.moduleBaseDir, path, oFileName)
	if pkg, found := li.tryLoadExportData(sameModulePath, path); found {
		return pkg
	}

	if pkg, found := li.scanOtherModuleSubdirs(path, oFileName); found {
		return pkg
	}

	return getEmptyPackage(path)
}

// scanOtherModuleSubdirs searches gcExportDataDir recursively (excluding the current
// module's own subtree) for a matching .o file. This resolves cross-module imports
// where the imported package belongs to a different module in the same project.
func (li *localImporter) scanOtherModuleSubdirs(path, oFileName string) (*types.Package, bool) {
	cachedOFile, ok := li.importPathToOFile[path]
	if ok {
		return li.tryLoadExportData(cachedOFile, path)
	}

	targetRelPath := filepath.Join(path, oFileName)
	ownBaseDir := filepath.FromSlash(li.moduleBaseDir)
	if ownBaseDir == "." {
		ownBaseDir = ""
	}

	var result *types.Package
	_ = filepath.WalkDir(li.gcExportDataDir, func(walkPath string, d fs.DirEntry, err error) error {
		if err != nil {
			return nil
		}
		if d.IsDir() {
			return nil
		}
		relPath, relErr := filepath.Rel(li.gcExportDataDir, walkPath)
		if relErr != nil {
			return nil
		}
		var moduleBaseDir string
		switch {
		case relPath == targetRelPath:
			moduleBaseDir = ""
		case strings.HasSuffix(relPath, string(os.PathSeparator)+targetRelPath):
			moduleBaseDir = relPath[:len(relPath)-len(targetRelPath)-1]
		default:
			return nil
		}
		if moduleBaseDir == ownBaseDir {
			return nil
		}
		if pkg, found := li.tryLoadExportData(walkPath, path); found {
			result = pkg
			li.importPathToOFile[path] = walkPath
			// Stop on first match. If multiple modules export the same package path this is
			// ambiguous, but resolving it would require go.mod information that  the importer does not have.
			return filepath.SkipAll
		}
		return nil
	})

	if result != nil {
		return result, true
	}
	return nil, false
}

func (li *localImporter) tryLoadExportData(filePath, path string) (*types.Package, bool) {
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		return nil, false
	}
	file, err := os.Open(filePath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file \"%s\": %s\n", filePath, err)
		pkg := getEmptyPackage(path)
		return pkg, true
	}
	pkg := li.gcExporter.getPackageFromFile(file, path, li.importCache)
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Found Gc Export Data for \"%s\" package at %s\n", path, filePath)
	}
	return pkg, true
}

func getPackageName(packagePath string) string {
	packageName := packagePath
	lastSlashIndex := strings.LastIndex(packagePath, "/")
	if lastSlashIndex != -1 {
		packageName = packagePath[lastSlashIndex+1:]
	}
	return packageName
}

func (li *localImporter) getPackageFromExportData(exportDataFileName, path string) *types.Package {
	file, err := packages.Open(exportDataFileName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", exportDataFileName, err)
		return getEmptyPackage(path)
	}
	return li.gcExporter.getPackageFromFile(file, path, li.importCache)
}

func getEmptyPackage(path string) *types.Package {
	pkg := types.NewPackage(path, path)
	pkg.MarkComplete()
	return pkg
}

func typeCheckAst(
	fileSet *token.FileSet,
	astFiles map[string]AstFileOrError,
	debugTypeCheck bool,
	gcExportDataDir string,
	moduleName string,
	moduleBaseDir string,
	gcExporter GcExporter,
) (*types.Info, []error) {
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
				gcExportDataDir:   gcExportDataDir,
				moduleName:        moduleName,
				moduleBaseDir:     moduleBaseDir,
				debugTypeCheck:    debugTypeCheck,
				gcExporter:        gcExporter,
				importPathToOFile: make(map[string]string),
				importCache:       make(map[string]*types.Package),
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
