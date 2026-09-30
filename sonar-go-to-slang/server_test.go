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
	"bufio"
	"bytes"
	"context"
	"encoding/binary"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type response struct {
	status byte
	body   string
}

func encodeRequest(args []string, files map[string]string) []byte {
	var buf bytes.Buffer
	writeBytes(&buf, int32(len(args)))
	for _, arg := range args {
		writeBytes(&buf, int32(len(arg)))
		buf.WriteString(arg)
	}
	payload := encodeFiles(files)
	writeBytes(&buf, int32(len(payload)))
	buf.Write(payload)
	return buf.Bytes()
}

// encodeFiles builds a payload in the format read by readAstFile.
func encodeFiles(files map[string]string) []byte {
	var buf bytes.Buffer
	for name, content := range files {
		writeBytes(&buf, int32(len(name)))
		buf.WriteString(name)
		writeBytes(&buf, int32(len(content)))
		buf.WriteString(content)
	}
	return buf.Bytes()
}

func decodeResponses(t *testing.T, output []byte) []response {
	t.Helper()
	reader := bytes.NewReader(output)
	var responses []response
	for reader.Len() > 0 {
		status, err := reader.ReadByte()
		require.NoError(t, err)
		var length int32
		require.NoError(t, binary.Read(reader, binary.LittleEndian, &length))
		body := make([]byte, length)
		_, err = io.ReadFull(reader, body)
		require.NoError(t, err)
		responses = append(responses, response{status, string(body)})
	}
	return responses
}

// serveRequests sends all the requests at once, as a client pipelining them would, and returns the responses.
func serveRequests(t *testing.T, s *server, requests ...[]byte) []response {
	t.Helper()
	var output bytes.Buffer
	require.NoError(t, s.serve(bytes.NewReader(bytes.Join(requests, nil)), &output))
	responses := decodeResponses(t, output.Bytes())
	require.Len(t, responses, len(requests), "every request must be answered")
	return responses
}

const simpleMain = "package main\n\nfunc main() {}\n"

func TestServerAnswersSequentialRequests(t *testing.T) {
	responses := serveRequests(t, &server{},
		encodeRequest([]string{"-module_name", "example.com/mod"}, map[string]string{"main.go": simpleMain}),
		encodeRequest([]string{"-module_name", "example.com/mod"}, map[string]string{"utils.go": "package main\n\nfunc helper() int { return 42 }\n"}),
		encodeRequest([]string{"-d"}, map[string]string{"main.go": simpleMain}))

	assert.Equal(t, statusOK, responses[0].status)
	first := decodeTrees(t, responses[0].body)
	require.Contains(t, first, "main.go")
	assert.Nil(t, first["main.go"].Error)
	assert.Equal(t, statusOK, responses[1].status)
	second := decodeTrees(t, responses[1].body)
	assert.Contains(t, second, "utils.go")
	assert.NotContains(t, second, "main.go", "a request must not see the files of a previous one")
	// Each request is parsed with its own flags.
	assert.Equal(t, statusOK, responses[2].status)
	assert.Contains(t, responses[2].body, "Package: token.Pos(1)")
}

func TestServerResolvesGcExportDataWrittenByAPreviousRequest(t *testing.T) {
	gcExportDataDir := t.TempDir()
	libFiles := map[string]string{"lib/util/util.go": "package util\n\ntype Thing struct{}\n"}
	appFiles := map[string]string{"app/main.go": "package main\n\nimport \"example.com/lib/util\"\n\nvar thing util.Thing\n"}
	parseApp := encodeRequest([]string{"-gc_export_data_dir", gcExportDataDir, "-module_name", "example.com/app", "-module_base_dir", "app"},
		appFiles)
	// The GC export data of each module is written under its own base directory, and the other modules find it
	// through the cross-module index.
	exportLib := encodeRequest([]string{"-dump_gc_export_data", "-gc_export_data_dir", filepath.Join(gcExportDataDir, "lib"),
		"-module_name", "example.com/lib", "-package_path", "util"}, libFiles)

	s := &server{}
	responses := serveRequests(t, s, parseApp, exportLib, parseApp)

	assert.Equal(t, statusOK, responses[0].status)
	assert.NotContains(t, identifierTypes(t, responses[0].body), "example.com/lib/util.Thing", "nothing is exported yet")
	assert.Equal(t, response{statusOK, ""}, responses[1], "exporting writes to disk only")
	assert.FileExists(t, filepath.Join(gcExportDataDir, "lib", "example.com", "lib", "util", "util.o"))
	assert.Equal(t, statusOK, responses[2].status)
	assert.Contains(t, identifierTypes(t, responses[2].body), "example.com/lib/util.Thing",
		"the index built by the first request must include new GC export data")
	assert.Equal(t, 1, s.crossIndexes[gcExportDataDir].builds, "the project index must not be walked again after the export")
}

