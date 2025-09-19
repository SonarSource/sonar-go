package main

import (
	"fmt"
	"go/types"
	"io/fs"
	"os"
	"path/filepath"
	"strings"

	"golang.org/x/tools/go/gcexportdata"
)

type GcExporter struct {
	packagesImportIssueInvalidMemoryAddress []string
}

func (gc *GcExporter) ExportGcExportData(info *types.Info, exportDataDir string, moduleName string, packagePath string, debugTypeCheck bool) {
	packagesToExport := gc.findPackagesToExport(info, packagePath)

	for _, pkgToExport := range packagesToExport {
		gc.exportPackage(pkgToExport, exportDataDir, moduleName, packagePath, debugTypeCheck)
	}
}

func (gc *GcExporter) PrintExportIssues() {
	size := len(gc.packagesImportIssueInvalidMemoryAddress)
	if size > 0 {
		limit := 10
		if size < 10 {
			limit = size
		}
		fmt.Fprintf(os.Stderr,
			"Found %d issues of type 'internal error while importing: invalid memory address or nil pointer dereference' for packages: %v",
			len(gc.packagesImportIssueInvalidMemoryAddress),
			gc.packagesImportIssueInvalidMemoryAddress[:limit])
		if limit != size {
			fmt.Fprintf(os.Stderr, " (%d more not shown)", size-limit)
		}
		fmt.Fprintln(os.Stderr)
	}
}

func (gc *GcExporter) findPackagesToExport(info *types.Info, packagePath string) []*types.Package {
	packagesToExport := make([]*types.Package, 0)
	packageName := getPackageName(packagePath)
	if gc.isBlank(packageName) {
		// if there is a blank package name, we export all packages
		// this happens when Go files are located directly in the project root directory (at the same level as go.mod)
		packagesToExport = gc.findMultiplePackages(info, packagesToExport)
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

func (gc *GcExporter) findMultiplePackages(info *types.Info, packagesToExport []*types.Package) []*types.Package {
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

func (gc *GcExporter) exportPackage(pkgToExport *types.Package, exportDataDir string, moduleName string, packagePath string, debugTypeCheck bool) {
	fullExportDataDir := filepath.Join(exportDataDir, moduleName, packagePath)
	if !strings.HasSuffix(fullExportDataDir, pkgToExport.Path()) {
		fullExportDataDir = filepath.Join(fullExportDataDir, pkgToExport.Path())
	}
	gc.createDirs(fullExportDataDir)

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

func (gc *GcExporter) isBlank(text string) bool {
	return len(strings.TrimSpace(text)) == 0
}

func (gc *GcExporter) createDirs(path string) {
	if !gc.isBlank(path) {
		err := os.MkdirAll(path, 0755)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error creating directory: %s\n", err)
			panic("Error creating directory")
		}
	}
}

func (gc *GcExporter) getPackageFromFile(file fs.File, path string) (*types.Package, error) {
	defer func(file fs.File) {
		_ = file.Close()
	}(file)
	imports := make(map[string]*types.Package)
	pkg, err := gcexportdata.Read(file, nil, imports, path)
	if err != nil {
		msg := err.Error()
		if strings.HasPrefix(msg, "internal error while importing") && strings.HasSuffix(msg, "(runtime error: invalid memory address or nil pointer dereference); please report an issue") {
			gc.packagesImportIssueInvalidMemoryAddress = append(gc.packagesImportIssueInvalidMemoryAddress, path)
		} else {
			fmt.Fprintf(os.Stderr, "Error while reading gcexportdata of %s error: %s\n", path, err)
		}
		return getEmptyPackage(path), nil
	}

	return pkg, nil
}
