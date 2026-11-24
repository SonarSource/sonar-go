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
	"strconv"
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

// importerFunc implements the single-method types.Importer interface with a function value,
// so localImporter.Import can take a context that the fixed interface signature cannot take.
type importerFunc func(path string) (*types.Package, error)

func (f importerFunc) Import(path string) (*types.Package, error) { return f(path) }

// typesImporter adapts li.Import to types.Importer, closing over ctx so every import resolved
// while type-checking one package attaches to that package's tracing span.
func (li *localImporter) typesImporter(ctx context.Context) types.Importer {
	return importerFunc(func(path string) (*types.Package, error) {
		return li.Import(ctx, path)
	})
}

func (li *localImporter) Import(ctx context.Context, path string) (*types.Package, error) {
	ctx, done := span(ctx, "import.resolve", "importPath", path)
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
	pkg, source, oFileBytes := li.getPackageFromLocalCodeExportData(ctx, path)
	done("source", source, "oFileBytes", oFileBytes, "transitivePackages", len(li.importCache)-cachedBefore)
	return pkg, nil
}

// getPackageFromLocalCodeExportData also reports which lookup produced the package and the size of the
// .o file it was read from, so the caller can annotate its span without repeating the lookup.
func (li *localImporter) getPackageFromLocalCodeExportData(ctx context.Context, path string) (*types.Package, string, int64) {
	if li.debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Search for local Gc Export Data for \"%s\" package\n", path)
	}
	if li.gcExportDataDir == "" {
		return getEmptyPackage(path), "empty", 0
	}

	sameModulePath := packageExportPath(filepath.Join(li.gcExportDataDir, li.moduleBaseDir), "", path, "")
	if pkg, oFileBytes, found := li.tryLoadExportData(sameModulePath, path); found {
		return pkg, "same-module", oFileBytes
	}

	if pkg, oFileBytes, found := li.scanOtherModuleSubdirs(ctx, path); found {
		return pkg, "cross-module", oFileBytes
	}

	return getEmptyPackage(path), "empty", 0
}

