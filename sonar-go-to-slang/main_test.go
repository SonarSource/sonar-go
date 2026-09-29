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
	"bytes"
	"context"
	"encoding/binary"
	"io"
	"os"
	"testing"
	"testing/iotest"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMain(m *testing.M) {
	// Remove files produced by tests before execute all the tests
	os.RemoveAll("build/main_test/out.o")
	m.Run()
}

func TestMainRejectsArguments(t *testing.T) {
	oldArgs, oldExit := os.Args, exit
	defer func() {
		os.Args, exit = oldArgs, oldExit
	}()
	os.Args = []string{"cmd", "-d"}
	exitCode := -1
	exit = func(code int) {
		exitCode = code
	}

	errFile, previousStderr, errChan := captureStandardError()
	main()
	stderr := getStandardError(errFile, previousStderr, errChan)

	assert.Equal(t, 2, exitCode)
	assert.Contains(t, stderr, "Usage: cmd\n")
	assert.Contains(t, stderr, "serves the requests read from stdin")
	assert.Contains(t, stderr, "-d\tdump ast (instead of JSON)")
	assert.Contains(t, stderr, "\tprint errors logs from type checking")
	assert.Contains(t, stderr, "\tdump GC export data")
	assert.Contains(t, stderr, "-gc_export_data_dir string")
	assert.Contains(t, stderr, "\tdirectory where GC export data is located")
	assert.Contains(t, stderr, "-module_name string")
	assert.Contains(t, stderr, "\tspecify module name (defined in go.mod)")
	assert.Contains(t, stderr, "\tspecify package path (e.g. foo/bar for files located in ${projectDir}/foo/bar)")
	assert.NotContains(t, stderr, "Starting in server mode")
}

func TestMainServesStdin(t *testing.T) {
	oldArgs, oldStdin, previousStdout := os.Args, os.Stdin, os.Stdout
	defer func() {
		os.Args, os.Stdin, os.Stdout = oldArgs, oldStdin, previousStdout
	}()
	os.Args = []string{"cmd"}
	stdinReader, stdinWriter, err := os.Pipe()
	require.NoError(t, err)
	_, err = stdinWriter.Write(encodeRequest(nil, map[string]string{"main.go": simpleMain}))
	require.NoError(t, err)
	require.NoError(t, stdinWriter.Close())
	os.Stdin = stdinReader

	outFile, _, outChan := captureStandardOutput()
	main()
	stdout := getStandardOutput(outFile, previousStdout, outChan)

	responses := decodeResponses(t, []byte(stdout))
	require.Len(t, responses, 1)
	assert.Equal(t, statusOK, responses[0].status)
	assert.Contains(t, responses[0].body, "\"main.go\"")
}

func TestAnalyzeWithDumpAstFlag(t *testing.T) {
	output, stderr := analyzeFiles(t, []string{"-d"}, "resources/simple_file.go.source")

	assert.Contains(t, output, "Package: token.Pos(1)")
	assert.Contains(t, stderr, "Received parameters: dumpAst=true, debugTypeCheck=false, dumpGcExportData=false, gcExportDataDir=\"\", moduleName=\"\", moduleBaseDir=\".\", packagePath=\"\"")
}

func TestAnalyzeSimpleFile(t *testing.T) {
	output, stderr := analyzeFiles(t, nil, "resources/simple_file.go.source")

	assert.Contains(t, output, "\"@type\": \"PackageDeclaration\", \"metaData\": \"1:0::17\"")
	assert.Contains(t, stderr, "Received parameters: dumpAst=false, debugTypeCheck=false, dumpGcExportData=false, gcExportDataDir=\"\", moduleName=\"\", moduleBaseDir=\".\", packagePath=\"\"\n")
}

func TestAnalyzeWithPackageResolution(t *testing.T) {
	output, _ := analyzeFiles(t, nil, "resources/simple_file_with_packages.go.source")

	assert.Contains(t, output, "\"type\":\"github.com/beego/beego/v2/server/web/session.Store\"")
}

func TestAnalyzeFillsIdentifierWithInfo(t *testing.T) {
	output, _ := analyzeFiles(t, nil, "resources/simple_file_with_static_packages.go.source")

	assert.Contains(t, output, "\"id\":66")
	assert.Contains(t, output, "\"type\":\"*database/sql.DB\"")
	assert.Contains(t, output, "\"package\":\"database/sql\"")
}

func TestAnalyzeWithDotImport(t *testing.T) {
	output, _ := analyzeFiles(t, nil, "resources/simple_file_with_dot_import.go.source")

	assert.Contains(t, output, "\"package\":\"math/rand\",\"name\":\"Intn\"")
}

func TestAnalyzeInvalidFile(t *testing.T) {
	output, stderr := analyzeFiles(t, nil, "resources/invalid_file.go.source")

	assert.Equal(t, `{
  "resources/invalid_file.go.source": { 
"treeMetaData": {
"comments": [
],
"tokens": [
]
},
"tree":
null,
"error": "resources/invalid_file.go.source:1:1: expected 'package', found xpackage"
} 

}
`, output)
	assert.Equal(t, "Received parameters: dumpAst=false, debugTypeCheck=false, dumpGcExportData=false, gcExportDataDir=\"\", moduleName=\"\", moduleBaseDir=\".\", packagePath=\"\"\n", stderr)
}

func TestAnalyzeShouldExportGcData(t *testing.T) {
	output, _ := analyzeFiles(t, []string{"-dump_gc_export_data", "-gc_export_data_dir", "build/main_test/"}, "resources/simple_file_with_packages.go.source")

	assert.Empty(t, output)
	assert.FileExists(t, "build/main_test/main/main.o", "File should exist")
}

