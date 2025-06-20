package main

import (
	"bytes"
	"log"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_importExistingPackage_returnsPackage(t *testing.T) {
	importer := &localImporter{}
	pkg, err := importer.Import("net/http")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func Test_importNonSupportedPackage_returnsEmptyPackage(t *testing.T) {
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

func Test_getPackageFromExportData_validFile_returnsPackage(t *testing.T) {
	pkg, err := getPackageFromExportData(PackageExportDataDir+"/"+"net_http.o", "net/http")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func Test_getPackageFromExportData_invalidFile_returnsEmptyPackage(t *testing.T) {
	pkg, err := getPackageFromExportData("invalid_file.o", "invalid/path")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "invalid/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func Test_getEmptyPackage_returnsEmptyPackage(t *testing.T) {
	pkg := getEmptyPackage("empty/path")
	assert.NotNil(t, pkg)
	assert.Equal(t, "empty/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func Test_typeCheckAst(t *testing.T) {
	source, err := os.ReadFile("resources/simple_file_with_packages.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	fileSet, astFiles := astFromString("simple_file_with_packages.go", string(source))
	info, _ := typeCheckAst(fileSet, astFiles, true, "", "")

	assert.NotNil(t, info)
	assert.NotEmpty(t, info.Types)
	assert.NotEmpty(t, info.Defs)
	assert.NotEmpty(t, info.Uses)
	assert.NotEmpty(t, info.Implicits)
	assert.NotEmpty(t, info.Selections)
	assert.NotEmpty(t, info.Scopes)
}

func Test_testOnlyFirstErrorIsReturned(t *testing.T) {
	source, err := os.ReadFile("resources/file_with_many_errors.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	fileSet, astFiles := astFromString("file_with_many_errors.go", string(source))

	info, err := typeCheckAst(fileSet, astFiles, false, "", "")
	assert.Equal(t, "file_with_many_errors.go:4:5: declared and not used: a1", err.Error())
	assert.NotNil(t, info)
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
