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

// The following directive is necessary to make the package coherent:
//go:build sonartrace

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// readTraceEvents parses the NDJSON written by initTracing into one map per line.
func readTraceEvents(t *testing.T, path string) []map[string]any {
	t.Helper()
	content, err := os.ReadFile(path)
	require.NoError(t, err)

	var events []map[string]any
	decoder := json.NewDecoder(bytes.NewReader(content))
	for decoder.More() {
		var event map[string]any
		require.NoError(t, decoder.Decode(&event))
		events = append(events, event)
	}
	return events
}

func eventNamed(events []map[string]any, name string) map[string]any {
	return eventWhere(events, func(event map[string]any) bool { return event["name"] == name })
}

func eventWhere(events []map[string]any, match func(map[string]any) bool) map[string]any {
	for _, event := range events {
		if match(event) {
			return event
		}
	}
	return nil
}

func TestTracingDisabledByDefault(t *testing.T) {
	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, "")
	initTracing()
	defer shutdownTracing()

	assert.False(t, tracingEnabled(), "tracing must stay off when neither env var is set")

	// Every annotation site has to be safe to call with tracing off; production runs this way.
	_, done := span(context.Background(), "noop", "key", "value")
	done("late", 1)
	counter("noop.counter", "value", 1)
	heapCounter("noop.heap")
	flowFinish()
	setTraceProcessLabel()
	setTraceLaneLabel("noop")
}

func TestTracingWritesChromeEvents(t *testing.T) {
	tracePath := filepath.Join(t.TempDir(), "trace.ndjson")
	t.Setenv(TraceEnvVar, tracePath)
	t.Setenv(ExecTraceEnvVar, "")

	initTracing()
	setTraceProcessLabel()
	setTraceLaneLabel("pkg=foo/bar")
	flowFinish()
	_, outer := span(context.Background(), "outer", "files", 2)
	_, inner := span(context.Background(), "inner", "file", "a.go")
	inner()
	outer("bytes", 42)
	counter("heap", "heapAllocKB", 128)
	shutdownTracing()

	events := readTraceEvents(t, tracePath)

	outerEvent := eventNamed(events, "outer")
	require.NotNil(t, outerEvent, "expected an 'outer' span in %v", events)
	assert.Equal(t, "X", outerEvent["ph"])
	assert.Equal(t, float64(tracePid), outerEvent["pid"])
	assert.Equal(t, float64(os.Getpid()), outerEvent["tid"])
	assert.Positive(t, outerEvent["ts"], "timestamps are wall-clock microseconds")

	// Args declared at the start and args supplied at the end have to end up on the same event.
	outerArgs := outerEvent["args"].(map[string]any)
	assert.Equal(t, float64(2), outerArgs["files"])
	assert.Equal(t, float64(42), outerArgs["bytes"])

	innerEvent := eventNamed(events, "inner")
	require.NotNil(t, innerEvent)
	assert.Equal(t, "a.go", innerEvent["args"].(map[string]any)["file"])

	// The inner span must be nested inside the outer one, otherwise the flame chart is wrong.
	innerStart := innerEvent["ts"].(float64)
	outerStart := outerEvent["ts"].(float64)
	assert.GreaterOrEqual(t, innerStart, outerStart)
	assert.LessOrEqual(t, innerStart+innerEvent["dur"].(float64), outerStart+outerEvent["dur"].(float64))

	flow := eventNamed(events, "spawn")
	require.NotNil(t, flow, "the Java->Go flow arrow must be closed")
	assert.Equal(t, "f", flow["ph"])
	assert.Equal(t, float64(os.Getpid()), flow["id"])

	require.NotNil(t, eventNamed(events, "thread_name"))
	assert.Equal(t, "C", eventNamed(events, "heap")["ph"])
}

func TestExecTraceWritesPerProcessFile(t *testing.T) {
	execDir := filepath.Join(t.TempDir(), "exec")
	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, execDir)

	initTracing()
	assert.True(t, tracingEnabled(), "regions must still be recorded with only the execution tracer on")
	_, done := span(context.Background(), "outer")
	done()
	shutdownTracing()

	entries, err := os.ReadDir(execDir)
	require.NoError(t, err)
	require.Len(t, entries, 1, "expected exactly one execution trace for this process")

	info, err := entries[0].Info()
	require.NoError(t, err)
	assert.Positive(t, info.Size(), "the execution trace must not be empty")
}

