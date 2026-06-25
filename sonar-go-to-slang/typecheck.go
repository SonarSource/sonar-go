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
	"sync"
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
	moduleBaseDir   string
	debugTypeCheck  bool
	gcExporter      GcExporter
	importCache     map[string]*types.Package
	// crossIndex is a shared, lazily-built index of gcExportDataDir used to resolve cross-module
	// imports. It is shared across all per-package importers of a single run so the directory is
	// walked at most once.
	crossIndex *crossModuleIndex
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

	if pkg, found := li.scanOtherModuleSubdirs(path); found {
		return pkg
	}

	return getEmptyPackage(path)
}

// scanOtherModuleSubdirs resolves a cross-module import: an imported package that belongs to a
// different module in the same project. It looks the import path up in a shared, lazily-built
// index of gcExportDataDir (see crossModuleIndex), excluding the current module's own subtree,
// instead of walking the whole directory tree on every call.
func (li *localImporter) scanOtherModuleSubdirs(path string) (*types.Package, bool) {
	if li.crossIndex == nil {
		// No shared index was injected (e.g. in unit tests): fall back to a per-importer one.
		li.crossIndex = &crossModuleIndex{dir: li.gcExportDataDir}
	}

	ownBaseDir := filepath.FromSlash(li.moduleBaseDir)
	if ownBaseDir == "." {
		ownBaseDir = ""
	}

	filePath, found := li.crossIndex.lookup(path, ownBaseDir)
	if !found {
		return nil, false
	}
	return li.tryLoadExportData(filePath, path)
}

// oFileCandidate is a single .o file found under gcExportDataDir that can satisfy an import path.
type oFileCandidate struct {
	// baseDir is the module base directory (OS-separator form), relative to gcExportDataDir, that
	// the file lives under; "" for a root-level module. It is compared against the importing
	// module's own base dir to skip the current module's own subtree.
	baseDir string
	// filePath is the path to the .o file, as produced by filepath.WalkDir.
	filePath string
}

// crossModuleIndex is a one-time index of gcExportDataDir mapping an import path to every .o file
// that can satisfy it. The tree is walked at most once per analysis run and shared across all packages,
// making each cross-module lookup an O(1) map access.
type crossModuleIndex struct {
	dir  string
	once sync.Once
	// builds counts how many times the tree was walked; it must remain 1 per run and is asserted
	// by the performance regression test.
	builds int
	byPath map[string][]oFileCandidate
}

// lookup returns the path of the first candidate .o file for importPath that does not belong to
// ownBaseDir (the importing module's own subtree, which is resolved earlier via the same-module
// path). The index is built on first use.
func (idx *crossModuleIndex) lookup(importPath, ownBaseDir string) (string, bool) {
	idx.once.Do(idx.build)
	for _, candidate := range idx.byPath[importPath] {
		if candidate.baseDir != ownBaseDir {
			return candidate.filePath, true
		}
	}
	return "", false
}

// build performs the single directory walk. For each .o file laid out as
// <baseDir>/<importPath>/<packageName>.o (where packageName is the last segment of importPath), it
// registers the file under every suffix of its directory as a candidate import path, recording the
// stripped prefix as baseDir. Candidates are appended in WalkDir's lexical order, preserving the
// previous "first match wins" behaviour for ambiguous cross-module clashes.
func (idx *crossModuleIndex) build() {
	idx.builds++
	idx.byPath = make(map[string][]oFileCandidate)
	if idx.dir == "" {
		return
	}
	separator := string(os.PathSeparator)
	_ = filepath.WalkDir(idx.dir, func(walkPath string, d fs.DirEntry, err error) error {
		if err != nil || d.IsDir() {
			return nil
		}
		name := d.Name()
		if !strings.HasSuffix(name, ".o") {
			return nil
		}
		relPath, relErr := filepath.Rel(idx.dir, walkPath)
		if relErr != nil {
			return nil
		}
		dir := filepath.Dir(relPath)
		if dir == "." {
			// The file sits directly under gcExportDataDir, so no import path can match it.
			return nil
		}
		packageName := strings.TrimSuffix(name, ".o")
		parts := strings.Split(dir, separator)
		if parts[len(parts)-1] != packageName {
			// Not the <importPath>/<packageName>.o layout, so it cannot satisfy any import path.
			return nil
		}
		for j := range parts {
			importPath := strings.Join(parts[j:], "/")
			baseDir := strings.Join(parts[:j], separator)
			idx.byPath[importPath] = append(idx.byPath[importPath], oFileCandidate{baseDir: baseDir, filePath: walkPath})
		}
		return nil
	})
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

	// Shared across every per-package importer so gcExportDataDir is walked at most once per run.
	crossIndex := &crossModuleIndex{dir: gcExportDataDir}

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
				moduleBaseDir:   moduleBaseDir,
				debugTypeCheck:  debugTypeCheck,
				gcExporter:      gcExporter,
				importCache:     make(map[string]*types.Package),
				crossIndex:      crossIndex,
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
