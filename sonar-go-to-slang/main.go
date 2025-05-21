// SonarQube Go Plugin
// Copyright (C) 2018-2025 SonarSource SA
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
	path             string
	dumpGcExportData bool
	gcExportDataFile string
	gcExportDataDir  string
}

func parseArgs() Params {
	flag.Usage = func() {
		fmt.Printf("Usage: %s [options] [- | path]\n\n", os.Args[0])
		flag.PrintDefaults()
	}

	dumpAstFlag := flag.Bool("d", false, "dump ast (instead of JSON)")
	debugTypeCheckFlag := flag.Bool("debug_type_check", false, "print errors logs from type checking")
	dumpGcExportData := flag.Bool("dump_gc_export_data", false, "dump GC export data")
	gcExportDataFile := flag.String("gc_export_data_file", "", "file to dump GC export data")
	gcExportDataDir := flag.String("gc_export_data_dir", "", "directory where GC export data is located")
	flag.Parse()
	var path string
	if len(flag.Args()) == 1 {
		path = flag.Args()[0]
	}

	return Params{
		dumpAst:          *dumpAstFlag,
		debugTypeCheck:   *debugTypeCheckFlag,
		path:             path,
		dumpGcExportData: *dumpGcExportData,
		gcExportDataFile: *gcExportDataFile,
		gcExportDataDir:  *gcExportDataDir,
	}
}

func main() {
	params := parseArgs()

	fileSet := token.NewFileSet()
	astFile, fileContent, _ := readAstFile(fileSet, params.path)

	info, _ := typeCheckAst(params.path, fileSet, astFile, params.debugTypeCheck, params.gcExportDataDir)
	// Ignoring errors at this point, they are reported before if needed.

	if params.dumpGcExportData {
		if params.gcExportDataFile == "" {
			panic("If the dump_gc_export_data flag is set then the gc_export_data_file flag must be set too")
		}
		exportGcExportData(info, params.gcExportDataFile)
		return
	}

	if params.dumpAst {
		fmt.Println(render(astFile))
	} else {
		slangTree, comments, tokens := toSlangTree(fileSet, astFile, fileContent, info)
		fmt.Println(toJsonSlang(slangTree, comments, tokens, ""))
	}
}
