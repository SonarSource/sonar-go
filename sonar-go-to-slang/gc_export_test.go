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
			moduleName := readModuleName(projectDir)
			filesByDir := getAllGoFilesByDirs(projectDir)
			for dir, files := range filesByDir {
				packagePath := strings.Trim(strings.TrimPrefix(dir, projectDir), "/\\")
				exportGcData(files, name, moduleName, packagePath)
			}

			files := getGoFilesIgnoreSubDirs(projectDir)
			filesToJson := parseFileToJson(files, name, moduleName)

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
			moduleName := readModuleName(projectDir)
			filesByDir := getAllGoFilesByDirs(projectDir)
			for dir, files := range filesByDir {
				packagePath := strings.Trim(strings.TrimPrefix(dir, projectDir), "/\\")
				exportGcData(files, name, moduleName, packagePath)
			}

			filesIgnoreSubDirs := getGoFilesIgnoreSubDirs(projectDir)
			parseFileToJsonAndSave(filesIgnoreSubDirs, name, moduleName)
		})
	}
	t.Fatal("This test is only for local development and should not run in CI build.")
}

func readModuleName(projectDir string) string {
	modFile := filepath.Join(projectDir, "go.mod")
	data, err := os.ReadFile(modFile)
	if err != nil {
		return ""
	}
	// git force to have newlines as LF so it is safe for tests to split by \n
	lines := strings.Split(string(data), "\n")
	for _, line := range lines {
		if strings.HasPrefix(line, "module ") {
			return strings.Trim(strings.TrimSpace(strings.TrimPrefix(line, "module ")), "\"")
		}
	}
	return ""
}

func exportGcData(files []string, name string, moduleName string, packagePath string) {
	exportLocation := "build/cross-file-tests/" + name
	fmt.Printf("Exporting GC data for: %s\n", files)
	fileSet := token.NewFileSet()

	astFiles, _, _ := readAstFile(fileSet, readFilesToReader(files))
	info, _ := typeCheckAst(fileSet, astFiles, true, exportLocation, moduleName)
	exportGcExportData(info, exportLocation, moduleName, packagePath, false)
}

func readFilesToReader(files []string) *bytes.Reader {
	var slice []byte
	for _, file := range files {
		slice = append(slice, readFileToByteSlice(file)...)
	}
	return bytes.NewReader(slice)
}

func parseFileToJsonAndSave(files []string, name string, moduleName string) {
	fileSet := token.NewFileSet()
	astFiles, fileContents, _ := readAstFile(fileSet, readFilesToReader(files))

	info, _ := typeCheckAst(fileSet, astFiles, true, "build/cross-file-tests/"+name, moduleName)

	for fileName, aFile := range astFiles {
		slangTree, comments, tokens, errMsg := toSlangTree(fileSet, &aFile, fileContents[fileName], info)
		slangTreeWithPlaceholders := slangTreeWithIdPlaceholders(slangTree)
		actual := toJsonSlang(slangTreeWithPlaceholders, comments, tokens, errMsg, "  ")

		jsonFile := strings.Replace(fileName, ".source", ".json", 1)
		fmt.Printf("Writing %s\n", jsonFile)
		err := os.WriteFile(jsonFile, []byte(actual), 0644)
		if err != nil {
			panic(fmt.Sprintf("failed to write file %s: %v", jsonFile, err))
		}
	}
}

func parseFileToJson(files []string, name string, moduleName string) map[string]string {
	fileSet := token.NewFileSet()
	astFiles, fileContents, _ := readAstFile(fileSet, readFilesToReader(files))

	info, _ := typeCheckAst(fileSet, astFiles, true, "build/cross-file-tests/"+name, moduleName)

	result := map[string]string{}

	for fileName, aFile := range astFiles {
		slangTree, comments, tokens, errMsg := toSlangTree(fileSet, &aFile, fileContents[fileName], info)
		slangTreeWithPlaceholders := slangTreeWithIdPlaceholders(slangTree)
		actual := toJsonSlang(slangTreeWithPlaceholders, comments, tokens, errMsg, "  ")
		jsonFile := strings.Replace(fileName, ".source", ".json", 1)
		result[jsonFile] = actual
	}
	return result
}

// We need to replace all "id" values with placeholders because the actual IDs may change between different runs.
func slangTreeWithIdPlaceholders(slangTree *Node) *Node {
	if slangTree.SlangField != nil {
		if _, ok := slangTree.SlangField["id"]; ok {
			slangTree.SlangField["id"] = "__id_placeholder__"
		}
	}

	for _, child := range slangTree.Children {
		slangTreeWithIdPlaceholders(child)
	}

	return slangTree
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

	exportGcExportData(&info, "export-pkg-is-nil.o", "", "", false)

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

	exportGcExportData(&info, "", "", "", false)

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
			// Windows uses backslashes, so we need to replace them with forward slashes
			dir = strings.ReplaceAll(dir, "\\", "/")
			files[dir] = append(files[dir], path)
		}
		return nil
	})
	if err != nil {
		panic(err)
	}
	return files
}
