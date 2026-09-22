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
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
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
	// ctx carries the enclosing type-check span, because Import's signature is fixed by types.Importer
	// and cannot take one.
	ctx             context.Context
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
	_, done := span(li.ctx, "import.resolve", "importPath", path)
	// gcexportdata.Read populates importCache with everything it reads transitively, so the growth
	// across one resolution is how many packages this single import actually decoded.
	cachedBefore := len(li.importCache)
	if pkg, ok := li.importCache[path]; ok && pkg.Complete() {
		done("source", "cache")
		return pkg, nil
	}
	if exportDataFileName, ok := packageExportData[path]; ok {
		// In embedded filesystem, the path separator is always '/', even on Windows.
		pkg, oFileBytes := li.getPackageFromExportData(PackageExportDataDir+"/"+exportDataFileName, path)
		done("source", "embedded", "oFileBytes", oFileBytes, "transitivePackages", len(li.importCache)-cachedBefore)
		return pkg, nil
	}
	pkg, source, oFileBytes := li.getPackageFromLocalCodeExportData(path)
	done("source", source, "oFileBytes", oFileBytes, "transitivePackages", len(li.importCache)-cachedBefore)
	return pkg, nil
}

// getPackageFromLocalCodeExportData also reports which lookup produced the package and the size of the
// .o file it was read from, so the caller can annotate its span without repeating the lookup.
func (li *localImporter) getPackageFromLocalCodeExportData(path string) (*types.Package, string, int64) {
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Search for local Gc Export Data for \"%s\" package\n", path)
	}
	if li.gcExportDataDir == "" {
		return getEmptyPackage(path), "empty", 0
	}

	oFileName := getPackageName(path) + ".o"

	sameModulePath := filepath.Join(li.gcExportDataDir, li.moduleBaseDir, path, oFileName)
	if pkg, oFileBytes, found := li.tryLoadExportData(sameModulePath, path); found {
		return pkg, "same-module", oFileBytes
	}

	if pkg, oFileBytes, found := li.scanOtherModuleSubdirs(path); found {
		return pkg, "cross-module", oFileBytes
	}

	return getEmptyPackage(path), "empty", 0
}

// scanOtherModuleSubdirs resolves a cross-module import: an imported package that belongs to a
// different module in the same project. It looks the import path up in a shared, lazily-built
// index of gcExportDataDir (see crossModuleIndex), excluding the current module's own subtree,
// instead of walking the whole directory tree on every call.
func (li *localImporter) scanOtherModuleSubdirs(path string) (*types.Package, int64, bool) {
	if li.crossIndex == nil {
		// No shared index was injected (e.g. in unit tests): fall back to a per-importer one.
		li.crossIndex = &crossModuleIndex{dir: li.gcExportDataDir}
	}

	ownBaseDir := filepath.FromSlash(li.moduleBaseDir)
	if ownBaseDir == "." {
		ownBaseDir = ""
	}

	filePath, found := li.crossIndex.lookup(li.ctx, path, ownBaseDir)
	if !found {
		return nil, 0, false
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
	// oFiles counts the .o files the walk registered, which is what makes its duration comparable
	// across corpora.
	oFiles int
	byPath map[string][]oFileCandidate
}

// lookup returns the path of the first candidate .o file for importPath that does not belong to
// ownBaseDir (the importing module's own subtree, which is resolved earlier via the same-module
// path). The index is built on first use.
func (idx *crossModuleIndex) lookup(ctx context.Context, importPath, ownBaseDir string) (string, bool) {
	idx.once.Do(func() { idx.buildTraced(ctx) })
	for _, candidate := range idx.byPath[importPath] {
		if candidate.baseDir != ownBaseDir {
			return candidate.filePath, true
		}
	}
	return "", false
}

// buildTraced isolates the one-time directory walk, which is otherwise charged to whichever import
// happens to trigger it and shows up as an outlier resolution rather than as the fixed cost it is.
func (idx *crossModuleIndex) buildTraced(ctx context.Context) {
	_, done := span(ctx, "crossindex.build", "dir", idx.dir)
	idx.build()
	done("oFiles", idx.oFiles, "importPaths", len(idx.byPath))
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
		idx.oFiles++
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

// tryLoadExportData also returns the size of the .o file it read, which the stat below yields for free.
func (li *localImporter) tryLoadExportData(filePath, path string) (*types.Package, int64, bool) {
	stat, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		return nil, 0, false
	}
	var oFileBytes int64
	if stat != nil {
		oFileBytes = stat.Size()
	}
	file, err := os.Open(filePath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file \"%s\": %s\n", filePath, err)
		pkg := getEmptyPackage(path)
		return pkg, oFileBytes, true
	}
	pkg := li.gcExporter.getPackageFromFile(file, path, li.importCache)
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Found Gc Export Data for \"%s\" package at %s\n", path, filePath)
	}
	return pkg, oFileBytes, true
}

func getPackageName(packagePath string) string {
	packageName := packagePath
	lastSlashIndex := strings.LastIndex(packagePath, "/")
	if lastSlashIndex != -1 {
		packageName = packagePath[lastSlashIndex+1:]
	}
	return packageName
}

// getPackageFromExportData also returns the size of the embedded .o it read. Embedded lookups are the
// most expensive resolution class in the corpus, so their byte counts are what make that cost divisible.
func (li *localImporter) getPackageFromExportData(exportDataFileName, path string) (*types.Package, int64) {
	file, err := packages.Open(exportDataFileName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", exportDataFileName, err)
		return getEmptyPackage(path), 0
	}
	oFileBytes := fileSizeIfTracing(file)
	return li.gcExporter.getPackageFromFile(file, path, li.importCache), oFileBytes
}

func getEmptyPackage(path string) *types.Package {
	pkg := types.NewPackage(path, path)
	pkg.MarkComplete()
	return pkg
}

func typeCheckAst(
	ctx context.Context,
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
		packageCtx, packageDone := span(ctx, "typecheck.package", "packageName", packageName, "fileCount", len(files))
		typeErrorCount := 0
		conf := types.Config{
			Importer: &localImporter{
				ctx:             packageCtx,
				gcExportDataDir: gcExportDataDir,
				moduleName:      moduleName,
				moduleBaseDir:   moduleBaseDir,
				debugTypeCheck:  debugTypeCheck,
				gcExporter:      gcExporter,
				importCache:     make(map[string]*types.Package),
				crossIndex:      crossIndex,
			},
			Error: func(err error) {
				typeErrorCount++
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
		_, err := conf.Check(packageName, fileSet, sortedAstFiles(files), info)
		if err != nil {
			errors = append(errors, err)
		}
		packageDone("typeErrorCount", typeErrorCount)
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

// Sort by file path to make type-checking results deterministic when declarations conflict.
func sortedAstFiles(astFiles map[string]AstFileOrError) []*ast.File {
	fileNames := make([]string, 0, len(astFiles))
	for fileName := range astFiles {
		fileNames = append(fileNames, fileName)
	}
	sort.Strings(fileNames)

	files := make([]*ast.File, 0, len(astFiles))
	for _, fileName := range fileNames {
		if astFile := astFiles[fileName].ast; astFile != nil {
			files = append(files, astFile)
		}
	}
	return files
}
