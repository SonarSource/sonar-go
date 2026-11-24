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
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/tools/go/gcexportdata"
)

// Regression test for the Go analyzer's semantic model after A3S context restoration.
// A call to a function in another file of the same package should resolve from restored context.
func TestA3SContextResolvesOtherFileInSamePackage(t *testing.T) {
	project := goProject{
		module: "example.com/contextrepro",
		source: `package main
func Source(value string) string { return Sink(value) }
`,
		sinkPath: "sink.go",
		sink: `package main
func Sink(value string) string { return value }
`,
	}

	full := modelFullCIAnalysis(t, project)
	collected := collectCIContext(t, full)
	restored := modelA3SRestoration(t, collected)
	partial := modelSourceOnlyA3SRequest(t, project, restored)

	assert.Empty(t, partial.errors, "source.go must resolve declarations retained in the restored context")
	assert.IsType(t, (*types.Func)(nil), partial.symbol,
		"Sink should resolve to a function using the restored context")
	assert.Equal(t, full.semantic.Package, partial.semantic.Package)
	assert.Equal(t, full.semantic.Type, partial.semantic.Type)
}

// Positive control: native context resolves a declaration in another package reached by import.
func TestA3SContextResolvesImportedPackage(t *testing.T) {
	project := goProject{
		module: "example.com/contextrepro",
		source: `package main
import "example.com/contextrepro/otherpkg"
func Source(value string) string { return otherpkg.Sink(value) }
`,
		sinkPath: "otherpkg/sink.go",
		sink: `package otherpkg
func Sink(value string) string { return value }
`,
	}

	full := modelFullCIAnalysis(t, project)
	collected := collectCIContext(t, full)
	restored := modelA3SRestoration(t, collected)
	partial := modelSourceOnlyA3SRequest(t, project, restored)

	assert.Empty(t, partial.errors, "source.go must resolve declarations retained in the restored context")
	assert.IsType(t, (*types.Func)(nil), partial.symbol,
		"Sink should resolve to a function using the restored context")
	assert.Equal(t, full.semantic.Package, partial.semantic.Package)
	assert.Equal(t, full.semantic.Type, partial.semantic.Type)
}

// goProject describes the two source files used in a context-restoration scenario.
// The file submitted to A3S is always source.go; sinkPath determines whether Sink is in another package.
type goProject struct {
	module   string
	source   string
	sinkPath string
	sink     string
}

// ciPackageAnalysis retains a package's type information for subsequent context collection.
type ciPackageAnalysis struct {
	path string
	info *types.Info
}

// ciFullAnalysis retains the full project's package analyses and the resolved call to Sink.
type ciFullAnalysis struct {
	project  goProject
	packages []ciPackageAnalysis
	semantic *IdentifierInfo
}

// ciCollectedContext records exported files and the Sink declaration restoration must preserve.
type ciCollectedContext struct {
	directory   string
	files       []string
	sinkPackage string
	sinkType    string
}

// a3sSourceOnlyAnalysis exposes the call's symbol, semantic information and type-checking errors.
type a3sSourceOnlyAnalysis struct {
	symbol   types.Object
	semantic *IdentifierInfo
	errors   []error
}

// modelFullCIAnalysis models step 1: analyze the complete project and verify that Sink resolves.
// Same-package files are checked together. An imported package is checked and exported first,
// making its declarations available when the analyzer checks the calling package.
// The result retains both the baseline call semantics and each package's type information.
func modelFullCIAnalysis(t *testing.T, project goProject) ciFullAnalysis {
	t.Helper()
	full := ciFullAnalysis{project: project}
	sources := map[string]string{"source.go": project.source}
	sinkDirectory := filepath.Dir(project.sinkPath)
	baselineContext := ""
	expectedPackage := "main"
	if sinkDirectory == "." {
		sources[project.sinkPath] = project.sink
	} else {
		baselineContext = t.TempDir()
		sinkSet, sinkFiles := astFromString(project.sinkPath, project.sink)
		sinkInfo, sinkErrors := typeCheckAst(context.Background(), sinkSet, sinkFiles, true, "", project.module, ".", GcExporter{})
		require.Empty(t, sinkErrors)
		exporter := GcExporter{}
		require.Len(t, exporter.ExportGcExportData(context.Background(), sinkInfo, baselineContext, project.module, sinkDirectory, true), 1)
		full.packages = append(full.packages, ciPackageAnalysis{path: sinkDirectory, info: sinkInfo})
		expectedPackage = filepath.ToSlash(sinkDirectory)
	}

	fileSet, files := astFromStrings(sources)
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, baselineContext, project.module, ".", GcExporter{})
	require.Empty(t, errors)
	call := a3sFindCall(t, files["source.go"].ast, "Sink")
	symbol := info.Uses[call]
	require.IsType(t, (*types.Func)(nil), symbol)
	mapper := NewSlangMapper(fileSet, files["source.go"].ast, project.source, info, project.module, buildUsesByPos(info))
	full.semantic = mapper.getIdentifierInfo(call)
	require.Equal(t, project.module+"/"+expectedPackage, full.semantic.Package)
	require.Equal(t, "func(value string) string", full.semantic.Type)
	t.Logf("full CI model: Sink symbol=%s; package=%s; type=%s", symbol, full.semantic.Package, full.semantic.Type)
	full.packages = append(full.packages, ciPackageAnalysis{info: info})
	return full
}

