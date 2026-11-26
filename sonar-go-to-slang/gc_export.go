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

func (gc *GcExporter) ExportGcExportData(ctx context.Context, info *types.Info, exportDataDir, moduleName, packagePath string, debugTypeCheck bool) []string {
	packagesToExport := gc.findPackagesToExport(info, moduleName, packagePath)

	exportedFiles := make([]string, 0, len(packagesToExport))
	for _, pkgToExport := range packagesToExport {
		exportedFiles = append(exportedFiles, gc.exportPackage(ctx, pkgToExport, exportDataDir, moduleName, packagePath, debugTypeCheck))
	}
	return exportedFiles
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

func (gc *GcExporter) findPackagesToExport(info *types.Info, moduleName, packagePath string) []*types.Package {
	packagesToExport := make([]*types.Package, 0)
	packageName := getPackageName(packagePath)
	var testNameMatch *types.Package
	if gc.isBlank(packageName) {
		packageName = filepath.Base(moduleName)
	}
	for _, pkg := range gc.findMultiplePackages(info, nil) {
		if strings.HasSuffix(pkg.Name(), "_test") {
			if pkg.Name() == packageName {
				testNameMatch = pkg
			}
			continue
		}
		if pkg.Name() == packageName {
			return []*types.Package{pkg}
		}
		packagesToExport = append(packagesToExport, pkg)
	}
	if len(packagesToExport) == 0 && testNameMatch != nil {
		return []*types.Package{testNameMatch}
	}
	if len(packagesToExport) != 1 {
		return nil
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

func (gc *GcExporter) exportPackage(ctx context.Context, pkgToExport *types.Package, exportDataDir, moduleName, packagePath string, debugTypeCheck bool) string {
	exportDataFile := packageExportPath(exportDataDir, moduleName, packagePath, pkgToExport.Name())
	gc.createDirs(filepath.Dir(exportDataFile))

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
	_, done := span(ctx, "gcexport.write", "packagePath", pkgToExport.Path(), "outputPath", exportDataFile)
	err = gcexportdata.Write(file, nil, pkgToExport)
	// gcexportdata.Write does not report how much it wrote, so the size comes from the file itself.
	done("outputBytes", fileSizeIfTracing(file))
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error writing gcexportdata: %s\n", err)
		panic("Error writing gcexportdata")
	}
	return exportDataFile
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

// getPackageFromFile reads gc export data of a single package from file.
//
// The imports map acts as a shared cache of already-resolved packages: gcexportdata.Read
// reuses any package it finds there. Callers that resolve several
// packages belonging to the same type-check session MUST pass the same map across all calls,
// otherwise the same import path ends up represented by multiple distinct *types.Package
// objects. Writing such a package then produces an export data manifest with duplicate
// PkgPaths, which makes a later read fail.
func (gc *GcExporter) getPackageFromFile(file fs.File, path string, imports map[string]*types.Package) *types.Package {
	defer func(file fs.File) {
		_ = file.Close()
	}(file)
	if imports == nil {
		imports = make(map[string]*types.Package)
	}
	pkg, err := gcexportdata.Read(file, nil, imports, path)
	if err != nil {
		msg := err.Error()
		if strings.HasPrefix(msg, "internal error while importing") && strings.HasSuffix(msg, "(runtime error: invalid memory address or nil pointer dereference); please report an issue") {
			gc.packagesImportIssueInvalidMemoryAddress = append(gc.packagesImportIssueInvalidMemoryAddress, path)
		} else {
			fmt.Fprintf(os.Stderr, "Error while reading gcexportdata of %s error: %s\n", path, err)
		}
		return getEmptyPackage(path)
	}

	return pkg
}

// Non-root packages use their import directory for both the directory and filename.
// Root packages matching the module suffix share its import directory.
// Other root packages use a distinct filename to avoid collisions with subdirectories.
func packageExportPath(exportDataDir, moduleName, packagePath, packageName string) string {
	if packagePath == "" || packagePath == "." {
		if packageName != "" && filepath.Base(moduleName) == packageName {
			return filepath.Join(exportDataDir, moduleName, packageName+".o")
		}
		return filepath.Join(exportDataDir, moduleName, ".root.o")
	}
	return filepath.Join(exportDataDir, moduleName, packagePath, filepath.Base(packagePath)+".o")
}
