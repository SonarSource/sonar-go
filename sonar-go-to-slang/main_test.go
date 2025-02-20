package main

import (
	"bytes"
	"flag"
	"github.com/stretchr/testify/assert"
	"io"
	"os"
	"testing"
)

func TestParseNoArguments(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}
	params := parseArgs()
	assert.False(t, params.dumpAst, "Expected dumpAst to be false when no arguments are provided")
	assert.Empty(t, params.path, "Expected path to be empty when no arguments are provided")
}

func TestParseInvalidArguments(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-undefined"}

	writeOut, oldStdOut, outChanel := captureStandardError()

	defer func() {
		output := getStandardError(writeOut, oldStdOut, outChanel)
		if r := recover(); r != nil {
			assert.Contains(t, output, "flag provided but not defined: -undefined")
			assert.Contains(t, output, "Usage of cmd:")
			assert.Contains(t, output, "-d\tdump ast (instead of JSON)")
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
	assert.Empty(t, params.path, "Expected path to be empty when only -d flag is provided")
}

func TestParseArgsWithFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()

	os.Args = []string{"cmd", "source.go"}
	params := parseArgs()
	assert.False(t, params.dumpAst, "Expected dumpAst to be false when only file path is provided")
	assert.Equal(t, "source.go", params.path, "Expected path to be 'source.go' when file path is provided")
}

func TestParseArgsWithDumpAstFlagAndFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()

	os.Args = []string{"cmd", "-d", "source.go"}
	params := parseArgs()
	assert.True(t, params.dumpAst, "Expected dumpAst to be true when -d flag and file path are provided")
	assert.Equal(t, "source.go", params.path, "Expected path to be 'source.go' when -d flag and file path are provided")
}

func TestMainWithDumpAstFlag(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-d", "resources/simple_file.go.source"}

	writeOut, oldStdOut, outChanel := captureStandardOutput()

	main()

	output := getStandardOutput(writeOut, oldStdOut, outChanel)

	// Validate the output
	assert.Contains(t, output, "Package: token.Pos(1)")
}

func TestMainWithFilePath(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "resources/simple_file.go.source"}

	writeOut, oldStdOut, outChanel := captureStandardOutput()

	main()

	output := getStandardOutput(writeOut, oldStdOut, outChanel)

	// Validate the output
	assert.Contains(t, output, "\"@type\": \"PackageDeclaration\", \"metaData\": \"1:0::17\"")
}

func TestMainWithInvalidFile(t *testing.T) {
	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "resources/invalid_file.go.source"}

	writeOut, oldStdOut, outChanel := captureStandardOutput()

	defer func() {
		output := getStandardOutput(writeOut, oldStdOut, outChanel)
		if r := recover(); r != nil {
			assert.Empty(t, output, "Expected empty standard output")
		}
	}()
	main()
	assert.Fail(t, "The main() should throw panic for invalid file")
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