// collectCIContext models step 2: export each fully analyzed package into a fresh context directory.
// It checks the native .o paths and records the Sink declaration that restoration must preserve.
func collectCIContext(t *testing.T, full ciFullAnalysis) ciCollectedContext {
	t.Helper()
	collected := ciCollectedContext{
		directory:   t.TempDir(),
		sinkPackage: full.semantic.Package,
		sinkType:    full.semantic.Type,
	}
	exporter := GcExporter{}
	for _, pkg := range full.packages {
		files := exporter.ExportGcExportData(context.Background(), pkg.info, collected.directory, full.project.module, pkg.path, true)
		require.Len(t, files, 1)
		packageName := filepath.Base(pkg.path)
		if pkg.path == "" {
			packageName = "main"
		}
		expected := filepath.Join(collected.directory, full.project.module, packageName, packageName+".o")
		if pkg.path == "" {
			expected = filepath.Join(collected.directory, full.project.module, ".root.o")
		}
		require.Equal(t, expected, files[0])
		t.Logf("collected context item: %s", filepath.ToSlash(a3sRelativePath(t, collected.directory, files[0])))
		collected.files = append(collected.files, files...)
	}
	return collected
}

// modelA3SRestoration models step 3: copy every unchanged context payload into a fresh directory,
// preserving its relative path. It then independently decodes Sink's package to verify that
// Sink and its signature survived collection and restoration, and returns the restored directory.
func modelA3SRestoration(t *testing.T, collected ciCollectedContext) string {
	t.Helper()
	restored := t.TempDir()
	for _, file := range collected.files {
		relative := a3sRelativePath(t, collected.directory, file)
		payload, err := os.ReadFile(file)
		require.NoError(t, err)
		restoredFile := filepath.Join(restored, relative)
		require.NoError(t, os.MkdirAll(filepath.Dir(restoredFile), 0755))
		require.NoError(t, os.WriteFile(restoredFile, payload, 0644))
	}

	exportPath := filepath.Join(restored, collected.sinkPackage, filepath.Base(collected.sinkPackage)+".o")
	if filepath.Base(collected.sinkPackage) == "main" {
		exportPath = filepath.Join(restored, filepath.Dir(collected.sinkPackage), ".root.o")
	}
	data, err := os.Open(exportPath)
	require.NoError(t, err)
	pkg, err := gcexportdata.Read(data, token.NewFileSet(), make(map[string]*types.Package), collected.sinkPackage)
	require.NoError(t, data.Close())
	require.NoError(t, err)
	sink := pkg.Scope().Lookup("Sink")
	require.NotNil(t, sink)
	require.Equal(t, collected.sinkType, sink.Type().String())
	t.Logf("restored context declares: %s", sink)
	return restored
}

// modelSourceOnlyA3SRequest models step 4: analyze only source.go using the restored context.
// It returns the call's symbol, semantic information and errors without asserting successful
// resolution, so each test can compare the result with its full-analysis baseline.
func modelSourceOnlyA3SRequest(t *testing.T, project goProject, restored string) a3sSourceOnlyAnalysis {
	t.Helper()
	fileSet, files := astFromString("source.go", project.source)
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, restored, project.module, ".", GcExporter{})
	call := a3sFindCall(t, files["source.go"].ast, "Sink")
	mapper := NewSlangMapper(fileSet, files["source.go"].ast, project.source, info, project.module, buildUsesByPos(info))
	partial := a3sSourceOnlyAnalysis{symbol: info.Uses[call], semantic: mapper.getIdentifierInfo(call), errors: errors}
	t.Logf("source-only model: Sink symbol=%v; package=%s; type=%s; errors=%v", partial.symbol, partial.semantic.Package, partial.semantic.Type, partial.errors)
	return partial
}