// The span names are a contract with the Python analysis tool that reads the merged trace, so a rename
// has to break a test rather than silently produce an unreadable timeline.
func TestAnalysisPipelineEmitsExpectedSpans(t *testing.T) {
	tracePath := filepath.Join(t.TempDir(), "trace.ndjson")
	t.Setenv(TraceEnvVar, tracePath)
	t.Setenv(ExecTraceEnvVar, "")

	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd"}
	callMainStdinFromFile("resources/simple_file_with_packages.go.source")

	events := readTraceEvents(t, tracePath)
	for _, name := range []string{
		"main", "stdin.read", "batch.decode", "parse.files", "parse.file", "typecheck.package",
		"import.resolve", "uses.index", "tree.map", "tree.encode", "tree.concat",
		"heap.start", "heap.afterTypeCheck", "heap.end",
	} {
		require.NotNil(t, eventNamed(events, name), "expected a %q event in the trace", name)
	}

	mainArgs := eventNamed(events, "main")["args"].(map[string]any)
	assert.Equal(t, float64(1), mainArgs["fileCount"])
	assert.Positive(t, mainArgs["sourceBytes"])
	assert.Equal(t, float64(os.Getpid()), mainArgs["pid"])

	parseFileArgs := eventNamed(events, "parse.file")["args"].(map[string]any)
	assert.Equal(t, "resources/simple_file_with_packages.go.source", parseFileArgs["fileName"])
	assert.Equal(t, false, parseFileArgs["parseError"])

	importArgs := eventNamed(events, "import.resolve")["args"].(map[string]any)
	assert.NotEmpty(t, importArgs["importPath"])
	assert.Contains(t, []any{"cache", "embedded", "same-module", "cross-module", "empty"}, importArgs["source"])

	// An embedded lookup must report the .o it read and the packages that read pulled in with it,
	// otherwise its duration cannot be divided into a per-byte cost.
	embedded := eventWhere(events, func(event map[string]any) bool {
		args, ok := event["args"].(map[string]any)
		return event["name"] == "import.resolve" && ok && args["source"] == "embedded"
	})
	require.NotNil(t, embedded, "the fixture imports fmt, which resolves from the embedded bundle")
	embeddedArgs := embedded["args"].(map[string]any)
	assert.Positive(t, embeddedArgs["oFileBytes"])
	assert.Positive(t, embeddedArgs["transitivePackages"])

	heapArgs := eventNamed(events, "heap.end")["args"].(map[string]any)
	assert.NotNil(t, heapArgs["numGC"])
	assert.NotNil(t, heapArgs["gcPauseUs"])

	assert.Positive(t, eventNamed(events, "tree.concat")["args"].(map[string]any)["outputBytes"])
}

func TestGcExportEmitsWriteSpan(t *testing.T) {
	tracePath := filepath.Join(t.TempDir(), "trace.ndjson")
	t.Setenv(TraceEnvVar, tracePath)
	t.Setenv(ExecTraceEnvVar, "")

	resetCommandLineFlagsToDefault()
	os.Args = []string{"cmd", "-dump_gc_export_data", "-gc_export_data_dir", t.TempDir()}
	callMainStdinFromFile("resources/simple_file_with_packages.go.source")

	events := readTraceEvents(t, tracePath)
	gcEvent := eventNamed(events, "gcexport.write")
	require.NotNil(t, gcEvent, "expected a gcexport.write span in %v", events)

	gcArgs := gcEvent["args"].(map[string]any)
	assert.Equal(t, "main", gcArgs["packagePath"])
	assert.NotEmpty(t, gcArgs["outputPath"])
	assert.Positive(t, gcArgs["outputBytes"])
}

// The directory walk is a fixed per-run cost, so it must be its own span rather than inflating whichever
// cross-module import happened to be first.
func TestCrossModuleIndexBuildEmitsItsOwnSpan(t *testing.T) {
	exportDir := t.TempDir()
	createTestExportData(t, filepath.Join(exportDir, "service2"), "shared/beta")

	tracePath := filepath.Join(t.TempDir(), "trace.ndjson")
	t.Setenv(TraceEnvVar, tracePath)
	t.Setenv(ExecTraceEnvVar, "")

	initTracing()
	index := &crossModuleIndex{dir: exportDir}
	_, found := index.lookup(context.Background(), "shared/beta", "service1")
	require.True(t, found)
	// A second lookup must not produce a second span: the walk happens once.
	index.lookup(context.Background(), "shared/beta", "service1")
	shutdownTracing()

	events := readTraceEvents(t, tracePath)
	var builds int
	for _, event := range events {
		if event["name"] == "crossindex.build" {
			builds++
		}
	}
	assert.Equal(t, 1, builds)

	buildArgs := eventNamed(events, "crossindex.build")["args"].(map[string]any)
	assert.Equal(t, exportDir, buildArgs["dir"])
	assert.Equal(t, float64(1), buildArgs["oFiles"])
	assert.Positive(t, buildArgs["importPaths"])
}

func TestFileSizeIfTracing(t *testing.T) {
	path := filepath.Join(t.TempDir(), "sized.o")
	file, err := os.Create(path)
	require.NoError(t, err)
	_, err = file.Write([]byte("hello"))
	require.NoError(t, err)

	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, "")
	initTracing()
	assert.Zero(t, fileSizeIfTracing(file), "must not stat the file when tracing is off")
	shutdownTracing()

	t.Setenv(TraceEnvVar, filepath.Join(t.TempDir(), "trace.ndjson"))
	initTracing()
	assert.EqualValues(t, 5, fileSizeIfTracing(file), "must report the current size once tracing is on")

	require.NoError(t, file.Close())
	assert.Zero(t, fileSizeIfTracing(file), "a stat error on a closed file must degrade to zero rather than panic")
	shutdownTracing()
}

