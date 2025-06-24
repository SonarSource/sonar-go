package main

import (
	"bytes"
	"go/ast"
	"log"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestImportExistingPackage_returnsPackage(t *testing.T) {
	importer := &localImporter{}
	pkg, err := importer.Import("net/http")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func TestImportNonSupportedPackage_returnsEmptyPackage(t *testing.T) {
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

func TestGetPackageFromExportData_validFile_returnsPackage(t *testing.T) {
	pkg, err := getPackageFromExportData(PackageExportDataDir+"/"+"net_http.o", "net/http")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "net/http", pkg.Path())
	assert.Greater(t, pkg.Scope().Len(), 0)
}

func TestGetPackageFromExportData_invalidFile_returnsEmptyPackage(t *testing.T) {
	pkg, err := getPackageFromExportData("invalid_file.o", "invalid/path")
	assert.NoError(t, err)
	assert.NotNil(t, pkg)
	assert.Equal(t, "invalid/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func TestGetEmptyPackage_returnsEmptyPackage(t *testing.T) {
	pkg := getEmptyPackage("empty/path")
	assert.NotNil(t, pkg)
	assert.Equal(t, "empty/path", pkg.Path())
	assert.Equal(t, 0, pkg.Scope().Len())
}

func TestTypeCheckAst(t *testing.T) {
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

func TestTestOnlyFirstErrorIsReturned(t *testing.T) {
	source, err := os.ReadFile("resources/file_with_many_errors.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	fileSet, astFiles := astFromString("file_with_many_errors.go", string(source))

	info, errors := typeCheckAst(fileSet, astFiles, false, "", "")
	assert.Len(t, errors, 1)
	assert.Equal(t, "file_with_many_errors.go:4:5: declared and not used: a1", errors[0].Error())
	assert.NotNil(t, info)
}

func TestShouldReturnErrorForAllFailingFilesPerPackage(t *testing.T) {
	source1, err := os.ReadFile("resources/file_with_many_errors.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}
	source2, err := os.ReadFile("resources/file_with_many_errors_2.go.source")
	if err != nil {
		t.Fatalf("Failed to read source file: %v", err)
	}

	filenameToContent := map[string]string{
		"file_with_many_errors_1.go": string(source1),
		"file_with_many_errors_2.go": string(source2),
	}
	fileSet, astFiles := astFromStrings(filenameToContent)

	info, errors := typeCheckAst(fileSet, astFiles, false, "", "")
	assert.ElementsMatch(t, []string{
		"file_with_many_errors_1.go:4:5: declared and not used: a1",
		"file_with_many_errors_2.go:4:5: declared and not used: a1",
	}, []string{errors[0].Error(), errors[1].Error()})
	assert.NotNil(t, info)
}

func TestShouldGroupFilesPerPackageNameTogether(t *testing.T) {
	var file1AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package1"}}, err: nil}
	var file2AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package2"}}, err: nil}
	var file3AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package2"}}, err: nil}

	var astFiles = map[string]AstFileOrError{
		"file1.go": file1AstOrError,
		"file2.go": file2AstOrError,
		"file3.go": file3AstOrError,
	}
	var filesPerPackageName = groupFilesPerPackageName(astFiles)
	assert.Len(t, filesPerPackageName, 2)
	assert.Contains(t, filesPerPackageName, "package1")
	assert.Contains(t, filesPerPackageName, "package2")

	var package1Files = filesPerPackageName["package1"]
	assert.Len(t, package1Files, 1)
	assert.Contains(t, package1Files, "file1.go")
	assert.Equal(t, file1AstOrError, package1Files["file1.go"])

	var package2Files = filesPerPackageName["package2"]
	assert.Len(t, package2Files, 2)
	assert.Contains(t, package2Files, "file2.go")
	assert.Equal(t, file2AstOrError, package2Files["file2.go"])
	assert.Contains(t, package2Files, "file3.go")
	assert.Equal(t, file3AstOrError, package2Files["file3.go"])
}

func TestShouldIgnoreFileWithoutAst(t *testing.T) {
	var file1AstOrError = AstFileOrError{ast: &ast.File{Name: &ast.Ident{Name: "package1"}}, err: nil}
	var file2AstOrError = AstFileOrError{ast: nil, err: nil}

	var astFiles = map[string]AstFileOrError{
		"file1.go": file1AstOrError,
		"file2.go": file2AstOrError,
	}
	var filesPerPackageName = groupFilesPerPackageName(astFiles)
	assert.Len(t, filesPerPackageName, 1)
	assert.Contains(t, filesPerPackageName, "package1")

	var package1Files = filesPerPackageName["package1"]
	assert.Len(t, package1Files, 1)
	assert.Contains(t, package1Files, "file1.go")
	assert.Equal(t, file1AstOrError, package1Files["file1.go"])
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