// a3sFindCall finds the called function's identifier in the parsed syntax tree.
// It handles both Sink(...) and otherpkg.Sink(...), and fails the test if no call matches.
func a3sFindCall(t *testing.T, file *ast.File, name string) *ast.Ident {
	t.Helper()
	var found *ast.Ident
	ast.Inspect(file, func(node ast.Node) bool {
		call, ok := node.(*ast.CallExpr)
		if !ok {
			return true
		}
		switch function := call.Fun.(type) {
		case *ast.Ident:
			if function.Name == name {
				found = function
			}
		case *ast.SelectorExpr:
			if function.Sel.Name == name {
				found = function.Sel
			}
		}
		return true
	})
	require.NotNil(t, found, "call to %s missing from test input", name)
	return found
}

// a3sRelativePath computes a context item's path relative to its collection directory,
// so restoration can preserve that path under another directory. It fails the test on error.
func a3sRelativePath(t *testing.T, base, file string) string {
	t.Helper()
	relative, err := filepath.Rel(base, file)
	require.NoError(t, err)
	return relative
}

func TestA3SContextSubmittedDeclarationsTakePrecedence(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/contextrepro"
	fileSet, files := astFromString("source.go", `package main
func Sink(value string) string { return value }
type Value string
type Alias = string
var Variable string
const Constant = "old"
`)
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false), 1)
	fileSet, files = astFromString("source.go", `package main
func Sink(value int) int { return value }
type Value int
type Alias = int
var Variable int
const Constant = 1
func Source() int { return Sink(int(Value(Variable)) + Alias(Constant)) }
`)
	info, errors = typeCheckAst(context.Background(), fileSet, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	call := a3sFindCall(t, files["source.go"].ast, "Sink")
	require.Equal(t, "func(value int) int", info.Uses[call].Type().String())
	for id, object := range info.Defs {
		if id.Name == "Sink" {
			assert.Same(t, object, info.Uses[call])
		}
	}
}

func TestA3SContextUnexportedBoundary(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/contextrepro"
	fileSet, files := astFromStrings(map[string]string{
		"source.go": "package main\nfunc Source() string { return Sink() }",
		"sink.go": `package main
func Sink() string { return hidden() }
func hidden() string { return "value" }
`,
	})
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	exported := exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
	require.Len(t, exported, 1)
	data, err := os.Open(exported[0])
	require.NoError(t, err)
	pkg, err := gcexportdata.Read(data, nil, make(map[string]*types.Package), "main")
	require.NoError(t, data.Close())
	require.NoError(t, err)
	assert.Nil(t, pkg.Scope().Lookup("hidden"))
	fileSet, files = astFromString("source.go", "package main\nfunc Source() string { return hidden() }")
	info, errors = typeCheckAst(context.Background(), fileSet, files, true, contextDir, module, ".", GcExporter{})
	require.Len(t, errors, 1)
	assert.Contains(t, errors[0].Error(), "undefined: hidden")
	assert.Nil(t, info.Uses[a3sFindCall(t, files["source.go"].ast, "hidden")])
	t.Log("Unexported helper absent from export data remains unresolved in source-only analysis")
}