func TestFileSizeIfTracingOnEmbeddedFile(t *testing.T) {
	var embeddedName string
	for _, name := range packageExportData {
		embeddedName = name
		break
	}
	require.NotEmpty(t, embeddedName, "the embedded bundle must contain at least one .o file")

	open := func() fs.File {
		file, err := packages.Open(PackageExportDataDir + "/" + embeddedName)
		require.NoError(t, err)
		t.Cleanup(func() { file.Close() })
		return file
	}

	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, "")
	initTracing()
	assert.Zero(t, fileSizeIfTracing(open()), "must not stat the embedded file when tracing is off")
	shutdownTracing()

	t.Setenv(TraceEnvVar, filepath.Join(t.TempDir(), "trace.ndjson"))
	initTracing()
	assert.Positive(t, fileSizeIfTracing(open()), "must report the embedded .o size once tracing is on")
	shutdownTracing()
}

func TestToArgsFoldsPairsAndPanicsOnMalformedOnes(t *testing.T) {
	assert.Nil(t, toArgs(nil))
	assert.Equal(t, map[string]any{"a": 1, "b": 2}, toArgs([]any{"a", 1, "b", 2}))

	// Malformed call sites are bugs, and this file only builds under the `sonartrace` tag, so they are
	// loud rather than silently dropping an argument from the trace.
	assert.Panics(t, func() { toArgs([]any{"dangling"}) }, "a key without a value must panic")
	assert.Panics(t, func() { toArgs([]any{"a", 1, "dangling"}) }, "a trailing key must panic")
	assert.Panics(t, func() { toArgs([]any{7, "notAKey"}) }, "a non-string key must panic")
}

// Tracing is opportunistic: every way of failing to enable it has to be reported and then ignored,
// because a broken trace destination must never take an analysis down with it.
func TestInitTracingReportsATraceFileItCannotOpen(t *testing.T) {
	t.Setenv(TraceEnvVar, filepath.Join(t.TempDir(), "missing-dir", "trace.ndjson"))
	t.Setenv(ExecTraceEnvVar, "")

	captureStdOutAndStdErr()
	initTracing()
	_, stderr := getStdOutAndStdErr()
	defer shutdownTracing()

	assert.Contains(t, stderr, "Tracing disabled, cannot open")
	assert.False(t, tracingEnabled(), "a trace file that cannot be opened must leave tracing off")
}

func TestInitTracingReportsAnExecTraceDirectoryItCannotCreate(t *testing.T) {
	// A regular file where a directory has to go is what makes MkdirAll fail.
	blocker := filepath.Join(t.TempDir(), "blocker")
	require.NoError(t, os.WriteFile(blocker, nil, 0644))

	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, filepath.Join(blocker, "exec"))

	captureStdOutAndStdErr()
	initTracing()
	_, stderr := getStdOutAndStdErr()
	defer shutdownTracing()

	assert.Contains(t, stderr, "Execution tracing disabled:")
	assert.False(t, tracingEnabled())
}

func TestStartExecTraceFailsWhenItsOwnFileCannotBeCreated(t *testing.T) {
	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, "")
	initTracing()
	defer shutdownTracing()

	// Occupying the per-process file name with a directory separates the two failure modes: the
	// directory tree is created, only the trace file itself cannot be.
	dir := t.TempDir()
	require.NoError(t, os.MkdirAll(filepath.Join(dir, fmt.Sprintf("trace-%d.out", os.Getpid())), 0755))

	assert.Error(t, startExecTrace(dir))
	assert.False(t, tracingEnabled(), "a failed start must not leave the execution tracer half-enabled")
}

func TestStartExecTraceFailsWhileAnotherTraceIsRunning(t *testing.T) {
	t.Setenv(TraceEnvVar, "")
	t.Setenv(ExecTraceEnvVar, t.TempDir())
	initTracing()
	defer shutdownTracing()
	require.True(t, tracingEnabled())

	// runtime/trace refuses a second concurrent tracer. The rejected attempt has to leave the
	// running one untouched, so that shutdown still stops and closes exactly one trace.
	assert.Error(t, startExecTrace(t.TempDir()))
	assert.True(t, tracingEnabled())
}

func TestWriteEventDropsAnEventItCannotSerialise(t *testing.T) {
	tracePath := filepath.Join(t.TempDir(), "trace.ndjson")
	t.Setenv(TraceEnvVar, tracePath)
	t.Setenv(ExecTraceEnvVar, "")

	initTracing()
	// A func value has no JSON form. The event has to be dropped whole rather than written as a
	// truncated line, which would make the entire NDJSON trace unparseable for the harness.
	counter("unserialisable", "callback", func() {})
	counter("serialisable", "value", 1)
	shutdownTracing()

	events := readTraceEvents(t, tracePath)
	assert.Nil(t, eventNamed(events, "unserialisable"))
	assert.NotNil(t, eventNamed(events, "serialisable"), "a dropped event must not stop the ones after it")
}
