package main

import (
	"bytes"
	"github.com/stretchr/testify/assert"
	"log"
	"os"
	"testing"
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
	pkg, err := getPackageFromExportData("net_http.o", "net/http")
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

	fileSet, astFile := astFromString(string(source))
	info, _ := typeCheckAst("my/path", fileSet, astFile)

	assert.NotNil(t, info)
	assert.NotEmpty(t, info.Types)
	assert.NotEmpty(t, info.Defs)
	assert.NotEmpty(t, info.Uses)
	assert.NotEmpty(t, info.Implicits)
	assert.NotEmpty(t, info.Selections)
	assert.NotEmpty(t, info.Scopes)
}