func TestServerReusesCrossIndexBetweenParseRequests(t *testing.T) {
	s := &server{}
	parse := Params{gcExportDataDir: "dir"}

	first := s.crossIndexFor(parse)
	assert.Same(t, first, s.crossIndexFor(Params{gcExportDataDir: "dir", moduleBaseDir: "other"}))
	assert.NotSame(t, first, s.crossIndexFor(Params{gcExportDataDir: "other"}))

	assert.Same(t, first, s.crossIndexFor(Params{gcExportDataDir: "dir", dumpGcExportData: true}))
	assert.Same(t, first, s.crossIndexFor(parse))
}

func TestServerAddsExportsToAnAlreadyBuiltIndexWithoutWalkingAgain(t *testing.T) {
	dir := t.TempDir()
	s := &server{}
	index := s.crossIndexFor(Params{gcExportDataDir: dir})
	_, _ = index.lookup(context.Background(), "missing", "other")
	require.Equal(t, 1, index.builds)

	responses := serveRequests(t, s,
		encodeRequest([]string{"-dump_gc_export_data", "-gc_export_data_dir", dir, "-module_name", "example.com/mod", "-package_path", "a"},
			map[string]string{"a/a.go": "package a\ntype Thing struct{}\n"}),
		encodeRequest([]string{"-dump_gc_export_data", "-gc_export_data_dir", dir, "-module_name", "example.com/mod", "-package_path", "b"},
			map[string]string{"b/b.go": "package b\ntype Other struct{}\n"}))

	assert.Equal(t, []response{{statusOK, ""}, {statusOK, ""}}, responses)
	assert.Same(t, index, s.crossIndexes[dir])
	assert.Equal(t, 1, index.builds)
	pathA, foundA := index.lookup(context.Background(), "example.com/mod/a", "other")
	pathB, foundB := index.lookup(context.Background(), "example.com/mod/b", "other")
	assert.True(t, foundA)
	assert.True(t, foundB)
	assert.FileExists(t, pathA)
	assert.FileExists(t, pathB)
}

func TestServerAnswersFailedRequestsWithAnErrorAndCarriesOn(t *testing.T) {
	// A name length pointing past the end of the payload makes the payload decoder panic.
	var corruptedPayload bytes.Buffer
	writeBytes(&corruptedPayload, int32(999))
	corruptedPayload.WriteString("x")
	var corrupted bytes.Buffer
	writeBytes(&corrupted, int32(0))
	writeBytes(&corrupted, int32(corruptedPayload.Len()))
	corrupted.Write(corruptedPayload.Bytes())

	responses := serveRequests(t, &server{},
		encodeRequest([]string{"-undefined"}, map[string]string{"main.go": simpleMain}),
		encodeRequest([]string{"-server"}, map[string]string{"main.go": simpleMain}),
		corrupted.Bytes(),
		encodeRequest([]string{"-dump_gc_export_data"}, map[string]string{"main.go": simpleMain}),
		encodeRequest(nil, map[string]string{"main.go": simpleMain}))

	assert.Equal(t, response{statusError, "flag provided but not defined: -undefined"}, responses[0])
	assert.Equal(t, response{statusError, "flag provided but not defined: -server"}, responses[1])
	assert.Equal(t, statusError, responses[2].status)
	assert.Contains(t, responses[2].body, "slice bounds out of range")
	assert.Equal(t, response{statusError, "If the dump_gc_export_data flag is set then the gc_export_data_dir flag must be set too"}, responses[3])
	assert.Equal(t, statusOK, responses[4].status, "the server must survive the failed requests")
	assert.Nil(t, decodeTrees(t, responses[4].body)["main.go"].Error)
}

