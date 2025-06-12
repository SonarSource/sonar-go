package main

import (
	"bytes"
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
	for _, name := range getSubDirs("resources/cross-file") {
		t.Run("Test_exportGcExportData_"+name, func(t *testing.T) {
			projectDir := "resources/cross-file/" + name
			filesByDir := getAllGoFilesByDirs(projectDir)
			for dir, files := range filesByDir {
				exportDir := strings.TrimPrefix(dir, "resources/cross-file/")
				exportGcData(files, exportDir)
			}

			files := getGoFilesIgnoreSubDirs(projectDir)

			filesToJson := parseFileToJson(files, name)

			for file, json := range filesToJson {
				expected, err := os.ReadFile(file)
				if err != nil {
					t.Fatalf("failed to read file %s: %v", file, err)
				}
				assert.Equal(t, string(expected), json)
			}
		})
	}
}

// Update all .json files in resources/cross-file from all .go.source files (ignoring sub directories)
// Add "Test_" before to run in IDE
func fixExportGcExportData(t *testing.T) {
	for _, name := range getSubDirs("resources/cross-file") {
		t.Run("fixExportGcExportData_"+name, func(t *testing.T) {
			projectDir := "resources/cross-file/" + name
			filesByDir := getAllGoFilesByDirs(projectDir)
			for dir, files := range filesByDir {
				exportDir := strings.TrimPrefix(dir, "resources/cross-file/")
				exportGcData(files, exportDir)
			}

			filesIgnoreSubDirs := getGoFilesIgnoreSubDirs(projectDir)
			parseFileToJsonAndSave(filesIgnoreSubDirs, name)
		})
	}
	t.Fatal("This test is only for local development and should not run in CI build.")
}

func exportGcData(files []string, name string) {
	fmt.Printf("Exporting GC data for: %s\n", files)
	fileSet := token.NewFileSet()

	astFiles, _, _ := readAstFile(fileSet, readFilesToReader(files))
	info, _ := typeCheckAst(fileSet, astFiles, true, "build/cross-file-tests/"+name)
	exportGcExportData(info, "build/cross-file-tests/"+name)
}

func readFilesToReader(files []string) *bytes.Reader {
	var slice []byte
	for _, file := range files {
		slice = append(slice, readFileToByteSlice(file)...)
	}
	return bytes.NewReader(slice)
}

func parseFileToJsonAndSave(files []string, name string) {
	fileSet := token.NewFileSet()
	astFiles, fileContents, _ := readAstFile(fileSet, readFilesToReader(files))

	info, _ := typeCheckAst(fileSet, astFiles, true, "build/cross-file-tests/"+name)

	for fileName, aFile := range astFiles {
		slangTree, comments, tokens := toSlangTree(fileSet, &aFile, fileContents[fileName], info)
		actual := toJsonSlang(slangTree, comments, tokens, "  ")

		jsonFile := strings.Replace(fileName, ".source", ".json", 1)
		fmt.Printf("Writing %s\n", jsonFile)
		err := os.WriteFile(jsonFile, []byte(actual), 0644)
		if err != nil {
			panic(fmt.Sprintf("failed to write file %s: %v", jsonFile, err))
		}
	}
}

func parseFileToJson(files []string, name string) map[string]string {
	fileSet := token.NewFileSet()
	astFiles, fileContents, _ := readAstFile(fileSet, readFilesToReader(files))

	info, _ := typeCheckAst(fileSet, astFiles, true, "build/cross-file-tests/"+name)

	result := map[string]string{}

	for fileName, aFile := range astFiles {
		slangTree, comments, tokens := toSlangTree(fileSet, &aFile, fileContents[fileName], info)
		actual := toJsonSlang(slangTree, comments, tokens, "  ")
		jsonFile := strings.Replace(fileName, ".source", ".json", 1)
		result[jsonFile] = actual
	}
	return result
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

func getSubDirs(folder string) []string {
	var files []string

	err := filepath.Walk(folder, func(path string, info os.FileInfo, err error) error {
		lastPathSep := strings.LastIndex(path, string(os.PathSeparator))
		if info.IsDir() && lastPathSep == len(folder) {
			files = append(files, info.Name())
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

func Test_should_not_fail_when_empty_path(t *testing.T) {
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

	exportGcExportData(&info, "")

	defer func() {
		os.Remove("foo.o")
	}()
	_, err := os.ReadFile("foo.o")
	if err != nil {
		assert.Fail(t, "The file should have been created", err)
	}
}

func getAllGoFilesByDirs(folder string) map[string][]string {
	files := map[string][]string{}

	err := filepath.Walk(folder, func(path string, info os.FileInfo, err error) error {
		if strings.HasSuffix(path, ".go.source") {
			dir := path[0:strings.LastIndex(path, string(os.PathSeparator))]
			files[dir] = append(files[dir], path)
		}
		return nil
	})
	if err != nil {
		panic(err)
	}
	return files
}
