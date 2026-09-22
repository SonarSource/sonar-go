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
	"context"
	"encoding/json"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"runtime"
	"runtime/trace"
	"time"
)

// TraceEnvVar names a file that receives Chrome trace events, one JSON object per line. The Java
// benchmark harness merges that file with its own events into a single trace.
const TraceEnvVar = "SONAR_GO_TRACE"

// ExecTraceEnvVar names a directory that receives one Go execution trace per process, readable with
// `go tool trace`. README.md documents when it is worth enabling.
const ExecTraceEnvVar = "SONAR_GO_EXEC_TRACE"

// The Java harness owns pid 1; every sonar-go-to-slang invocation is a thread of pid 2 so that
// trace viewers group the whole fleet of short-lived processes under one process track.
const tracePid = 2

var (
	// traceOut is nil unless TraceEnvVar is set, which is what makes every span site a nil check in
	// production. Writes are single, line-sized and O_APPEND, so concurrent processes cannot interleave.
	traceOut  *os.File
	execTrace *os.File
	traceTid  int
)

// initTracing enables tracing according to TraceEnvVar and ExecTraceEnvVar. Both are independent: the
// Chrome events feed the cross-language timeline, the execution trace feeds `go tool trace`. Failure to
// enable is reported on stderr and left non-fatal, because tracing must never break an analysis.
func initTracing() {
	traceTid = os.Getpid()

	if path := os.Getenv(TraceEnvVar); path != "" {
		file, err := os.OpenFile(path, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0o644)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Tracing disabled, cannot open %s: %v\n", path, err)
		} else {
			traceOut = file
		}
	}

	if dir := os.Getenv(ExecTraceEnvVar); dir != "" {
		if err := startExecTrace(dir); err != nil {
			fmt.Fprintf(os.Stderr, "Execution tracing disabled: %v\n", err)
		}
	}
}

func startExecTrace(dir string) error {
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	file, err := os.Create(filepath.Join(dir, fmt.Sprintf("trace-%d.out", os.Getpid())))
	if err != nil {
		return err
	}
	if err := trace.Start(file); err != nil {
		file.Close()
		return err
	}
	execTrace = file
	return nil
}

func shutdownTracing() {
	if execTrace != nil {
		trace.Stop()
		execTrace.Close()
		execTrace = nil
	}
	if traceOut != nil {
		traceOut.Close()
		traceOut = nil
	}
}

func tracingEnabled() bool {
	return traceOut != nil || trace.IsEnabled()
}

// span starts a timed region and returns a function that ends it. Arguments are alternating key/value
// pairs attached to the event; passing more pairs to the returned function merges them in, which is
// how a span records a fact it only learns at the end such as an output size.
//
// The context is taken and returned but unused: neither backend needs it to nest spans (runtime/trace
// nests by goroutine, Chrome events by timestamp). It is in the signature because a backend that does
// carry the parent span in the context — OpenTelemetry — could then be dropped in by rewriting this
// function alone, rather than by threading a context through the whole analysis pipeline first.
func span(ctx context.Context, name string, args ...any) (context.Context, func(...any)) {
	if !tracingEnabled() {
		return ctx, func(...any) {
			// No span was started, so there is nothing to end and no event to record.
		}
	}
	start := time.Now()
	region := trace.StartRegion(ctx, name)
	return ctx, func(endArgs ...any) {
		region.End()
		writeEvent(map[string]any{
			"name": name,
			"cat":  "go",
			"ph":   "X",
			"pid":  tracePid,
			"tid":  traceTid,
			"ts":   start.UnixMicro(),
			"dur":  time.Since(start).Microseconds(),
		}, append(args, endArgs...))
	}
}

// counter emits a timestamped numeric sample, aligned to the span timeline.
func counter(name string, args ...any) {
	writeEvent(map[string]any{
		"name": name,
		"cat":  "go",
		"ph":   "C",
		"pid":  tracePid,
		"tid":  traceTid,
		"ts":   time.Now().UnixMicro(),
	}, args)
}

// heapCounter samples the Go heap. runtime.ReadMemStats stops the world, so this belongs at phase
// boundaries only — never per file or per import.
func heapCounter(name string) {
	if traceOut == nil {
		return
	}
	var stats runtime.MemStats
	runtime.ReadMemStats(&stats)
	// NumGC and PauseTotalNs come from the same stop-the-world read, so they are free here and are the
	// only way to tell a GC-bound process from a slow one.
	counter(name, "heapAllocKB", stats.HeapAlloc/1024, "heapSysKB", stats.HeapSys/1024,
		"numGC", stats.NumGC, "gcPauseUs", stats.PauseTotalNs/1000)
}

// flowFinish closes the arrow the harness opened when it spawned this process. The OS pid is the shared
// flow id: the harness reads it from Process.pid() right after spawning, so neither side needs to agree
// on a counter or pass an id through the wire protocol.
func flowFinish() {
	writeEvent(map[string]any{
		"name": "spawn",
		"cat":  "boundary",
		"ph":   "f",
		"bp":   "e",
		"id":   os.Getpid(),
		"pid":  tracePid,
		"tid":  traceTid,
		"ts":   time.Now().UnixMicro(),
	}, nil)
}

// setTraceProcessLabel groups every invocation under one named process in the timeline.
func setTraceProcessLabel() {
	writeEvent(map[string]any{
		"name": "process_name",
		"ph":   "M",
		"pid":  tracePid,
		"args": map[string]any{"name": "sonar-go-to-slang"},
	}, nil)
}

// setTraceLaneLabel names this invocation's own lane. Metadata events are keyed by pid/tid rather than
// ordered, so this can be called late — which it has to be, because the batch is only identifiable once
// its file names have been read.
func setTraceLaneLabel(label string) {
	writeEvent(map[string]any{
		"name": "thread_name",
		"ph":   "M",
		"pid":  tracePid,
		"tid":  traceTid,
		"args": map[string]any{"name": label},
	}, nil)
}

func writeEvent(event map[string]any, args []any) {
	if traceOut == nil {
		return
	}
	if parsed := toArgs(args); len(parsed) > 0 {
		event["args"] = parsed
	}
	line, err := json.Marshal(event)
	if err != nil {
		return
	}
	// One write per event keeps O_APPEND lines from interleaving between concurrent processes.
	traceOut.Write(append(line, '\n'))
}

// fileSizeIfTracing should be used to report the byte count of an IO operation that its own API does not return.
func fileSizeIfTracing(file fs.File) int64 {
	if !tracingEnabled() {
		return 0
	}
	stat, err := file.Stat()
	if err != nil {
		return 0
	}
	return stat.Size()
}

// toArgs folds alternating key/value pairs into a map. A dangling key or a non-string key is a bug at
// the call site, and it panics on both: this file only builds under the `sonartrace` tag, so the panic
// can only reach a benchmark run, where a loud failure beats an argument quietly missing from the trace.
func toArgs(args []any) map[string]any {
	if len(args) == 0 {
		return nil
	}
	if len(args)%2 != 0 {
		panic(fmt.Sprintf("tracing: got %d span arguments, want key/value pairs: %v", len(args), args))
	}
	parsed := make(map[string]any, len(args)/2)
	for i := 0; i < len(args); i += 2 {
		key, ok := args[i].(string)
		if !ok {
			panic(fmt.Sprintf("tracing: span argument key %v is a %T, want a string", args[i], args[i]))
		}
		parsed[key] = args[i+1]
	}
	return parsed
}
