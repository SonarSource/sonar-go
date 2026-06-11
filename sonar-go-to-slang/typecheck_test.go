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
	"bytes"
	"go/ast"
	"go/token"
	"go/types"
	"log"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"golang.org/x/tools/go/gcexportdata"
)

func TestImportExistingPackage_returnsPackage(t *testing.T) {
	importer := &localImporter{}
	pkg, err := importer.Import("net/http")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func TestImportNonSupportedPackage_returnsEmptyPackage(t *testing.T) {
	importer := &localImporter{}
	pkg, err := importer.Import("non/supported")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "non/supported", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())

	// Assert that it failed silently
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer log.SetOutput(os.Stderr)
	assert.Empty(t, buf.String())
}

func TestImport_notFoundPackageDoesNotPoisonLaterEmbeddedRead(t *testing.T) {
	importer := &localImporter{importCache: make(map[string]*types.Package)}

	// "sync/atomic" is not embedded and not on disk, so it resolves to a synthesized empty
	// package whose name is only a guess from the path ("sync/atomic", not the real "atomic").
	// Such empty packages must NOT enter the shared importCache: it is the same map handed to
	// gcexportdata.Read, which reconciles it against export data manifests by package name.
	// The embedded "runtime/debug" data references "sync/atomic" by its real name "atomic"; a
	// wrong-named cached entry would abort that read and leave runtime/debug (hence PrintStack,
	// rule S4507) unresolved.
	_, err := importer.Import("sync/atomic")
	assert.NoError(t, err)

	debugPkg, err := importer.Import("runtime/debug")
	assert.NoError(t, err)
	assert.NotNil(t, debugPkg.Scope().Lookup("PrintStack"),
		"runtime/debug must still expose PrintStack after a not-found package was imported")
}