func TestShouldNotPanicWhenGcExportOneInvalidFile(t *testing.T) {
	output, stderr := analyzeFiles(t, []string{"-dump_gc_export_data", "-gc_export_data_dir", "build/main_test/"},
		"resources/invalid_file.go.source", "resources/simple_file_with_packages.go.source")

	assert.Empty(t, output)
	assert.Contains(t, stderr, "Received parameters: dumpAst=false, debugTypeCheck=false, dumpGcExportData=true, gcExportDataDir=\"build/main_test/\", moduleName=\"\", moduleBaseDir=\".\", packagePath=\"\"")
}

func TestShouldNotPanicWhenGenerateASTOneInvalidFile(t *testing.T) {
	output, _ := analyzeFiles(t, []string{"-gc_export_data_dir", "build/main_test/"},
		"resources/invalid_file.go.source", "resources/simple_file_with_packages.go.source")

	assert.Contains(t, output, "\"tree\":\nnull,\n\"error\": \"resources/invalid_file.go.source:1:1: expected 'package', found xpackage\"")
	assert.Contains(t, output, "\"__cfgId\":2},\n\"error\": null")
}

func TestAnalyzePanicsWhenInputCannotBeRead(t *testing.T) {
	errFile, previousStderr, errChan := captureStandardError()
	defer func() {
		stderr := getStandardError(errFile, previousStderr, errChan)
		assert.NotNil(t, recover(), "analyze must panic when its input cannot be read")
		assert.Contains(t, stderr, "Error reading AST file:")
	}()
	analyze(context.Background(), Params{}, iotest.ErrReader(io.ErrUnexpectedEOF), io.Discard, nil, &analysisStats{})
}

func TestLaneLabel(t *testing.T) {
	files := map[string]string{"plumbing/format/idxfile/decoder.go": "", "plumbing/format/idxfile/encoder.go": ""}

	assert.Equal(t, "given/path", laneLabel(Params{packagePath: "given/path", moduleName: "mod"}, files))
	// The parse pass gets no package path, so the shared directory of the batch has to name the lane.
	assert.Equal(t, "plumbing/format/idxfile", laneLabel(Params{moduleName: "mod"}, files))
	assert.Equal(t, "mod", laneLabel(Params{moduleName: "mod"}, map[string]string{"main.go": ""}))
	assert.Equal(t, "mod", laneLabel(Params{moduleName: "mod"}, nil))
	// Stable across runs despite Go's randomised map iteration order.
	spanning := map[string]string{"b/x.go": "", "a/y.go": ""}
	for range 20 {
		assert.Equal(t, "a", laneLabel(Params{moduleName: "mod"}, spanning))
	}
}

// analyzeFiles runs one request with the given options on the given files, and returns what it wrote as its output
// and to stderr.
func analyzeFiles(t *testing.T, args []string, filePaths ...string) (output string, stderr string) {
	t.Helper()
	var payload bytes.Buffer
	for _, filePath := range filePaths {
		payload.Write(readFileToByteSlice(filePath))
	}
	var out bytes.Buffer
	errFile, previousStderr, errChan := captureStandardError()
	func() {
		defer func() {
			stderr = getStandardError(errFile, previousStderr, errChan)
		}()
		params, err := parseRequestArgs(args)
		require.NoError(t, err)
		analyze(context.Background(), params, &payload, &out, nil, &analysisStats{})
	}()
	return out.String(), stderr
}

var stdoutFile *os.File
var oldStdout *os.File
var stdoutChan chan string
var stderrFile *os.File
var oldStderr *os.File
var stderrChan chan string

func getStdOutAndStdErr() (stdoutText, stderrText string) {
	stdoutText = getStandardOutput(stdoutFile, oldStdout, stdoutChan)
	stderrText = getStandardError(stderrFile, oldStderr, stderrChan)
	return
}

func captureStdOutAndStdErr() {
	stdoutFile, oldStdout, stdoutChan = captureStandardOutput()
	stderrFile, oldStderr, stderrChan = captureStandardError()
}

func getStandardOutput(w *os.File, old *os.File, outC chan string) string {
	// Restore the original stdout
	w.Close()
	os.Stdout = old
	output := <-outC
	return output
}

func getStandardError(w *os.File, old *os.File, outC chan string) string {
	// Restore the original stderr
	w.Close()
	os.Stderr = old
	output := <-outC
	return output
}

// writeOut, oldStdOut, outChanel
func captureStandardOutput() (*os.File, *os.File, chan string) {
	// Capture the standard output
	r, w, _ := os.Pipe()
	old := os.Stdout
	os.Stdout = w

	// Capture the output in a separate goroutine
	outC := make(chan string)
	go func() {
		var buf bytes.Buffer
		io.Copy(&buf, r)
		outC <- buf.String()
	}()
	return w, old, outC
}

func captureStandardError() (*os.File, *os.File, chan string) {
	// Create a pipe to capture the standard error output
	r, w, _ := os.Pipe()
	old := os.Stderr
	os.Stderr = w

	// Capture the output in a separate goroutine
	outC := make(chan string)
	go func() {
		var buf bytes.Buffer
		io.Copy(&buf, r)
		outC <- buf.String()
	}()
	return w, old, outC
}

func readFileToByteSlice(filePath string) []byte {
	fileContent, err := os.ReadFile(filePath)
	if err != nil {
		panic(err)
	}

	byteData := new(bytes.Buffer)
	writeBytes(byteData, int32(len(filePath)))
	writeBytes(byteData, []byte(filePath))

	writeBytes(byteData, int32(len(fileContent)))
	writeBytes(byteData, fileContent)

	return byteData.Bytes()
}

func writeBytes(byteData *bytes.Buffer, data any) {
	err := binary.Write(byteData, binary.LittleEndian, data)
	if err != nil {
		panic(err)
	}
}