func TestA3SContextPackageLayouts(t *testing.T) {
	for _, scenario := range []struct {
		name, module, moduleBase, directory, packageName string
	}{
		{"root", "example.com/mod", ".", ".", "main"},
		{"root-library", "example.com/mod", ".", ".", "mod"},
		{"nested", "example.com/mod", ".", "internal/service", "service"},
		{"test-suffix-directory", "example.com/mod", ".", "integration_test", "integration_test"},
		{"different-name", "example.com/mod", ".", "internal/service", "api"},
		{"nested-module", "example.com/mod", "apps/server", "internal/service", "service"},
		{"command", "example.com/mod", ".", "cmd/server", "main"},
	} {
		t.Run(scenario.name, func(t *testing.T) {
			contextDir := t.TempDir()
			fileName := filepath.Join(scenario.moduleBase, scenario.directory, "source.go")
			source := "package " + scenario.packageName + "\nfunc Source(value string) string { return Sink(value) }"
			sink := "package " + scenario.packageName + "\nfunc Sink(value string) string { return value }"
			fileSet, files := astFromStrings(map[string]string{fileName: source, filepath.Join(filepath.Dir(fileName), "sink.go"): sink})
			info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", scenario.module, scenario.moduleBase, GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			require.Len(t, exporter.ExportGcExportData(context.Background(), info, filepath.Join(contextDir, scenario.moduleBase), scenario.module, scenario.directory, false), 1)
			moduleBases := []string{"."}
			if scenario.moduleBase != "." {
				moduleBases = append(moduleBases, scenario.moduleBase)
			}
			for _, moduleBase := range moduleBases {
				for _, full := range []bool{false, true} {
					t.Run(fmt.Sprintf("base=%s/full=%t", moduleBase, full), func(t *testing.T) {
						sources := a3sLayoutSources(fileName, source, sink, full)
						fileSet, files := astFromStrings(sources)
						info, errors := typeCheckAst(context.Background(), fileSet, files, true, contextDir, scenario.module, moduleBase, GcExporter{})
						require.Empty(t, errors)
						assert.IsType(t, (*types.Func)(nil), info.Uses[a3sFindCall(t, files[fileName].ast, "Sink")])
					})
				}
			}
		})
	}
}

func a3sLayoutSources(fileName, source, sink string, full bool) map[string]string {
	sources := map[string]string{fileName: source}
	if full {
		sources[filepath.Join(filepath.Dir(fileName), "sink.go")] = sink
	}
	return sources
}

