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
	packagesToExport := findPackagesToExport(info, packagePath)

	for _, pkgToExport := range packagesToExport {
		exportPackage(pkgToExport, exportDataDir, moduleName, packagePath, debugTypeCheck)
	}
}

func findPackagesToExport(info *types.Info, packagePath string) []*types.Package {
	packagesToExport := make([]*types.Package, 0)
	packageName := getPackageName(packagePath)
	if isBlank(packageName) {
		// if there is a blank package name, we export all packages
		// this happens when Go files are located directly in the project root directory (at the same level as go.mod)
		packagesToExport = findMultiplePackages(info, packagesToExport)
	} else {
		// otherwise, we export only packages that match the given package name
		// this prevent exporting unintended packages, example:
		// the dir "utils" contains files with package name "utils" and "utils_test", so only "utils" should be exported
		for _, obj := range info.Defs {
			if obj != nil && obj.Pkg() != nil && obj.Pkg().Path() == packageName {
				packagesToExport = []*types.Package{obj.Pkg()}
				break
			}
		}
	}

	return packagesToExport
}

func findMultiplePackages(info *types.Info, packagesToExport []*types.Package) []*types.Package {
	pkgToExport := map[string]*types.Package{}
	for _, obj := range info.Defs {
		if obj != nil && obj.Pkg() != nil {
			pkgToExport[obj.Pkg().Path()] = obj.Pkg()
		}
	}

	for _, pkg := range pkgToExport {
		packagesToExport = append(packagesToExport, pkg)
	}
	return packagesToExport
}

func exportPackage(pkgToExport *types.Package, exportDataDir string, moduleName string, packagePath string, debugTypeCheck bool) {
	fullExportDataDir := filepath.Join(exportDataDir, moduleName, packagePath)
	if !strings.HasSuffix(fullExportDataDir, pkgToExport.Path()) {
		fullExportDataDir = filepath.Join(fullExportDataDir, pkgToExport.Path())
	}
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