func TestServerStopsWhenInputEndsBetweenRequests(t *testing.T) {
	var output bytes.Buffer
	assert.NoError(t, (&server{}).serve(bytes.NewReader(nil), &output))
	assert.Empty(t, output.Bytes())
}

func TestServerFailsOnCorruptedStream(t *testing.T) {
	request := encodeRequest([]string{"-module_name", "example.com/mod"}, map[string]string{"main.go": simpleMain})
	negativeLength := []byte{0xff, 0xff, 0xff, 0xff}
	tooManyArguments := binary.LittleEndian.AppendUint32(nil, maxArgumentCount+1)

	cases := map[string]struct {
		input []byte
		err   string
	}{
		"truncated argument count": {request[:2], "unexpected EOF"},
		"truncated argument":       {request[:6], "unexpected EOF"},
		"truncated payload":        {request[:len(request)-1], "unexpected EOF"},
		"missing payload":          {request[:4+4+len("-module_name")+4+len("example.com/mod")], "unexpected EOF"},
		"negative length":          {negativeLength, "invalid length -1"},
		"too many arguments":       {tooManyArguments, "invalid length 1025"},
	}
	for name, testCase := range cases {
		t.Run(name, func(t *testing.T) {
			var output bytes.Buffer
			err := (&server{}).serve(bytes.NewReader(testCase.input), &output)
			require.Error(t, err)
			assert.Contains(t, err.Error(), testCase.err)
			assert.Empty(t, output.Bytes())
		})
	}
}

func TestAnalyzeFailsWhenOutputCannotBeWritten(t *testing.T) {
	reader, writer, err := os.Pipe()
	require.NoError(t, err)
	require.NoError(t, writer.Close())
	defer func() {
		assert.NoError(t, reader.Close())
	}()

	defer func() {
		assert.Contains(t, recover(), "cannot write the output: ")
	}()
	analyze(context.Background(), Params{}, bytes.NewReader(encodeFiles(map[string]string{"main.go": simpleMain})), writer, nil, &analysisStats{})
	assert.Fail(t, "analyze must panic when its output cannot be written")
}

func TestServerFailsWhenResponseCannotBeWritten(t *testing.T) {
	request := encodeRequest(nil, map[string]string{"main.go": simpleMain})
	reader, writer, err := os.Pipe()
	require.NoError(t, err)
	require.NoError(t, reader.Close())

	err = (&server{}).serve(bytes.NewReader(request), writer)

	require.Error(t, err)
	assert.Contains(t, err.Error(), "cannot write the response")
}

func TestRunServerKeepsStdoutForResponses(t *testing.T) {
	stdinReader, stdinWriter, err := os.Pipe()
	require.NoError(t, err)
	stdoutReader, stdoutWriter, err := os.Pipe()
	require.NoError(t, err)
	oldStdin, oldStdout := os.Stdin, os.Stdout
	os.Stdin, os.Stdout = stdinReader, stdoutWriter
	defer func() {
		os.Stdin, os.Stdout = oldStdin, oldStdout
	}()

	done := make(chan struct{})
	go func() {
		defer close(done)
		runServer()
		assert.Same(t, stdoutWriter, os.Stdout, "stdout must be restored once the server stops")
		assert.NoError(t, stdoutWriter.Close())
	}()

	// A request is only written once the previous response was read, like the Java client does.
	responses := bufio.NewReader(stdoutReader)
	for _, file := range []string{"a.go", "b.go"} {
		_, err = stdinWriter.Write(encodeRequest(nil, map[string]string{file: simpleMain}))
		require.NoError(t, err)
		status, err := responses.ReadByte()
		require.NoError(t, err)
		var length int32
		require.NoError(t, binary.Read(responses, binary.LittleEndian, &length))
		body := make([]byte, length)
		_, err = io.ReadFull(responses, body)
		require.NoError(t, err)
		assert.Equal(t, statusOK, status)
		assert.Contains(t, decodeTrees(t, string(body)), file)
	}
	require.NoError(t, stdinWriter.Close())
	<-done
}

