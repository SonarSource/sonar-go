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
	"flag"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"runtime/debug"
)

// The process is kept alive for a whole step of an analysis, instead of being started for every directory. It reads
// requests from stdin and answers each of them on stdout, in order. All the lengths are little-endian int32, like in
// the payload itself:
//
//	request:  [argument count] ([length] [argument])* [length] [payload]
//	response: [status] [length] [body]
//
// The arguments are options defined by defineFlags, and the payload holds the files to analyze, as read by
// readAstFile. With statusOK, the body is the Slang AST in the format the request asked for, or the Go AST, and is
// empty when GC export data is written; with statusError, it is the reason why the request failed.
const (
	statusOK    byte = 0
	statusError byte = 1
)

// A corrupted length could otherwise make the server allocate an arbitrary amount of memory.
const (
	maxArgumentCount  = 1024      // Maximum number of arguments (option flags and values) in one request.
	maxFrameLength    = 1 << 30   // 1 GiB maximum for each request argument or payload frame.
	maxResponseLength = 256 << 20 // 256 MiB maximum response body sent to the Java client.
)

// runServer serves requests until the client closes stdin. A request that fails is answered with statusError and
// the server carries on, but a stream that cannot be read or written any more stops it, as there is no telling
// where the next request would start.
func runServer() {
	fmt.Fprintln(os.Stderr, "Starting in server mode")
	// Stdout carries the responses, so anything else printed there would corrupt them.
	protocolOut := os.Stdout
	os.Stdout = os.Stderr
	defer func() {
		os.Stdout = protocolOut
	}()
	if err := (&server{}).serve(os.Stdin, protocolOut); err != nil {
		fmt.Fprintf(os.Stderr, "Server stopped: %v\n", err)
		panic(err)
	}
}

type server struct {
	// Export and parse requests can use different directory roots. Keep each index for the whole process and
	// add newly exported files to every index whose root contains them.
	crossIndexes map[string]*crossModuleIndex
}

func (s *server) serve(in io.Reader, out io.Writer) error {
	ctx, done := span(context.Background(), "server")
	// Inside the span for the same reason as in main.
	flowFinish()
	setTraceLaneLabel("server")
	requestCount := 0
	defer func() {
		done("requestCount", requestCount)
	}()

	reader := bufio.NewReader(in)
	writer := bufio.NewWriter(out)
	for {
		args, payload, err := readRequest(reader)
		if err == io.EOF {
			return nil
		}
		if err != nil {
			return fmt.Errorf("cannot read the request: %w", err)
		}
		requestCount++
		status, body := s.handle(ctx, args, payload)
		if err := writeResponse(writer, status, body); err != nil {
			return fmt.Errorf("cannot write the response: %w", err)
		}
		payloadBytes, responseBytes := len(payload), len(body)
		payload, body = nil, nil
		releaseMemoryAfterLargeRequest(payloadBytes, responseBytes)
	}
}

// After a large request, release the heap if the server retains more than 256 MiB from the OS.
var retainedMemoryLimit uint64 = 256 << 20

// Only requests or responses of at least 16 MiB trigger a retained-heap check.
const largeRequestThreshold = 16 << 20

// releaseMemoryAfterLargeRequest hands the memory of a large request back to the OS. Otherwise the next GC is only
// due once the heap grows as large again, so the process would hold on to that memory while it analyzes the smaller
// directories that follow. Smaller requests are left to the GC, as
// releasing their memory would only make the next request take it back.
func releaseMemoryAfterLargeRequest(payloadBytes, responseBytes int) {
	if payloadBytes < largeRequestThreshold && responseBytes < largeRequestThreshold {
		return
	}
	var stats runtime.MemStats
	runtime.ReadMemStats(&stats)
	if stats.HeapSys-stats.HeapReleased > retainedMemoryLimit {
		debug.FreeOSMemory()
	}
}

