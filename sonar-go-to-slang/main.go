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
	"flag"
	"fmt"
	"go/token"
	"io"
	"os"
	"path/filepath"
	"strings"
)

type Params struct {
	dumpAst          bool
	debugTypeCheck   bool
	dumpGcExportData bool
	gcExportDataDir  string
	moduleName       string
	moduleBaseDir    string
	packagePath      string
}

// defineFlags registers the options of a request on flagSet and returns a function reading their values once
// flagSet is parsed.
func defineFlags(flagSet *flag.FlagSet) func() Params {
	dumpAstFlag := flagSet.Bool("d", false, "dump ast (instead of the Slang AST)")
	debugTypeCheckFlag := flagSet.Bool("debug_type_check", false, "print errors logs from type checking")
	dumpGcExportData := flagSet.Bool("dump_gc_export_data", false, "dump GC export data")
	gcExportDataDir := flagSet.String("gc_export_data_dir", "", "directory where GC export data is located")
	moduleName := flagSet.String("module_name", "", "specify module name (defined in go.mod)")
	moduleBaseDir := flagSet.String("module_base_dir", ".", "relative path of the go.mod directory from the project root")
	packagePath := flagSet.String("package_path", "", "specify package path (e.g. foo/bar for files located in ${projectDir}/foo/bar)")

	return func() Params {
		return Params{
			dumpAst:          *dumpAstFlag,
			debugTypeCheck:   *debugTypeCheckFlag,
			dumpGcExportData: *dumpGcExportData,
			gcExportDataDir:  *gcExportDataDir,
			moduleName:       *moduleName,
			moduleBaseDir:    *moduleBaseDir,
			packagePath:      *packagePath,
		}
	}
}

func logParams(params Params) {
	fmt.Fprintf(os.Stderr, "Received parameters: dumpAst=%t, debugTypeCheck=%t, dumpGcExportData=%t, gcExportDataDir=\"%s\", moduleName=\"%s\", moduleBaseDir=\"%s\", packagePath=\"%s\"\n",
		params.dumpAst, params.debugTypeCheck, params.dumpGcExportData, params.gcExportDataDir, params.moduleName, params.moduleBaseDir, params.packagePath)
}

// laneLabel names this invocation's lane in the timeline.
// The logic keeps the label stable across runs regardless of map iteration order.
func laneLabel(params Params, fileContents map[string]string) string {
	if params.packagePath != "" {
		return params.packagePath
	}
	dir := ""
	for name := range fileContents {
		// `<` on strings compares them byte by byte, so this keeps the lexicographically first
		// directory of the batch: an arbitrary pick, but the same one on every run.
		if candidate := filepath.Dir(name); dir == "" || candidate < dir {
			dir = candidate
		}
	}
	if dir == "" || dir == "." {
		return params.moduleName
	}
	return dir
}

// exit is replaced by tests, which cannot let the process exit.
var exit = os.Exit

func main() {
	initTracing()
	defer shutdownTracing()

	if len(os.Args) > 1 {
		fmt.Fprint(os.Stderr, usage(os.Args[0]))
		shutdownTracing()
		exit(2)
		return
	}

	setTraceProcessLabel()
	runServer()
}

// usage explains that the options are not given on the command line, but with each request.
func usage(command string) string {
	var text strings.Builder
	text.WriteString("Usage: " + command + "\n\n")
	text.WriteString("Takes no argument: serves the requests read from stdin until it is closed (see README.md).\n")
	text.WriteString("Each request carries the files to analyze and the following options:\n")
	flagSet := flag.NewFlagSet(command, flag.ContinueOnError)
	flagSet.SetOutput(&text)
	defineFlags(flagSet)
	flagSet.PrintDefaults()
	return text.String()
}

func analysisSpanArgs(params Params) []any {
	return []any{
		"moduleName", params.moduleName,
		"moduleBaseDir", params.moduleBaseDir,
		"packagePath", params.packagePath,
		"hasGcExportDataDir", params.gcExportDataDir != "",
		"pid", os.Getpid(),
	}
}

// analysisStats describes the input of one request for its trace span. The counts are only filled when they are
// known, so that a span ended by a panic still reports what had been read.
type analysisStats struct {
	fileCount     int
	sourceBytes   int
	laneLabel     string
	exportedFiles []string
}

// analyze reads the files from in, type checks them, and then either writes the GC export data to disk or writes
// the Slang AST (or the Go AST) to out. It is the whole work of one request.
// It panics on an invalid input, which the server recovers from to fail only that request.
//
// crossIndex is the index of the GC export data directory to resolve cross-module imports with; nil means that a
// new one is built, lazily, for this analysis only.
func analyze(ctx context.Context, params Params, in io.Reader, out io.Writer, crossIndex *crossModuleIndex, stats *analysisStats) {
	fileSet := token.NewFileSet()
	astFiles, fileContents, err := readAstFile(ctx, fileSet, in)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading AST file: %v\n", err)
		panic(err)
	}
	stats.fileCount = len(astFiles)
	if tracingEnabled() {
		for _, fileContent := range fileContents {
			stats.sourceBytes += len(fileContent)
		}
		stats.laneLabel = laneLabel(params, fileContents)
	}

	if crossIndex == nil {
		crossIndex = &crossModuleIndex{dir: params.gcExportDataDir}
	}
	gcExporter := GcExporter{}
	// Ignoring errors at this point, they are reported before if needed
	info, _ := typeCheckAstWithCrossIndex(ctx, fileSet, astFiles, params.debugTypeCheck, params.gcExportDataDir, params.moduleName, params.moduleBaseDir, gcExporter, crossIndex)
	heapCounter("heap.afterTypeCheck")

	if params.dumpGcExportData {
		if params.gcExportDataDir == "" {
			panic("If the dump_gc_export_data flag is set then the gc_export_data_dir flag must be set too")
		}
		stats.exportedFiles = gcExporter.ExportGcExportData(ctx, info, params.gcExportDataDir, params.moduleName, params.packagePath, params.debugTypeCheck)
		return
	}

	if params.dumpAst {
		writeOutput(out, []byte(render(astFiles)))
	} else {
		// The frames are written straight to out, rather than returned, to keep one copy of a large
		// batch rather than two.
		toSlangProto(ctx, fileSet, astFiles, fileContents, info, params.moduleName, out)
	}
	gcExporter.PrintExportIssues()
}

func writeOutput(out io.Writer, output []byte) {
	// A truncated output must fail the analysis rather than pass for a complete one.
	if _, err := out.Write(output); err != nil {
		panic(fmt.Sprintf("cannot write the output: %v", err))
	}
}