func TestA3SContextMissingOrInvalidData(t *testing.T) {
	for _, scenario := range []string{"no-directory", "missing", "invalid", "other-module", "external-test"} {
		t.Run(scenario, func(t *testing.T) {
			contextDir := t.TempDir()
			const module = "example.com/contextrepro"
			fileSet, files := astFromString("source.go", "package main\nfunc Source(value string) string { return Sink(value) }")
			seedSet, seedFiles := astFromString("sink.go", "package main\nfunc Sink(value string) string { return value }")
			info, errors := typeCheckAst(context.Background(), seedSet, seedFiles, false, "", module, ".", GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			switch scenario {
			case "other-module":
				exporter.ExportGcExportData(context.Background(), info, filepath.Join(contextDir, "other"), module, "", false)
			case "external-test":
				exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
			case "invalid":
				path := filepath.Join(contextDir, module, ".root.o")
				require.NoError(t, os.MkdirAll(filepath.Dir(path), 0755))
				require.NoError(t, os.WriteFile(path, []byte("invalid export data"), 0644))
			case "no-directory":
				contextDir = ""
			}
			if scenario == "external-test" {
				fileSet, files = astFromString("source_test.go", "package main_test\nfunc Source(value string) string { return Sink(value) }")
			}
			_, errors = typeCheckAst(context.Background(), fileSet, files, false, contextDir, module, ".", GcExporter{})
			require.Len(t, errors, 1)
			assert.Contains(t, errors[0].Error(), "undefined: Sink")
		})
	}
}

func TestA3SContextSharesImportedTypeIdentity(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/contextrepro"
	exporter := GcExporter{}
	fileSet, files := astFromString("otherpkg/value.go", "package otherpkg\ntype Value struct { Text string }")
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter.ExportGcExportData(context.Background(), info, contextDir, module, "otherpkg", false)
	source := `package main
import "example.com/contextrepro/otherpkg"
func Source(value otherpkg.Value) string { return Config{Value: value}.text() }
`
	fileSet, files = astFromStrings(map[string]string{
		"source.go": source,
		"config.go": `package main
import "example.com/contextrepro/otherpkg"
type Config struct { Value otherpkg.Value }
func (cfg Config) text() string { return cfg.Value.Text }
`,
	})
	info, errors = typeCheckAst(context.Background(), fileSet, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
	fileSet, files = astFromString("source.go", source)
	info, errors = typeCheckAst(context.Background(), fileSet, files, true, contextDir, module, ".", GcExporter{})
	assert.Empty(t, errors, "restored Config and directly imported Value must share type identity, including unexported methods")
}

func TestA3SContextExportIgnoresRestoredDeclarations(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/contextrepro"
	args := []string{"-dump_gc_export_data", "-gc_export_data_dir", contextDir, "-module_name", module}
	serveRequests(t, &server{},
		encodeRequest(args, map[string]string{"source.go": "package main\ntype Old int\nfunc Sink() Old { return 1 }"}),
		encodeRequest(args, map[string]string{"source.go": "package main\ntype Current string\nfunc Sink() Current { return \"value\" }"}))
	data, err := os.Open(filepath.Join(contextDir, module, ".root.o"))
	require.NoError(t, err)
	pkg, err := gcexportdata.Read(data, nil, make(map[string]*types.Package), "main")
	require.NoError(t, data.Close())
	require.NoError(t, err)
	assert.Nil(t, pkg.Scope().Lookup("Old"), "export must use submitted source only")
	require.NotNil(t, pkg.Scope().Lookup("Current"))
	assert.Equal(t, "func() main.Current", pkg.Scope().Lookup("Sink").Type().String())
}

func TestA3SContextMethodsOnOmittedReceiverDoNotPanic(t *testing.T) {
	for _, method := range []string{
		"func (cfg Config) Method(value string) string { return Sink(value) }",
		"func (cfg *Config) Method(value int) int { return value }",
		"type Alias = Config; func (cfg Alias) Method(value string) string { return Sink(value) }",
		"func (cfg Config[T]) Method(value T) T { return value }",
	} {
		t.Run(method, func(t *testing.T) {
			contextDir := t.TempDir()
			const module = "example.com/contextrepro"
			seedSet, seedFiles := astFromString("types.go", `package main
 type Config struct{}
 func (cfg Config) Method(value string) string { return value }
 func Sink(value string) string { return value }
`)
			info, errors := typeCheckAst(context.Background(), seedSet, seedFiles, false, "", module, ".", GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
			fileSet, files := astFromString("source.go", "package main\n"+method+"\nfunc Source(value string) string { return Sink(value) }")
			require.NotPanics(t, func() {
				info, errors = typeCheckAst(context.Background(), fileSet, files, false, contextDir, module, ".", GcExporter{})
			})
			require.NotEmpty(t, errors, "omitted receiver remains unresolved as in source-only checking")
			assert.Contains(t, errors[0].Error(), "undefined: Config")
			assert.IsType(t, (*types.Func)(nil), info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")])
		})
	}
}

func TestA3SContextDoesNotShadowStandardLibrary(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/contextrepro"
	fileSet, files := astFromString("helper.go", "package errors\nfunc Sink() string { return \"value\" }")
	info, errors := typeCheckAst(context.Background(), fileSet, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false), 1)
	fileSet, files = astFromString("source.go", `package errors
import "errors"
func Source() error { return errors.New(Sink()) }
`)
	info, errors = typeCheckAst(context.Background(), fileSet, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	standard := info.Uses[a3sFindCall(t, files["source.go"].ast, "New")]
	restored := info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")]
	require.NotNil(t, standard)
	require.NotNil(t, restored)
	assert.Equal(t, "errors", standard.Pkg().Path())
	assert.NotSame(t, standard.Pkg(), restored.Pkg())

}

func TestA3SContextSkipsDeclarationsWithReplacedTypes(t *testing.T) {
	for _, signature := range []string{
		"func Load() Config { return Config{} }",
		"type Wrapper struct { Value Config }; func Load() Wrapper { return Wrapper{} }",
		"type Wrapper struct{}; func (Wrapper) Get() Config { return Config{} }; func Load() Wrapper { return Wrapper{} }",
		"type Alias = Config; func Load() Alias { return Alias{} }",
		"func Load() *Config { return nil }",
		"func Load() []Config { return nil }",
		"func Load() [1]Config { return [1]Config{} }",
		"func Load() map[string]Config { return nil }",
		"func Load() chan Config { return nil }",
		"func Load() interface { Get() Config } { return nil }",
		"type Box[T any] struct{ Value T }; func Load() Box[Config] { return Box[Config]{} }",
	} {
		t.Run(signature, func(t *testing.T) {
			contextDir := t.TempDir()
			const module = "example.com/mod"
			fset, files := astFromString("load.go", "package main\ntype Config struct{ V int }\n"+signature)
			info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false), 1)
			fset, files = astFromString("config.go", "package main\ntype Config struct{ V int; W int }\nfunc Use() int { var c Config = Load(); return c.W }")
			info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
			require.Len(t, errors, 1)
			assert.Contains(t, errors[0].Error(), "undefined: Load")
			assert.Nil(t, info.Uses[a3sFindCall(t, files["config.go"].ast, "Load")])
			var configType, variableType types.Type
			for ident, object := range info.Defs {
				if ident.Name == "Config" {
					configType = object.Type()
				}
				if ident.Name == "c" {
					variableType = object.Type()
				}
			}
			assert.Same(t, configType, variableType)
		})
	}
}

func TestA3SContextDoesNotExportMixedPackageDirectory(t *testing.T) {
	fset, files := astFromStrings(map[string]string{
		"fixedbugs/a.go": "package main\nfunc main() {}",
		"fixedbugs/b.go": "package main\nfunc main() {}",
		"fixedbugs/c.go": "package p\nfunc F() {}",
	})
	info, originalErrors := typeCheckAst(context.Background(), fset, files, true, "", "example.com/mod", ".", GcExporter{})
	exporter := GcExporter{}
	contextDir := t.TempDir()
	assert.Empty(t, exporter.ExportGcExportData(context.Background(), info, contextDir, "example.com/mod", "fixedbugs", false))
	_, restoredErrors := typeCheckAst(context.Background(), fset, files, true, contextDir, "example.com/mod", ".", GcExporter{})
	require.Len(t, originalErrors, 1)
	require.Len(t, restoredErrors, 1)
	assert.Equal(t, originalErrors[0].Error(), restoredErrors[0].Error())
}

func TestA3SContextImportsPackageWithDifferentName(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromString("internal/service/sink.go", "package api\nfunc Sink() string { return \"value\" }")
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "internal/service", false), 1)
	fset, files = astFromString("source.go", "package main\nimport \"example.com/mod/internal/service\"\nfunc Source() string { return api.Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	assert.NotNil(t, info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")])
}

