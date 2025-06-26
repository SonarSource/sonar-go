package main

import (
	"fmt"
	"go/types"
	"golang.org/x/tools/go/gcexportdata"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
)

func exportGcExportData(info *types.Info, exportDataDir string, moduleName string, packagePath string, debugTypeCheck bool) {
	var pkgToExport *types.Package
	for _, obj := range info.Defs {
		if obj != nil && obj.Pkg() != nil {
			pkgToExport = obj.Pkg()
			break
		}
	}
	if pkgToExport == nil {
		return
	}

	fullExportDataDir := filepath.Join(exportDataDir, moduleName, packagePath)
	createDirs(fullExportDataDir)

	exportDataFile := filepath.Join(fullExportDataDir, pkgToExport.Path()+".o")

	file, err := os.Create(exportDataFile)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error creating file: %s\n", err)
		panic("Error creating file")
	}

	defer func(file *os.File) {
		_ = file.Close()
	}(file)

	if debugTypeCheck {
		fmt.Fprintf(os.Stderr, "Writing gcexportdata to file: \"%s\", num of exported elements: %d\n", exportDataFile, pkgToExport.Scope().Len())
	}
	err = gcexportdata.Write(file, nil, pkgToExport)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error writing gcexportdata: %s\n", err)
		panic("Error writing gcexportdata")
	}
}

func isBlank(text string) bool {
	return len(strings.TrimSpace(text)) == 0
}

func createDirs(path string) {
	if !isBlank(path) {
		err := os.MkdirAll(path, 0755)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error creating directory: %s\n", err)
			panic("Error creating directory")
		}
	}
}

func getPackageFromFile(file fs.File, path string) (*types.Package, error) {
	defer func(file fs.File) {
		_ = file.Close()
	}(file)
	imports := make(map[string]*types.Package)
	pkg, err := gcexportdata.Read(file, nil, imports, path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while reading gcexportdata of %s error: %s\n", path, err)
		return getEmptyPackage(path), nil
	}

	return pkg, nil
}
