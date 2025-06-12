package main

import (
	"bytes"
	"encoding/binary"
	"flag"
	"github.com/stretchr/testify/assert"
	"io"
	"os"
	"testing"
)

var stdoutFile *os.File
var oldStdout *os.File
var stdoutChan chan string
var stderrFile *os.File
var oldStderr *os.File
var stderrChan chan string

func TestMain(m *testing.M) {
	// Remove files produced by tests before execute all the tests
	os.RemoveAll("build/main_test/out.o")
	m.Run()
}

func TestParseNoArguments(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}
	params := parseArgs()
	assert.False(t, params.dumpAst, "Expected dumpAst to be false when no arguments are provided")
	assert.False(t, params.debugTypeCheck, "Expected debugTypeCheck to be false when no arguments are provided")
}

func TestParseInvalidArguments(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-undefined"}

	captureStdOutAndStdErr()

	defer func() {
		stdout, stderr := getStdOutAndStdErr()
		if r := recover(); r != nil {
			assert.Empty(t, stdout, "Expected empty standard output")
			assert.Contains(t, stderr, "flag provided but not defined: -undefined")
			assert.Contains(t, stderr, "Usage of cmd:")
			assert.Contains(t, stderr, "-d\tdump ast (instead of JSON)")
			assert.Contains(t, stderr, "-debug_type_check")
			assert.Contains(t, stderr, "print errors logs from type checking")
		}
	}()
	parseArgs()
	assert.Fail(t, "The parseArgs() should throw panic for undefined arguments")
}

func TestParseArgsWithDumpAstFlag(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-d"}
	params := parseArgs()
	assert.True(t, params.dumpAst, "Expected dumpAst to be true when -d flag is provided")
}

func TestParseArgsWithFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "source.go"}
	params := parseArgs()
	assert.False(t, params.dumpAst, "Expected dumpAst to be false when only file path is provided")
}

func TestParseArgsWithDumpAstFlagAndFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-d", "source.go"}
	params := parseArgs()
	assert.True(t, params.dumpAst, "Expected dumpAst to be true when -d flag and file path are provided")
}

func TestMainWithDumpAstFlag(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-d"}
	stdout, stderr := callMainStdinFromFile("resources/simple_file.go.source")

	assert.Contains(t, stdout, "Package: token.Pos(1)")
	assert.Empty(t, stderr, "Expected empty stderr")
}

func TestMainWithFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}
	stdout, stderr := callMainStdinFromFile("resources/simple_file.go.source")

	assert.Contains(t, stdout, "\"@type\": \"PackageDeclaration\", \"metaData\": \"1:0::17\"")
	assert.Empty(t, stderr, "Expected empty stderr")
}

func TestMainWithPackageResolution(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}
	stdout, stderr := callMainStdinFromFile("resources/simple_file_with_packages.go.source")

	assert.Contains(t, stdout, "\"type\":\"github.com/beego/beego/v2/server/web/session.Store\"")
	assert.Empty(t, stderr, "Expected empty stderr")
}

func TestMainFillIdentifierWithInfo(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}

	stdout, stderr := callMainStdinFromFile("resources/simple_file_with_static_packages.go.source")

	assert.Contains(t, stdout, "\"id\":66")
	assert.Contains(t, stdout, "\"type\":\"*database/sql.DB\"")
	assert.Contains(t, stdout, "\"package\":\"database/sql\"")
	assert.Empty(t, stderr, "Expected empty stderr")
}

func TestMainWithDotImport(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}

	stdout, stderr := callMainStdinFromFile("resources/simple_file_with_dot_import.go.source")

	assert.Contains(t, stdout, "\"package\":\"math/rand\",\"name\":\"Intn\"")
	assert.Empty(t, stderr, "Expected empty stderr")
}

func TestMainWithInvalidFile(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}

	defer func() {
		stdout, stderr := getStdOutAndStdErr()
		if r := recover(); r != nil {
			assert.Empty(t, stdout, "Expected empty standard output")
			assert.Contains(t, stderr, "Error reading AST file: resources/invalid_file.go.source:1:1: expected 'package', found xpackage")
		}
	}()

	callMainStdinFromFile("resources/invalid_file.go.source")

	assert.Fail(t, "The main() should throw panic for invalid file")
}

func TestMainWithDumpGcExportDataFlagOnly(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-dump_gc_export_data", "resources/simple_file.go.source"}

	defer func() {
		stdout, _ := getStdOutAndStdErr()
		if r := recover(); r != nil {
			assert.Empty(t, stdout, "Expected empty standard output")
		}
	}()
	callMain()
	assert.Fail(t, "The main() should throw panic for missing gc_export_data_dir")
}

func TestMainShouldExportGcData(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-dump_gc_export_data", "-gc_export_data_dir", "build/main_test/"}
	callMainStdinFromFile("resources/simple_file_with_packages.go.source")
	assert.FileExists(t, "build/main_test/main.o", "File should exist")
}

func TestPrintUsageForInvalidArguments(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-invalid-flag"}

	defer func() {
		stdout, stderr := getStdOutAndStdErr()
		if r := recover(); r != nil {
			assert.Empty(t, stdout, "Expected empty standard output")
			assert.Contains(t, stderr, "flag provided but not defined: -invalid-flag", "Expected in standard output")
			assert.Contains(t, stderr, "-d\tdump ast (instead of JSON)", "Expected in standard output")
			assert.Contains(t, stderr, "debug_type_check", "Expected in standard output")
			assert.Contains(t, stderr, "\tprint errors logs from type checking", "Expected in standard output")
			assert.Contains(t, stderr, "-dump_gc_export_data", "Expected in standard output")
			assert.Contains(t, stderr, "\tdump GC export data", "Expected in standard output")
			assert.Contains(t, stderr, "-gc_export_data_dir string", "Expected in standard output")
			assert.Contains(t, stderr, "\tdirectory where GC export data is located", "Expected in standard output")
		}
	}()
	callMain()
	assert.Fail(t, "The main() should throw panic for invalid flag")
}

func getStandardOutput(w *os.File, old *os.File, outC chan string) string {
	// Restore the original stdout
	w.Close()
	os.Stdout = old
	output := <-outC
	return output
}

func getStandardError(w *os.File, old *os.File, outC chan string) string {
	// Restore the original stdout
	w.Close()
	os.Stderr = old
	output := <-outC
	return output
}

func getStdOutAndStdErr() (stdoutText, stderrText string) {
	stdoutText = getStandardOutput(stdoutFile, oldStdout, stdoutChan)
	stderrText = getStandardError(stderrFile, oldStderr, stderrChan)
	return
}

func captureStdOutAndStdErr() {
	stdoutFile, oldStdout, stdoutChan = captureStandardOutput()
	stderrFile, oldStderr, stderrChan = captureStandardError()
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

func resetCommandLineFlagsToDefault() {
	flag.CommandLine = flag.NewFlagSet(os.Args[0], flag.PanicOnError)
}

func callMainStdinFromFile(file string) (stdout string, stderr string) {
	captureStdOutAndStdErr()
	oldStdin := setStdIn(file)
	main()
	os.Stdin = oldStdin
	return getStdOutAndStdErr()
}

func callMain() (stdout string, stderr string) {
	captureStdOutAndStdErr()
	main()
	return getStdOutAndStdErr()
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

func setStdIn(filePath string) *os.File {

	r, w, err := os.Pipe()

	go func() {
		_, err = w.Write(readFileToByteSlice(filePath))
		if err != nil {
			panic(err)
		}
		w.Close()
	}()

	// Store the original standard input
	old := os.Stdin

	// Set the standard input to the file
	os.Stdin = r

	return old
}