// scanOtherModuleSubdirs resolves a cross-module import: an imported package that belongs to a
// different module in the same project. It looks the import path up in a shared, lazily-built
// index of gcExportDataDir (see crossModuleIndex), excluding the current module's own subtree,
// instead of walking the whole directory tree on every call.
func (li *localImporter) scanOtherModuleSubdirs(ctx context.Context, path string) (*types.Package, int64, bool) {
	if li.crossIndex == nil {
		// No shared index was injected (e.g. in unit tests): fall back to a per-importer one.
		li.crossIndex = &crossModuleIndex{dir: li.gcExportDataDir}
	}

	ownBaseDir := filepath.FromSlash(li.moduleBaseDir)
	if ownBaseDir == "." {
		ownBaseDir = ""
	}

	filePath, found := li.crossIndex.lookup(ctx, path, ownBaseDir)
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

// crossModuleIndex indexes one gcExportDataDir, mapping an import path to every .o file
// that can satisfy it. The tree is walked at most once per directory in the process and shared across requests,
// making each cross-module lookup an O(1) map access.
type crossModuleIndex struct {
	dir   string
	once  sync.Once
	ready bool
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
	defer func() { idx.ready = true }()
	if idx.dir == "" {
		return
	}
	_ = filepath.WalkDir(idx.dir, func(walkPath string, d fs.DirEntry, err error) error {
		if err != nil || d.IsDir() {
			return nil
		}
		idx.registerFile(walkPath)
		return nil
	})
}

// addExportedFile updates an index that was already built before an export. A lazy index will see the file
// in its first directory walk instead. Requests are served sequentially, so this needs no separate lock.
func (idx *crossModuleIndex) addExportedFile(path string) {
	if idx.ready {
		idx.registerFile(path)
	}
}

func (idx *crossModuleIndex) registerFile(path string) {
	name := filepath.Base(path)
	if !strings.HasSuffix(name, ".o") {
		return
	}
	relPath, err := filepath.Rel(idx.dir, path)
	if err != nil || relPath == ".." || strings.HasPrefix(relPath, ".."+string(os.PathSeparator)) {
		return
	}
	dir := filepath.Dir(relPath)
	if dir == "." {
		return
	}
	parts := strings.Split(dir, string(os.PathSeparator))
	if parts[len(parts)-1] != strings.TrimSuffix(name, ".o") {
		idx.oFiles++
		return
	}
	// A package may be exported again; retain one candidate per file.
	fullPath := strings.Join(parts, "/")
	for _, candidate := range idx.byPath[fullPath] {
		if candidate.filePath == path {
			return
		}
	}
	idx.oFiles++
	for j := range parts {
		importPath := strings.Join(parts[j:], "/")
		candidate := oFileCandidate{baseDir: strings.Join(parts[:j], string(os.PathSeparator)), filePath: path}
		// WalkDir uses lexical order. Keep the same first-match behavior when adding exports later.
		candidates := idx.byPath[importPath]
		position := sort.Search(len(candidates), func(i int) bool { return !walkOrderLess(candidates[i].filePath, path) })
		candidates = append(candidates, oFileCandidate{})
		copy(candidates[position+1:], candidates[position:])
		candidates[position] = candidate
		idx.byPath[importPath] = candidates
	}
}

// walkOrderLess compares path components in the order filepath.WalkDir visits them.
func walkOrderLess(a, b string) bool {
	aParts := strings.Split(a, string(os.PathSeparator))
	bParts := strings.Split(b, string(os.PathSeparator))
	for i := 0; i < len(aParts) && i < len(bParts); i++ {
		if aParts[i] != bParts[i] {
			return aParts[i] < bParts[i]
		}
	}
	return len(aParts) < len(bParts)
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

// typeCheckAstWithCrossIndex resolves cross-module imports with an index the caller can reuse as long as the
// GC export data directory does not change.
func typeCheckAstWithCrossIndex(
	ctx context.Context,
	fileSet *token.FileSet,
	astFiles map[string]AstFileOrError,
	params Params,
	gcExporter GcExporter,
	// Shared across every per-package importer so gcExportDataDir is walked at most once per run.
	crossIndex *crossModuleIndex,
) (*types.Info, []error) {
	debugTypeCheck := params.debugTypeCheck
	gcExportDataDir := params.gcExportDataDir
	moduleName := params.moduleName
	moduleBaseDir := params.moduleBaseDir
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
		packageCtx, packageDone := span(ctx, "typecheck.package", "packageName", packageName, "fileCount", len(files))
		typeErrorCount := 0
		importer := &localImporter{
			gcExportDataDir: gcExportDataDir,
			moduleName:      moduleName,
			moduleBaseDir:   moduleBaseDir,
			debugTypeCheck:  debugTypeCheck,
			gcExporter:      gcExporter,
			importCache:     make(map[string]*types.Package),
			crossIndex:      crossIndex,
		}
		conf := types.Config{
			Importer: importer.typesImporter(packageCtx),
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
		sortedFiles := sortedAstFiles(files)
		var pkg *types.Package
		if !params.dumpGcExportData {
			// The checker would resolve these imports anyway; loading them first keeps their cost out of
			// the context span and lets restored declarations share the fresh imported objects.
			importer.loadSubmittedImports(packageCtx, sortedFiles)
			_, contextDone := span(packageCtx, "typecheck.context")
			pkg = importer.packageWithContext(packageName, files)
			contextDone()
		}
		if pkg == nil {
			pkg = types.NewPackage(packageName, packageName)
		}
		checker := types.NewChecker(&conf, fileSet, pkg, info)
		err := checker.Files(sortedFiles)
		if err != nil {
			errors = append(errors, err)
		}
		packageDone("typeErrorCount", typeErrorCount)
	}

	return info, errors
}

// packageWithContext preserves the checker's short package path used by the semantic mapper.
// Only the current module's directory can supply same-package declarations.
func (li *localImporter) packageWithContext(packageName string, files map[string]AstFileOrError) *types.Package {
	pkg := types.NewPackage(packageName, packageName)
	if li.gcExportDataDir == "" || li.moduleName == "" {
		return pkg
	}
	var directory string
	for name := range files {
		candidate := filepath.Dir(name)
		if directory != "" && candidate != directory {
			return pkg
		}
		directory = candidate
	}
	packageDir, err := filepath.Rel(li.moduleBaseDir, directory)
	if err != nil || packageDir == ".." || strings.HasPrefix(packageDir, ".."+string(os.PathSeparator)) {
		return pkg
	}
	filePath := li.packageContextPath(directory, packageDir, packageName)
	restored, found := li.readPackageContext(filePath, packageName)
	if !found || restored.Name() != packageName {
		return pkg
	}
	submitted := submittedPackageNames(files)
	receivers := make(map[string]bool)
	excludeRestoredReceivers(receivers, files)
	excludedTypes := excludedContextTypes(restored, submitted)
	for _, name := range restored.Scope().Names() {
		object := restored.Scope().Lookup(name)
		if !submitted[name] && !receivers[name] && !excludedTypes[object.Type()] {
			pkg.Scope().Insert(object)
		}
	}
	return pkg
}

func (li *localImporter) readPackageContext(filePath, packageName string) (*types.Package, bool) {
	// Preserve the semantic package path and keep its short cache key isolated
	// from imported packages, including standard-library namesakes.
	cached := li.importCache[packageName]
	delete(li.importCache, packageName)
	restored, _, found := li.tryLoadExportData(filePath, packageName)
	delete(li.importCache, packageName)
	if cached != nil {
		li.importCache[packageName] = cached
	}
	return restored, found
}

func (li *localImporter) loadSubmittedImports(ctx context.Context, files []*ast.File) {
	for _, file := range files {
		for _, spec := range file.Imports {
			path, err := strconv.Unquote(spec.Path.Value)
			if err == nil && path != "C" {
				_, _ = li.Import(ctx, path)
			}
		}
	}
}

// Methods and imports do not declare names in the package scope.
func submittedPackageNames(files map[string]AstFileOrError) map[string]bool {
	names := make(map[string]bool)
	for _, file := range files {
		for _, declaration := range file.ast.Decls {
			addSubmittedNames(names, declaration)
		}
	}
	return names
}

// go/types requires a source declaration for a method's receiver base type.
// Leave missing receiver declarations unresolved, as in source-only checking,
// rather than passing imported TypeNames to its source-method resolver.
func excludeRestoredReceivers(excluded map[string]bool, files map[string]AstFileOrError) {
	aliases := make(map[string]ast.Expr)
	var receivers []ast.Expr
	for _, file := range files {
		for _, declaration := range file.ast.Decls {
			switch declaration := declaration.(type) {
			case *ast.FuncDecl:
				receivers = append(receivers, methodReceivers(declaration)...)
			case *ast.GenDecl:
				collectSubmittedAliases(aliases, declaration)
			}
		}
	}
	for _, receiver := range receivers {
		excludeReceiverBase(excluded, aliases, receiver)
	}
}

func methodReceivers(declaration *ast.FuncDecl) []ast.Expr {
	if declaration.Recv == nil {
		return nil
	}
	var receivers []ast.Expr
	for _, field := range declaration.Recv.List {
		receivers = append(receivers, field.Type)
	}
	return receivers
}

func collectSubmittedAliases(aliases map[string]ast.Expr, declaration *ast.GenDecl) {
	for _, spec := range declaration.Specs {
		if spec, ok := spec.(*ast.TypeSpec); ok && spec.Assign.IsValid() {
			aliases[spec.Name.Name] = spec.Type
		}
	}
}

func excludeReceiverBase(excluded map[string]bool, aliases map[string]ast.Expr, receiver ast.Expr) {
	seen := make(map[string]bool)
	for receiver != nil {
		switch expr := ast.Unparen(receiver).(type) {
		case *ast.Ident:
			if seen[expr.Name] {
				return
			}
			seen[expr.Name] = true
			excluded[expr.Name] = true
			receiver = aliases[expr.Name]
		case *ast.StarExpr:
			receiver = expr.X
		case *ast.IndexExpr:
			receiver = expr.X
		case *ast.IndexListExpr:
			receiver = expr.X
		default:
			return
		}
	}
}

func addSubmittedNames(names map[string]bool, declaration ast.Decl) {
	switch declaration := declaration.(type) {
	case *ast.FuncDecl:
		if declaration.Recv == nil {
			names[declaration.Name.Name] = true
		}
	case *ast.GenDecl:
		for _, spec := range declaration.Specs {
			switch spec := spec.(type) {
			case *ast.TypeSpec:
				names[spec.Name.Name] = true
			case *ast.ValueSpec:
				for _, name := range spec.Names {
					names[name.Name] = true
				}
			}
		}
	}
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

// A3S may omit go.mod. Recover a missing module base only from an unambiguous
// export path whose module base is an ancestor of the submitted source directory.
func (li *localImporter) packageContextPath(directory, packageDir, packageName string) string {
	path := packageExportPath(filepath.Join(li.gcExportDataDir, li.moduleBaseDir), li.moduleName, packageDir, packageName)
	if _, err := os.Stat(path); err == nil || li.moduleBaseDir != "." {
		return path
	}
	candidate := ""
	for base := directory; base != "." && base != filepath.Dir(base); base = filepath.Dir(base) {
		rel, err := filepath.Rel(base, directory)
		if err != nil {
			continue
		}
		possible := packageExportPath(filepath.Join(li.gcExportDataDir, base), li.moduleName, rel, packageName)
		if _, err := os.Stat(possible); err == nil {
			if candidate != "" {
				return ""
			}
			candidate = possible
		}
	}
	return candidate
}

// Build reverse edges once, then propagate exclusion through cycles to every dependent type.
func excludedContextTypes(restored *types.Package, excluded map[string]bool) map[types.Type]bool {
	reverse := make(map[types.Type][]types.Type)
	seen := make(map[types.Type]bool)
	invalid := make(map[types.Type]bool)
	var pending []types.Type
	for _, name := range restored.Scope().Names() {
		pending = append(pending, restored.Scope().Lookup(name).Type())
	}
	var blocked []types.Type
	for len(pending) > 0 {
		t := pending[len(pending)-1]
		pending = pending[:len(pending)-1]
		if t == nil || seen[t] {
			continue
		}
		seen[t] = true
		for _, child := range contextTypeChildren(t, restored, excluded) {
			if child == nil {
				invalid[t] = true
				blocked = append(blocked, t)
			} else {
				reverse[child] = append(reverse[child], t)
				pending = append(pending, child)
			}
		}
	}
	propagateExcludedTypes(reverse, invalid, blocked)
	return invalid
}

func propagateExcludedTypes(reverse map[types.Type][]types.Type, invalid map[types.Type]bool, blocked []types.Type) {
	for len(blocked) > 0 {
		t := blocked[len(blocked)-1]
		blocked = blocked[:len(blocked)-1]
		for _, parent := range reverse[t] {
			if !invalid[parent] {
				invalid[parent] = true
				blocked = append(blocked, parent)
			}
		}
	}
}

// A nil child marks a replaced local type. Imported named types keep their identity.
func contextTypeChildren(t types.Type, restored *types.Package, excluded map[string]bool) []types.Type {
	switch t := t.(type) {
	case *types.Pointer:
		return []types.Type{t.Elem()}
	case *types.Slice:
		return []types.Type{t.Elem()}
	case *types.Array:
		return []types.Type{t.Elem()}
	case *types.Map:
		return []types.Type{t.Key(), t.Elem()}
	case *types.Chan:
		return []types.Type{t.Elem()}
	}
	return contextDefinedTypeChildren(t, restored, excluded)
}

func contextDefinedTypeChildren(t types.Type, restored *types.Package, excluded map[string]bool) []types.Type {
	switch t := t.(type) {
	case *types.Named:
		return contextNamedTypeChildren(t, restored, excluded)
	case *types.Alias:
		return contextAliasTypeChildren(t, restored, excluded)
	case *types.Signature:
		return append([]types.Type{t.Params(), t.Results()}, typeParameterConstraints(t.TypeParams())...)
	case *types.Tuple:
		var children []types.Type
		for i := 0; i < t.Len(); i++ {
			children = append(children, t.At(i).Type())
		}
		return children
	case *types.Struct:
		var children []types.Type
		for i := 0; i < t.NumFields(); i++ {
			children = append(children, t.Field(i).Type())
		}
		return children
	case *types.Interface:
		var children []types.Type
		for i := 0; i < t.NumExplicitMethods(); i++ {
			children = append(children, t.ExplicitMethod(i).Type())
		}
		for i := 0; i < t.NumEmbeddeds(); i++ {
			children = append(children, t.EmbeddedType(i))
		}
		return children
	case *types.TypeParam:
		return []types.Type{t.Constraint()}
	case *types.Union:
		var children []types.Type
		for i := 0; i < t.Len(); i++ {
			children = append(children, t.Term(i).Type())
		}
		return children
	}
	return nil
}

func typeListElements(list *types.TypeList) []types.Type {
	var result []types.Type
	for i := 0; i < list.Len(); i++ {
		result = append(result, list.At(i))
	}
	return result
}

func typeParameterConstraints(list *types.TypeParamList) []types.Type {
	var result []types.Type
	for i := 0; i < list.Len(); i++ {
		result = append(result, list.At(i).Constraint())
	}
	return result
}

func contextNamedTypeChildren(t *types.Named, restored *types.Package, excluded map[string]bool) []types.Type {
	children := typeListElements(t.TypeArgs())
	if t.Obj().Pkg() != restored {
		return children
	}
	if excluded[t.Obj().Name()] {
		return []types.Type{nil}
	}
	if t.Origin() != t {
		return append(children, t.Origin())
	}
	children = append(children, t.Underlying())
	for i := 0; i < t.NumMethods(); i++ {
		children = append(children, t.Method(i).Type())
	}
	return children
}

func contextAliasTypeChildren(t *types.Alias, restored *types.Package, excluded map[string]bool) []types.Type {
	if t.Obj().Pkg() == restored && excluded[t.Obj().Name()] {
		return []types.Type{nil}
	}
	if t.Origin() != t {
		return append(typeListElements(t.TypeArgs()), t.Origin())
	}
	return []types.Type{t.Rhs()}
}
