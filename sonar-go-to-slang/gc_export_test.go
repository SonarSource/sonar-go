package main

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"go/ast"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func Test_exportGcExportData(t *testing.T) {
	tests := []struct {
		name string
	}{
		{
			"e1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			for _, file := range getAllGoFiles("resources/cross-file/" + tt.name) {
				exportGcData(file, tt)
			}

			for _, file := range getGoFilesIgnoreSubDirs("resources/cross-file/" + tt.name) {
				actual := parseFileToJson(file, tt)
				jsonFile := strings.Replace(file, ".source", ".json", 1)

				expected, err := os.ReadFile(jsonFile)
				if err != nil {
					t.Fatalf("failed to read file %s: %v", jsonFile, err)
				}
				assert.Equal(t, string(expected), actual)
			}
		})
	}
}

// Update all .json files in resources/cross-file from all .go.source files (ignoring sub directories)
// Add "Test_" before to run in IDE
func fixExportGcExportData(t *testing.T) {
	tests := []struct {
		name string
	}{
		{
			"e1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			for _, file := range getAllGoFiles("resources/cross-file/" + tt.name) {
				exportGcData(file, tt)
			}

			for _, file := range getGoFilesIgnoreSubDirs("resources/cross-file/" + tt.name) {
				actual := parseFileToJson(file, tt)
				jsonFile := strings.Replace(file, ".source", ".json", 1)
				err := os.WriteFile(jsonFile, []byte(actual), 7444)
				if err != nil {
					t.Fatalf("failed to write file %s: %v", jsonFile, err)
				}
			}
		})
	}
}

func exportGcData(file string, tt struct{ name string }) {
	fmt.Printf("Exporting GC data for: %s\n", file)
	fileSet := token.NewFileSet()
	astFile, _, _ := readAstFile(fileSet, file)
	info, _ := typeCheckAst("-", fileSet, astFile, true, "build/cross-file-tests/"+tt.name)
	fileNoPrefix := strings.Replace(file, "resources/cross-file/"+tt.name+"/", "", 1)
	fileExt := strings.Replace(fileNoPrefix, ".go.source", ".o", 1)
	exportGcExportData(info, "build/cross-file-tests/"+tt.name+"/"+fileExt)
}

func parseFileToJson(file string, tt struct{ name string }) string {
	fmt.Printf("Parsing %s\n", file)
	fileSet := token.NewFileSet()
	astFile, fileContent, _ := readAstFile(fileSet, file)
	//TODO SONARGO-576 Go executable should read all gcexportdata of user's code, the foo should be removed here
	info, _ := typeCheckAst("-", fileSet, astFile, true, "build/cross-file-tests/"+tt.name+"/foo")
	slangTree, comments, tokens := toSlangTree(fileSet, astFile, fileContent, info)
	actual := toJsonSlang(slangTree, comments, tokens, "  ")
	return actual
}

func getGoFilesIgnoreSubDirs(folder string) []string {
	var files []string

	err := filepath.Walk(folder, func(path string, info os.FileInfo, err error) error {
		lastPathSep := strings.LastIndex(path, string(os.PathSeparator))
		if strings.HasSuffix(path, ".go.source") && lastPathSep == len(folder) {
			files = append(files, path)
		}
		return nil
	})
	if err != nil {
		panic(err)
	}
	return files
}

func Test_should_return_early_when_pkg_is_nil(t *testing.T) {
	info := types.Info{
		Defs: make(map[*ast.Ident]types.Object),
	}

	exportGcExportData(&info, "export-pkg-is-nil.o")

	_, err := os.ReadFile("export-pkg-is-nil.o")
	if err == nil {
		assert.Fail(t, "The file should NOT have been created")
	}
}

func Test_should_not_fail_when_path_does_not_contain_path_separator(t *testing.T) {
	ident := &ast.Ident{
		Name: "foo",
	}
	pkg := types.NewPackage("foo", "foo")
	obj := types.NewPkgName(0, pkg, "foo", nil)
	defs := map[*ast.Ident]types.Object{
		ident: obj,
	}
	info := types.Info{
		Defs: defs,
	}

	exportGcExportData(&info, "path_does_not_contain_path_separator.o")

	defer func() {
		os.Remove("path_does_not_contain_path_separator.o")
	}()
	_, err := os.ReadFile("path_does_not_contain_path_separator.o")
	if err != nil {
		assert.Fail(t, "The file should have been created", err)
	}
}