func TestGetPackageFromExportData_validFile_returnsPackage(t *testing.T) {
	li := localImporter{}
	pkg := li.getPackageFromExportData(PackageExportDataDir+"/"+"net_http.o", "net/http")
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func TestGetPackageFromExportData_invalidFile_returnsEmptyPackage(t *testing.T) {
	li := localImporter{}
	pkg := li.getPackageFromExportData("invalid_file.o", "invalid/path")
	assert.NotNil(t, pkg)
	assert.Equal(t, "invalid/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func TestGetEmptyPackage_returnsEmptyPackage(t *testing.T) {
	pkg := getEmptyPackage("empty/path")
	assert.NotNil(t, pkg)
	assert.Equal(t, "empty/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func TestTypeCheckAst(t *testing.T) {
	source, err := os.ReadFile("resources/simple_file_with_packages.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	fileSet, astFiles := astFromString("simple_file_with_packages.go", string(source))
	info, _ := typeCheckAst(fileSet, astFiles, true, "", "", ".", GcExporter{})

	assert.NotNil(t, info)
	assert.NotEmpty(t, info.Types)
	assert.NotEmpty(t, info.Defs)
	assert.NotEmpty(t, info.Uses)
	assert.NotEmpty(t, info.Implicits)
	assert.NotEmpty(t, info.Selections)
	assert.NotEmpty(t, info.Scopes)
}

func TestTestOnlyFirstErrorIsReturned(t *testing.T) {
	source, err := os.ReadFile("resources/file_with_many_errors.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	fileSet, astFiles := astFromString("file_with_many_errors.go", string(source))

	info, errors := typeCheckAst(fileSet, astFiles, false, "", "", ".", GcExporter{})
	assert.Len(t, errors, 1)
	assert.Equal(t, "file_with_many_errors.go:4:5: declared and not used: a1", errors[0].Error())
	assert.NotNil(t, info)
}

func TestShouldReturnErrorForAllFailingFilesPerPackage(t *testing.T) {
	source1, err := os.ReadFile("resources/file_with_many_errors.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}
	source2, err := os.ReadFile("resources/file_with_many_errors_2.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	filenameToContent := map[string]string{
		"file_with_many_errors_1.go": string(source1),
		"file_with_many_errors_2.go": string(source2),
	}
	fileSet, astFiles := astFromStrings(filenameToContent)

	info, errors := typeCheckAst(fileSet, astFiles, false, "", "", ".", GcExporter{})
	assert.ElementsMatch(t, []string{
		"file_with_many_errors_1.go:4:5: declared and not used: a1",
		"file_with_many_errors_2.go:4:5: declared and not used: a1",
	}, []string{errors[0].Error(), errors[1].Error()})
	assert.NotNil(t, info)
}

func TestShouldGroupFilesPerPackageNameTogether(t *testing.T) {
	var file1AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package1"}}, err: nil}
	var file2AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package2"}}, err: nil}
	var file3AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package2"}}, err: nil}

	var astFiles = map[string]AstFileOrError{
		"file1.go": file1AstOrError,
		"file2.go": file2AstOrError,
		"file3.go": file3AstOrError,
	}
	var filesPerPackageName = groupFilesPerPackageName(astFiles)
	assert.Len(t, filesPerPackageName, 2)
	assert.Contains(t, filesPerPackageName, "package1")
	assert.Contains(t, filesPerPackageName, "package2")

	var package1Files = filesPerPackageName["package1"]
	assert.Len(t, package1Files, 1)
	assert.Contains(t, package1Files, "file1.go")
	assert.Equal(t, file1AstOrError, package1Files["file1.go"])

	var package2Files = filesPerPackageName["package2"]
	assert.Len(t, package2Files, 2)
	assert.Contains(t, package2Files, "file2.go")
	assert.Equal(t, file2AstOrError, package2Files["file2.go"])
	assert.Contains(t, package2Files, "file3.go")
	assert.Equal(t, file3AstOrError, package2Files["file3.go"])
}

func TestShouldIgnoreFileWithoutAst(t *testing.T) {
	var file1AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package1"}}, err: nil}
	var file2AstOrError = AstFileOrError{ast: nil, err: nil}

	var astFiles = map[string]AstFileOrError{
		"file1.go": file1AstOrError,
		"file2.go": file2AstOrError,
	}
	var filesPerPackageName = groupFilesPerPackageName(astFiles)
	assert.Len(t, filesPerPackageName, 1)
	assert.Contains(t, filesPerPackageName, "package1")

	var package1Files = filesPerPackageName["package1"]
	assert.Len(t, package1Files, 1)
	assert.Contains(t, package1Files, "file1.go")
	assert.Equal(t, file1AstOrError, package1Files["file1.go"])
}

// This test checks that all files from the mapping are present in the packages directory.
// It ensures that the mapping is up-to-date.
func TestAllFilesFromMappingShouldBePresent(t *testing.T) {
	mapping := packageExportData
	files, err := packages.ReadDir(PackageExportDataDir)
	assert.NoError(t, err)

	for _, fileName := range mapping {
		found := false
		for i, file := range files {
			if file.Name() == fileName {
				found = true
				files = append(files[:i], files[i+1:]...)
				break
			}
		}
		assert.True(t, found, "File %s is missing in the `build/packages` directory", fileName)
	}
	assert.Empty(t, files, "There are files in the `build/packages` directory that are not present in the mapping")
}

// createTestExportData writes a minimal valid .o export data file for the given import path
// under the specified directory. The created package has one exported variable so its scope
// is non-empty, allowing tests to distinguish it from an empty fallback package.
func createTestExportData(t *testing.T, dir string, importPath string) {
	t.Helper()
	pkgName := getPackageName(importPath)
	fullDir := filepath.Join(dir, importPath)
	err := os.MkdirAll(fullDir, 0755)
	assert.NoError(t, err)

	pkg := types.NewPackage(importPath, pkgName)
	pkg.Scope().Insert(types.NewVar(token.NoPos, pkg, "TestMarker", types.Typ[types.Int]))
	pkg.MarkComplete()

	file, err := os.Create(filepath.Join(fullDir, pkgName+".o"))
	assert.NoError(t, err)
	defer file.Close()

	err = gcexportdata.Write(file, nil, pkg)
	assert.NoError(t, err)
}

func TestImport_moduleBaseDir_ownModule(t *testing.T) {
	tmpDir := t.TempDir()
	// Write .o under {tmpDir}/service1/mymod/pkg/pkg.o
	createTestExportData(t, filepath.Join(tmpDir, "service1"), "mymod/pkg")

	importer := &localImporter{
		gcExportDataDir: tmpDir,
		moduleBaseDir:   "service1",
		gcExporter:      GcExporter{},
	}
	pkg, err := importer.Import("mymod/pkg")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "mymod/pkg", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0, "should find the real .o, not an empty package")
}

func TestImport_moduleBaseDir_crossModule(t *testing.T) {
	tmpDir := t.TempDir()
	// Write .o under a different module's subdir
	createTestExportData(t, filepath.Join(tmpDir, "pkg"), "ModulePkg/foo")

	importer := &localImporter{
		gcExportDataDir:   tmpDir,
		moduleBaseDir:     ".", // root module
		gcExporter:        GcExporter{},
		importPathToOFile: make(map[string]string),
	}
	// Root module imports ModulePkg/foo — should find it via cross-module scan
	pkg, err := importer.Import("ModulePkg/foo")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "ModulePkg/foo", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0, "should find the .o via cross-module fallback")
}

func TestImport_rootModule_flatLookup(t *testing.T) {
	tmpDir := t.TempDir()
	// Write .o at flat path: {tmpDir}/mymod/pkg/pkg.o
	createTestExportData(t, tmpDir, "mymod/pkg")

	importer := &localImporter{
		gcExportDataDir:   tmpDir,
		moduleBaseDir:     ".",
		gcExporter:        GcExporter{},
		importPathToOFile: make(map[string]string),
	}
	pkg, err := importer.Import("mymod/pkg")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "mymod/pkg", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0, "should find the .o via flat lookup")
}

func TestImport_moduleBaseDir_prefersOwnModule(t *testing.T) {
	tmpDir := t.TempDir()
	// Both service1 and service2 have poc/util — importer with moduleBaseDir=service1 should prefer service1's
	createTestExportData(t, filepath.Join(tmpDir, "service1"), "poc/util")
	createTestExportData(t, filepath.Join(tmpDir, "service2"), "poc/util")

	importer := &localImporter{
		gcExportDataDir:   tmpDir,
		moduleBaseDir:     "service1",
		gcExporter:        GcExporter{},
		importPathToOFile: make(map[string]string),
	}
	pkg, err := importer.Import("poc/util")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Greater(t, pkg.Scope().Len(), 0, "should find the .o from own module")
}

func TestImport_subModule_findsRootModulePackage(t *testing.T) {
	tmpDir := t.TempDir()
	// Root module (moduleBaseDir=".") stores data at flat path: {tmpDir}/rootapi/service/service.o
	createTestExportData(t, tmpDir, "rootapi/service")
	createTestExportData(t, filepath.Join(tmpDir, "sub"), "sub/internal")

	importer := &localImporter{
		gcExportDataDir:   tmpDir,
		moduleBaseDir:     "sub",
		gcExporter:        GcExporter{},
		importPathToOFile: make(map[string]string),
	}
	pkg, err := importer.Import("rootapi/service")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Greater(t, pkg.Scope().Len(), 0,
		"sub-module should find root module's package at flat path %s/rootapi/service/service.o",
		tmpDir)
}

func TestImport_multiSegmentModuleBaseDir_findsSiblingPackage(t *testing.T) {
	tmpDir := t.TempDir()
	// Two modules nested under "a/": a/b and a/c
	createTestExportData(t, filepath.Join(tmpDir, "a/b"), "shared/lib")
	createTestExportData(t, filepath.Join(tmpDir, "a/c"), "other/api")

	importer := &localImporter{
		gcExportDataDir:   tmpDir,
		moduleBaseDir:     "a/b",
		gcExporter:        GcExporter{},
		importPathToOFile: make(map[string]string),
	}
	pkg, err := importer.Import("other/api")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Greater(t, pkg.Scope().Len(), 0,
		"module a/b should find package from sibling module a/c at %s/a/c/other/api/api.o",
		tmpDir)
}

func TestImport_sharedCacheDeduplicatesTransitiveDeps(t *testing.T) {
	importer := &localImporter{
		gcExporter:  GcExporter{},
		importCache: make(map[string]*types.Package),
	}

	// Import net/url first so it populates the shared cache.
	urlPkg, err := importer.Import("net/url")
	assert.NoError(t, err)
	assert.NotNil(t, urlPkg)

	// Import net/http, whose manifest references net/url as a direct import.
	// gcexportdata.Read must reuse the cached net/url object from the shared importCache.
	httpPkg, err := importer.Import("net/http")
	assert.NoError(t, err)
	assert.NotNil(t, httpPkg)

	var httpUrlDep *types.Package
	for _, imp := range httpPkg.Imports() {
		if imp.Path() == "net/url" {
			httpUrlDep = imp
			break
		}
	}
	assert.NotNil(t, httpUrlDep, "net/http must import net/url")
	assert.Same(t, urlPkg, httpUrlDep,
		"shared importCache must return the identical *types.Package for net/url in both direct and transitive positions")

	// Round-trip: write net/http then read it back.
	// The shared importCache ensures no duplicate PkgPaths in the manifest, so both steps must succeed.
	tmpDir := t.TempDir()
	outFile, err := os.Create(filepath.Join(tmpDir, "net_http.o"))
	assert.NoError(t, err)
	err = gcexportdata.Write(outFile, nil, httpPkg)
	outFile.Close()
	assert.NoError(t, err)

	inFile, err := os.Open(filepath.Join(tmpDir, "net_http.o"))
	assert.NoError(t, err)
	defer inFile.Close()
	roundTrippedPkg, err := gcexportdata.Read(inFile, nil, make(map[string]*types.Package), "net/http")
	assert.NoError(t, err, "round-trip read must succeed")
	assert.Equal(t, "net/http", roundTrippedPkg.Path())
}