func TestRunServerPanicsOnCorruptedStream(t *testing.T) {
	stdinReader, stdinWriter, err := os.Pipe()
	require.NoError(t, err)
	oldStdin := os.Stdin
	os.Stdin = stdinReader
	defer func() {
		os.Stdin = oldStdin
	}()
	_, err = stdinWriter.Write([]byte{0xff, 0xff, 0xff, 0xff})
	require.NoError(t, err)
	require.NoError(t, stdinWriter.Close())

	assert.PanicsWithError(t, "cannot read the request: invalid length -1", runServer)
}

// A long-lived process must not accumulate memory from one request to the next.
func TestServerDoesNotRetainMemoryBetweenRequests(t *testing.T) {
	content, err := os.ReadFile("resources/simple_file_with_packages.go.source")
	require.NoError(t, err)
	gcExportDataDir := t.TempDir()
	parse := encodeRequest([]string{"-gc_export_data_dir", gcExportDataDir, "-module_name", "example.com/mod"},
		map[string]string{"main.go": string(content)})
	export := encodeRequest([]string{"-dump_gc_export_data", "-gc_export_data_dir", gcExportDataDir, "-module_name", "example.com/mod", "-package_path", "pkg"},
		map[string]string{"pkg/pkg.go": "package pkg\n\ntype Thing struct{}\n"})
	s := &server{}
	serveBatch := func(count int) {
		for range count {
			serveRequests(t, s, parse, export)
		}
	}

	serveBatch(5)
	before := liveHeapAfterGC()
	var allocBefore runtime.MemStats
	runtime.ReadMemStats(&allocBefore)
	serveBatch(50)
	after := liveHeapAfterGC()
	var allocAfter runtime.MemStats
	runtime.ReadMemStats(&allocAfter)

	const tolerance = 2 << 20
	allocated := allocAfter.TotalAlloc - allocBefore.TotalAlloc
	require.Greater(t, allocated, uint64(50*tolerance), "the requests must allocate enough for a leak to stand out")
	assert.Less(t, int64(after)-int64(before), int64(tolerance), "live heap grew from %d to %d bytes", before, after)
}

func TestReleaseMemoryAfterLargeRequest(t *testing.T) {
	defer func(limit uint64) {
		retainedMemoryLimit = limit
	}(retainedMemoryLimit)
	retainedMemoryLimit = 8 << 20
	retained := func() uint64 {
		var stats runtime.MemStats
		runtime.ReadMemStats(&stats)
		return stats.HeapSys - stats.HeapReleased
	}

	// Allocated in a function of its own, so that it is garbage once the function returns.
	func() {
		large := make([][]byte, 64)
		for i := range large {
			large[i] = make([]byte, 1<<20)
		}
		require.Greater(t, retained(), retainedMemoryLimit)
		runtime.KeepAlive(large)
	}()

	releaseMemoryAfterLargeRequest(largeRequestThreshold, 0)

	assert.LessOrEqual(t, retained(), retainedMemoryLimit)
}

func liveHeapAfterGC() uint64 {
	runtime.GC()
	var stats runtime.MemStats
	runtime.ReadMemStats(&stats)
	return stats.HeapAlloc
}

func TestParseRequestArgs(t *testing.T) {
	params, err := parseRequestArgs([]string{"-module_name", "example.com/mod", "-module_base_dir", "service", "-gc_export_data_dir", "dir",
		"-debug_type_check"})

	require.NoError(t, err)
	assert.Equal(t, Params{moduleName: "example.com/mod", moduleBaseDir: "service", gcExportDataDir: "dir", debugTypeCheck: true}, params)

	params, err = parseRequestArgs(nil)
	require.NoError(t, err)
	assert.Equal(t, Params{moduleBaseDir: "."}, params, "the defaults of the command line apply")
}