func TestA3SContextRejectsAmbiguousModuleBase(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromString("sink.go", "package service\nfunc Sink() string { return \"value\" }")
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	for _, base := range []string{"apps", "apps/server"} {
		dir, err := filepath.Rel(base, "apps/server/internal/service")
		require.NoError(t, err)
		require.Len(t, exporter.ExportGcExportData(context.Background(), info, filepath.Join(contextDir, base), module, dir, false), 1)
	}
	fset, files = astFromString("apps/server/internal/service/source.go", "package service\nfunc Source() string { return Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
	require.Len(t, errors, 1)
	assert.Contains(t, errors[0].Error(), "undefined: Sink")
	assert.Nil(t, info.Uses[a3sFindCall(t, files["apps/server/internal/service/source.go"].ast, "Sink")])
}

func TestA3SContextDoesNotSeedExternalTestPackage(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromString("internal/service/sink.go", "package service\nfunc Sink() string { return \"value\" }")
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "internal/service", false), 1)
	fset, files = astFromString("internal/service/source_test.go", "package service_test\nfunc Source() string { return Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
	require.Len(t, errors, 1)
	assert.Contains(t, errors[0].Error(), "undefined: Sink")
	assert.Nil(t, info.Uses[a3sFindCall(t, files["internal/service/source_test.go"].ast, "Sink")])
}

func TestA3SContextRootExportExcludesExternalTests(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromStrings(map[string]string{
		"sink.go":     "package api\nfunc Sink() string { return \"value\" }",
		"api_test.go": "package api_test\nfunc ExternalTestHelper() {}",
	})
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	paths := exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
	require.Equal(t, []string{filepath.Join(contextDir, module, ".root.o")}, paths)
	fset, files = astFromString("source.go", "package api\nfunc Source() string { return Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	assert.NotNil(t, info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")])
}

func TestA3SContextRootExportWithGenerator(t *testing.T) {
	for _, module := range []string{"example.com/api", "example.com/mod"} {
		t.Run(module, func(t *testing.T) {
			contextDir := t.TempDir()
			fset, files := astFromStrings(map[string]string{
				"sink.go":     "package api\nfunc Sink() string { return \"value\" }",
				"generate.go": "//go:build ignore\n\npackage main\nfunc main() {}",
			})
			info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			paths := exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false)
			if module == "example.com/mod" {
				assert.Empty(t, paths)
				assert.NoFileExists(t, filepath.Join(contextDir, module, ".root.o"))
				return
			}
			require.Equal(t, []string{filepath.Join(contextDir, module, "api.o")}, paths)
			fset, files = astFromString("source.go", "package api\nfunc Source() string { return Sink() }")
			info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
			require.Empty(t, errors)
			assert.NotNil(t, info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")])
		})
	}
}

