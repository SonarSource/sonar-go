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
	"os"
	"path/filepath"
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

func parseArgs() Params {
	flag.Usage = func() {
		fmt.Printf("Usage: %s [options] [- | path]\n\n", os.Args[0])
		flag.PrintDefaults()
	}

	dumpAstFlag := flag.Bool("d", false, "dump ast (instead of JSON)")
	debugTypeCheckFlag := flag.Bool("debug_type_check", false, "print errors logs from type checking")
	dumpGcExportData := flag.Bool("dump_gc_export_data", false, "dump GC export data")
	gcExportDataDir := flag.String("gc_export_data_dir", "", "directory where GC export data is located")
	moduleName := flag.String("module_name", "", "specify module name (defined in go.mod)")
	moduleBaseDir := flag.String("module_base_dir", ".", "relative path of the go.mod directory from the project root")
	packagePath := flag.String("package_path", "", "specify package path (e.g. foo/bar for files located in ${projectDir}/foo/bar)")
	flag.Parse()

	fmt.Fprintf(os.Stderr, "Received parameters: dumpAst=%t, debugTypeCheck=%t, dumpGcExportData=%t, gcExportDataDir=\"%s\", moduleName=\"%s\", moduleBaseDir=\"%s\", packagePath=\"%s\"\n",
		*dumpAstFlag, *debugTypeCheckFlag, *dumpGcExportData, *gcExportDataDir, *moduleName, *moduleBaseDir, *packagePath)

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

func main() {
	initTracing()
	defer shutdownTracing()

	params := parseArgs()

	setTraceProcessLabel()
	heapCounter("heap.start")

	ctx, mainDone := span(context.Background(), "main",
		"moduleName", params.moduleName,
		"moduleBaseDir", params.moduleBaseDir,
		"packagePath", params.packagePath,
		"hasGcExportDataDir", params.gcExportDataDir != "",
		"pid", os.Getpid())
	// Called here, and not before the span above, because a trace viewer attaches a flow-finish event
	// to the span that encloses it and drops the arrow entirely when no span does.
	flowFinish()
	fileCount, sourceBytes := 0, 0
	// Deferred because the gc export data path returns early, and because the counts are only known
	// once the input has been read.
	defer func() {
		mainDone("fileCount", fileCount, "sourceBytes", sourceBytes)
		heapCounter("heap.end")
	}()

	fileSet := token.NewFileSet()
	astFiles, fileContents, err := readAstFile(ctx, fileSet, os.Stdin)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading AST file: %v\n", err)
		panic(err)
	}
	fileCount = len(astFiles)
	if tracingEnabled() {
		for _, fileContent := range fileContents {
			sourceBytes += len(fileContent)
		}
		setTraceLaneLabel(laneLabel(params, fileContents))
	}

	gcExporter := GcExporter{}
	// Ignoring errors at this point, they are reported before if needed
	info, _ := typeCheckAst(ctx, fileSet, astFiles, params.debugTypeCheck, params.gcExportDataDir, params.moduleName, params.moduleBaseDir, gcExporter)
	heapCounter("heap.afterTypeCheck")

	if params.dumpGcExportData {
		if params.gcExportDataDir == "" {
			panic("If the dump_gc_export_data flag is set then the gc_export_data_dir flag must be set too")
		}
		gcExporter.ExportGcExportData(ctx, info, params.gcExportDataDir, params.moduleName, params.packagePath, params.debugTypeCheck)
		return
	}

	if params.dumpAst {
		fmt.Println(render(astFiles))
	} else {
		json := toSlangJson(ctx, fileSet, astFiles, fileContents, info, params.moduleName, "")
		fmt.Println(json)
	}
	gcExporter.PrintExportIssues()
}