// handle runs one request. It never panics: the panics raised by an invalid input are turned into a statusError
// response, so that only this request fails.
func (s *server) handle(ctx context.Context, args []string, payload []byte) (status byte, body []byte) {
	var exportRequest bool
	defer func() {
		if r := recover(); r != nil {
			if exportRequest {
				// An export may have written only some files before failing; rebuild on the next request.
				s.crossIndexes = nil
			}
			fmt.Fprintf(os.Stderr, "Request failed: %v\n", r)
			status, body = statusError, []byte(fmt.Sprint(r))
		}
	}()

	params, err := parseRequestArgs(args)
	if err != nil {
		return statusError, []byte(err.Error())
	}
	exportRequest = params.dumpGcExportData

	heapCounter("heap.start")
	// Named "main", as the tools reading the trace expect from the time every request had a process of its own.
	requestCtx, done := span(ctx, "main", analysisSpanArgs(params)...)
	stats := analysisStats{}
	defer func() {
		done("fileCount", stats.fileCount, "sourceBytes", stats.sourceBytes, "lane", stats.laneLabel)
		heapCounter("heap.end")
	}()

	var output bytes.Buffer
	index := s.crossIndexFor(params)
	analyze(requestCtx, params, bytes.NewReader(payload), &output, index, &stats)
	for _, path := range stats.exportedFiles {
		s.addExportedFile(path)
	}
	return statusOK, output.Bytes()
}

// crossIndexFor returns the index of the GC export data directory for the given request.
func (s *server) crossIndexFor(params Params) *crossModuleIndex {
	if s.crossIndexes == nil {
		s.crossIndexes = make(map[string]*crossModuleIndex)
	}
	index := s.crossIndexes[params.gcExportDataDir]
	if index == nil {
		index = &crossModuleIndex{dir: params.gcExportDataDir}
		s.crossIndexes[params.gcExportDataDir] = index
	}
	return index
}

func (s *server) addExportedFile(path string) {
	// Only indices rooted at an ancestor of the new file can contain it.
	for dir := filepath.Dir(path); ; dir = filepath.Dir(dir) {
		if index := s.crossIndexes[dir]; index != nil {
			index.addExportedFile(path)
		}
		if filepath.Dir(dir) == dir {
			return
		}
	}
}

func parseRequestArgs(args []string) (Params, error) {
	flagSet := flag.NewFlagSet("request", flag.ContinueOnError)
	flagSet.SetOutput(io.Discard)
	readParams := defineFlags(flagSet)
	if err := flagSet.Parse(args); err != nil {
		return Params{}, err
	}
	params := readParams()
	logParams(params)
	return params, nil
}

// readRequest returns io.EOF only when the stream ends between two requests, which is how the client stops the
// server; a stream that ends within a request is reported as io.ErrUnexpectedEOF.
func readRequest(reader *bufio.Reader) ([]string, []byte, error) {
	argCount, err := readLength(reader, maxArgumentCount)
	if err != nil {
		return nil, nil, err
	}
	args := make([]string, argCount)
	for i := range args {
		arg, err := readFrame(reader)
		if err != nil {
			return nil, nil, unexpectedEOF(err)
		}
		args[i] = string(arg)
	}
	payload, err := readFrame(reader)
	if err != nil {
		return nil, nil, unexpectedEOF(err)
	}
	return args, payload, nil
}

func readFrame(reader *bufio.Reader) ([]byte, error) {
	length, err := readLength(reader, maxFrameLength)
	if err != nil {
		return nil, err
	}
	frame := make([]byte, length)
	if _, err := io.ReadFull(reader, frame); err != nil {
		return nil, unexpectedEOF(err)
	}
	return frame, nil
}

func readLength(reader *bufio.Reader, maxLength int) (int, error) {
	var length int32
	if err := binary.Read(reader, binary.LittleEndian, &length); err != nil {
		return 0, err
	}
	if length < 0 || int(length) > maxLength {
		return 0, fmt.Errorf("invalid length %d", length)
	}
	return int(length), nil
}

func unexpectedEOF(err error) error {
	if err == io.EOF {
		return io.ErrUnexpectedEOF
	}
	return err
}

func writeResponse(writer *bufio.Writer, status byte, body []byte) error {
	if len(body) > maxResponseLength {
		status = statusError
		body = []byte("Go response exceeds the maximum size")
	}
	if err := writer.WriteByte(status); err != nil {
		return err
	}
	if err := binary.Write(writer, binary.LittleEndian, int32(len(body))); err != nil {
		return err
	}
	if _, err := writer.Write(body); err != nil {
		return err
	}
	return writer.Flush()
}
