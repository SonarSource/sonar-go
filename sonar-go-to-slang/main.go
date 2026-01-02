// SonarQube Go Plugin
// Copyright (C) 2018-2026 SonarSource Sàrl
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
	"flag"
	"fmt"
	"go/token"
	"os"
)

type Params struct {
	dumpAst          bool
	debugTypeCheck   bool
	dumpGcExportData bool
	gcExportDataDir  string
	moduleName       string
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
	packagePath := flag.String("package_path", "", "specify package path (e.g. foo/bar for files located in ${projectDir}/foo/bar)")
	flag.Parse()

	fmt.Fprintf(os.Stderr, "Received parameters: dumpAst=%t, debugTypeCheck=%t, dumpGcExportData=%t, gcExportDataDir=\"%s\", moduleName=\"%s\", packagePath=\"%s\"\n",
		*dumpAstFlag, *debugTypeCheckFlag, *dumpGcExportData, *gcExportDataDir, *moduleName, *packagePath)

	return Params{
		dumpAst:          *dumpAstFlag,
		debugTypeCheck:   *debugTypeCheckFlag,
		dumpGcExportData: *dumpGcExportData,
		gcExportDataDir:  *gcExportDataDir,
		moduleName:       *moduleName,
		packagePath:      *packagePath,
	}
}

func main() {
	params := parseArgs()

	fileSet := token.NewFileSet()
	astFiles, fileContents, err := readAstFile(fileSet, os.Stdin)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading AST file: %v\n", err)
		panic(err)
	}

	gcExporter := GcExporter{}
	// Ignoring errors at this point, they are reported before if needed
	info, _ := typeCheckAst(fileSet, astFiles, params.debugTypeCheck, params.gcExportDataDir, params.moduleName, gcExporter)

	if params.dumpGcExportData {
		if params.gcExportDataDir == "" {
			panic("If the dump_gc_export_data flag is set then the gc_export_data_dir flag must be set too")
		}
		gcExporter.ExportGcExportData(info, params.gcExportDataDir, params.moduleName, params.packagePath, params.debugTypeCheck)
		return
	}

	if params.dumpAst {
		fmt.Println(render(astFiles))
	} else {
		json := toSlangJson(fileSet, astFiles, fileContents, info, params.moduleName, "")
		fmt.Println(json)
	}
	gcExporter.PrintExportIssues()
}