func TestA3SContextTestSuffixDirectoryPrefersProduction(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromStrings(map[string]string{
		"foo_test/sink.go":     "package foo\nfunc Sink() string { return \"value\" }",
		"foo_test/foo_test.go": "package foo_test\nfunc ExternalTestHelper() {}",
	})
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	paths := exporter.ExportGcExportData(context.Background(), info, contextDir, module, "foo_test", false)
	require.Equal(t, []string{filepath.Join(contextDir, module, "foo_test", "foo_test.o")}, paths)
	fset, files = astFromString("foo_test/source.go", "package foo\nfunc Source() string { return Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	assert.NotNil(t, info.Uses[a3sFindCall(t, files["foo_test/source.go"].ast, "Sink")])
}

func TestA3SContextImportsRootLibrary(t *testing.T) {
	for _, base := range []string{".", "apps/server"} {
		t.Run(base, func(t *testing.T) {
			contextDir := t.TempDir()
			const module = "example.com/library"
			fset, files := astFromString("sink.go", "package library\nfunc Sink() string { return \"value\" }")
			info, errors := typeCheckAst(context.Background(), fset, files, true, "", module, base, GcExporter{})
			require.Empty(t, errors)
			exporter := GcExporter{}
			paths := exporter.ExportGcExportData(context.Background(), info, filepath.Join(contextDir, base), module, "", false)
			require.Equal(t, []string{filepath.Join(contextDir, base, module, "library.o")}, paths)
			fset, files = astFromString("cmd/tool/source.go", "package main\nimport \"example.com/library\"\nfunc Source() string { return library.Sink() }")
			info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, module, ".", GcExporter{})
			require.Empty(t, errors)
			assert.NotNil(t, info.Uses[a3sFindCall(t, files["cmd/tool/source.go"].ast, "Sink")])
		})
	}
}

func TestA3SContextExportsDirectoryPackageWithGenerator(t *testing.T) {
	contextDir := t.TempDir()
	fset, files := astFromStrings(map[string]string{
		"service/sink.go": "package service\nfunc Sink() string { return \"value\" }",
		"service/gen.go":  "//go:build ignore\n\npackage main\nfunc main() {}",
	})
	info, errors := typeCheckAst(context.Background(), fset, files, true, "", "example.com/mod", ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Equal(t, []string{filepath.Join(contextDir, "example.com/mod/service/service.o")}, exporter.ExportGcExportData(context.Background(), info, contextDir, "example.com/mod", "service", false))
	fset, files = astFromString("source.go", "package main\nimport \"example.com/mod/service\"\nfunc Source() string { return service.Sink() }")
	info, errors = typeCheckAst(context.Background(), fset, files, true, contextDir, "example.com/mod", ".", GcExporter{})
	require.Empty(t, errors)
	assert.NotNil(t, info.Uses[a3sFindCall(t, files["source.go"].ast, "Sink")])
}

func TestA3SContextGenericInstantiationCycle(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromString("list.go", `package main
 type List[T any] struct{ V T }
 func (l List[T]) Wrap() List[[]T] { return List[[]T]{} }
 `)
	info, _ := typeCheckAst(context.Background(), fset, files, false, "", module, ".", GcExporter{})
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false), 1)
	fset, files = astFromString("source.go", "package main\nfunc Source() int { return 1 }")
	started := time.Now()
	_, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	assert.Less(t, time.Since(started), 5*time.Second)
}

func TestA3SContextLargeCyclicTypeGraph(t *testing.T) {
	const count = 2000
	var source strings.Builder
	source.WriteString("package main\n")
	for i := 0; i < count; i++ {
		fmt.Fprintf(&source, "type T%d struct { A *T%d; B []T%d; C map[string]*T%d }; func (T%d) Method() {}; func New%d() T%d { return T%d{} }\n", i, (i+1)%count, (i+7)%count, (i+13)%count, i, i, i, i)
	}
	contextDir := t.TempDir()
	const module = "example.com/mod"
	fset, files := astFromString("generated.go", source.String())
	info, errors := typeCheckAst(context.Background(), fset, files, false, "", module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, "", false), 1)
	for _, replacement := range []string{"", "type T1999 struct{}"} {
		fset, files = astFromString("source.go", "package main\n"+replacement+"\nfunc Source() { _ = New0() }")
		started := time.Now()
		info, errors = typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
		elapsed := time.Since(started)
		t.Logf("replacement=%t: %s", replacement != "", elapsed)
		assert.Less(t, elapsed, 5*time.Second)
		symbol := info.Uses[a3sFindCall(t, files["source.go"].ast, "New0")]
		if replacement == "" {
			require.Empty(t, errors)
			require.NotNil(t, symbol)
		} else {
			require.NotEmpty(t, errors)
			assert.Nil(t, symbol)
		}
	}
}

func TestA3SContextKeepsHelpersForOmittedReceiver(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	a3sExportSource(t, contextDir, module, "", "package main\ntype Server struct{ Name string }; func NewServer() *Server { return &Server{} }")
	fset, files := astFromString("handlers.go", "package main\nfunc (s *Server) Handle() {}\nfunc Source() string { return NewServer().Name }")
	info, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
	require.Len(t, errors, 1)
	assert.Contains(t, errors[0].Error(), "undefined: Server")
	assert.NotNil(t, info.Uses[a3sFindCall(t, files["handlers.go"].ast, "NewServer")])
}

func TestA3SContextRootAndSameNamedSubdirectory(t *testing.T) {
	for _, rootFirst := range []bool{true, false} {
		t.Run(fmt.Sprint(rootFirst), func(t *testing.T) {
			contextDir := t.TempDir()
			const module = "example.com/mod"
			dirs := []string{"", "main"}
			if !rootFirst {
				dirs[0], dirs[1] = dirs[1], dirs[0]
			}
			for _, dir := range dirs {
				result := "string { return \"root\" }"
				if dir != "" {
					result = "int { return 1 }"
				}
				a3sExportSource(t, contextDir, module, dir, "package main\nfunc Helper() "+result)
			}
			a3sCheckRootAndSubdirectory(t, contextDir, module)
		})
	}
}

func TestA3SContextUsesFreshSubmittedImports(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	a3sExportSource(t, contextDir, module, "model", "package model\ntype User struct{ Name string }")
	a3sExportSource(t, contextDir, module, "api", "package api\nimport \"example.com/mod/model\"\nfunc Get() model.User { return model.User{} }")
	a3sExportSource(t, contextDir, module, "model", "package model\ntype User struct{ Name, Email string }")
	for _, full := range []bool{false, true} {
		sources := map[string]string{"api/use.go": "package api\nimport \"example.com/mod/model\"\nfunc Use() string { var u model.User; return u.Email }"}
		if full {
			sources["api/get.go"] = "package api\nimport \"example.com/mod/model\"\nfunc Get() model.User { return model.User{} }"
		}
		fset, files := astFromStrings(sources)
		_, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
		assert.Empty(t, errors)
	}
}

func TestA3SContextStandardLibraryNamesakeBoundary(t *testing.T) {
	contextDir := t.TempDir()
	const module = "example.com/mod"
	a3sExportSource(t, contextDir, module, "internal/context", "package context\nimport stdctx \"context\"\nfunc Wrap(ctx stdctx.Context) stdctx.Context { return ctx }")
	fset, files := astFromString("internal/context/source.go", "package context\nimport stdctx \"context\"\nfunc Source(ctx stdctx.Context) { _ = Wrap(ctx) }")
	info, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
	require.NotEmpty(t, errors)
	assert.Nil(t, info.Uses[a3sFindCall(t, files["internal/context/source.go"].ast, "Wrap")])
}

func a3sExportSource(t *testing.T, contextDir, module, dir, source string) {
	t.Helper()
	fset, files := astFromString(filepath.Join(dir, "lib.go"), source)
	info, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
	require.Empty(t, errors)
	exporter := GcExporter{}
	require.Len(t, exporter.ExportGcExportData(context.Background(), info, contextDir, module, dir, false), 1)
}

func a3sCheckRootAndSubdirectory(t *testing.T, contextDir, module string) {
	t.Helper()
	for _, dir := range []string{"", "main"} {
		result := "string"
		if dir != "" {
			result = "int"
		}
		fset, files := astFromString(filepath.Join(dir, "extra.go"), "package main\nfunc Use() "+result+" { return Helper() }")
		_, errors := typeCheckAst(context.Background(), fset, files, false, contextDir, module, ".", GcExporter{})
		assert.Empty(t, errors)
	}
}
