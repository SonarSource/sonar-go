# sonar-go-to-slang

Generate slang serialized AST in JSON from a go source file.

## Requirements
* Docker (specifically, Docker Buildx)
* (optional, required on Sonar machines) CA certificate for FortiClient traffic inspection in the project root directory

## Building

### Build in Docker environment

For convenience, the build automatically provisions a Docker container with pre-configured Go environment.
The tasks are automatically linked to `assemble`, `check`, and `build` tasks, so you usually don't need to run them directly.

Build Docker image:

```shell
./gradlew :sonar-go-to-slang:buildDockerImage
```

Execute build inside Docker generating and building Go code and executing tests:

```shell
./gradlew :sonar-go-to-slang:dockerCompileGo
```

In case you system does not require the certificate for traffic inspection set `-DtrafficInspection=false` while running any Gradle task.
Otherwise, place the `.crt` file into the project root directory.

### What happens under the hood

The entire build process can be done with the `make.sh` script.

First, determine Go version used in the project:
```shell
export GO_VERSION=$(grep -E '^goVersion=' ../gradle.properties | cut -d'=' -f2)
```

The following command will download Go of the required version and build the project:

```shell
./make.sh build
```

Individual build steps are described below.

To generate `goparser_generated.go` file in current directory, run:

```shell
go generate
```

To create `sonar-go-to-slang` executable in current directory, run:

```shell
go build
```

To create `sonar-go-to-slang` executable in `$GOPATH/bin`, run:

```shell
go install
```

### Building on Windows

When trying to build `sonar-go-to-slang` on Windows, the build may fail with the following error:

> Create symbolic link at [...]\slang\sonar-go-to-slang\.gogradle\project_gopath\src\github.com\SonarSource\slang\sonar-go-to-slang failed
     
Creating the symbolic link by hand solves this problem:

* (Eventually enable [developer mode in Windows](https://docs.microsoft.com/en-us/windows/uwp/get-started/enable-your-device-for-development))

## Running

Run with `-h`, `-help`, or `--help` to see usage.

### Input Format

⚠️ **Important**: sonar-go-to-slang expects **binary-encoded input on stdin**, not command-line file arguments.

Input format (read from stdin, little-endian):
```
[For each file, in sequence]
  N (4 bytes, little-endian)  — filename length
  <filename> (N bytes)        — file path
  M (4 bytes, little-endian)  — content length
  <content> (M bytes)         — file content
```
See `sonar-go-commons/src/main/java/org/sonar/go/converter/GoParseCommand.java` for implementation details.

### Command-Line Options

- `-d` - Dump native Go AST instead of SLANG JSON
- `-debug_type_check` - Print type-checking errors/warnings to stderr
- `-dump_gc_export_data` - dump GC (Go compiler) export data
- `-gc_export_data_dir <dir>` - Directory containing `.o` files for cross-package type resolution
- `-module_base_dir <dir>` - Relative path to go.mod directory (default: `.`)
- `-module_name <name>` - Module name from go.mod (required for type checking)
- `-package_path <name>` - Specify package path (e.g. foo/bar for files located in ${projectDir}/foo/bar)

## Tracing and profiling

`sonar-go-to-slang` can emit a performance trace of the analysis pipeline — stdin decoding, parsing,
type checking, import resolution, SLANG mapping and JSON encoding. It is used to profile the analyzer
and to build a single cross-language timeline together with the events the Java side records.

The instrumentation is behind the `sonartrace` build tag and is **not** compiled into released binaries:
default builds get no-op stubs from `tracing_stub.go`, so there is no runtime cost and no extra
dependency (`encoding/json`, `runtime/trace`) in production.

### Building with tracing enabled

```shell
go generate
go build -tags sonartrace
```

Without `-tags sonartrace` the two environment variables below are ignored, whatever their value.

### Enabling a trace at runtime

Two independent backends, each activated by an environment variable. Both are inert when unset, and a
failure to enable either one is reported on stderr and left non-fatal — tracing must never break an
analysis.

| Variable | Value | Output |
| --- | --- | --- |
| `SONAR_GO_TRACE` | path to a file | Chrome trace events, one JSON object per line (NDJSON), appended |
| `SONAR_GO_EXEC_TRACE` | path to a directory | one Go execution trace per process, `trace-<pid>.out`, for `go tool trace` |

`SONAR_GO_TRACE` appends with `O_APPEND` and writes one line per event, so several concurrent
invocations can safely share a single file — which is the point: the Java harness passes the same path
to every spawned process and gets one merged trace back.

`SONAR_GO_EXEC_TRACE` costs an initial runtime state dump, which is a large relative distortion on a
process this short-lived. Use it to drill into a few packages, not to trace a whole corpus.

Remember that `sonar-go-to-slang` reads its input from stdin in the binary format described above, so
the environment has to be applied to the binary itself rather than to the producer of the pipe:

```shell
producer | env SONAR_GO_TRACE=/tmp/trace.ndjson /path/to/sonar-go-to-slang -module_name demo > /dev/null
```

### Reading the trace

`SONAR_GO_TRACE` output is NDJSON, not a ready-made trace file. Wrap the lines into a
`traceEvents` array before loading it into a trace viewer:

```shell
jq -s '{traceEvents: .}' /tmp/trace.ndjson > /tmp/chrome-trace.json
```

For the execution traces:

```shell
go tool trace /tmp/exectrace/trace-<pid>.out
```

### What gets emitted

Duration spans (`ph: "X"`) follow the pipeline: reading stdin, decoding the batch, parsing each file,
type checking and resolving imports, indexing uses, then mapping and encoding each tree. Read the names
and the arguments they carry off a trace instead of from a list that would drift out of date:

```shell
# every span name that occurred, with the argument keys it recorded
jq -r 'select(.ph == "X") | "\(.name)\t\(.args // {} | keys | join(", "))"' /tmp/trace.ndjson | sort -u
```

One span is not where its name suggests: `crossindex.build` covers the one-off walk of the GC export
data directory, kept separate so the walk is not charged to whichever import happened to trigger it.

Plus heap counters (`ph: "C"`) at phase boundaries — `heap.start`, `heap.afterTypeCheck`, `heap.end` —
each carrying `heapAllocKB`, `heapSysKB`, `numGC` and `gcPauseUs`. They call `runtime.ReadMemStats`,
which stops the world, so they are deliberately limited to phase boundaries and never emitted per file
or per import.

Two metadata events (`ph: "M"`) label the timeline: `process_name` groups every invocation under one
`sonar-go-to-slang` process track, and `thread_name` names this invocation's lane (the package path
under `-package_path`, otherwise the lowest directory among the batch's files). Every invocation is a
thread of `pid` 2, because the Java harness owns `pid` 1.

Finally, a `spawn` flow-finish event (`ph: "f"`) closes the arrow the Java side opens when it spawns the
process. The OS pid is the shared flow id, so neither side has to agree on a counter or pass an id
through the wire protocol.

Span names are a contract with the tools that read the merged trace; `TestAnalysisPipelineEmitsExpectedSpans`
in `tracing_test.go` fails on a rename rather than letting it silently produce an unreadable timeline.

## Testing

To perform the tests, run:

```shell
go test
```

To update expected test data, use the method `fix_all_go_files_test_automatically` in `goparser_test.go`.

The tracing tests are behind the same build tag as the feature they cover, so a plain `go test` skips
them. To run them:

```shell
go test -tags sonartrace
```

## Tips and tricks

### Segmentation fault during Go build on MacOS Sequoia

In case you can't run it on MacOs Sequoia 15.3.1 and have the issues like: `illegal instructions` or `reflect: /usr/local/go/pkg/tool/linux_amd64/asm: signal: segmentation fault` try flipping the Rosetta option in Docker Settings.
Disabling `Use Rosetta for x86_64/amd64 emulation on Apple Silicon` usually solve the problem.
Src: https://www.reddit.com/r/golang/comments/1eoe3on/docker_illegal_instructions_and_apple_silicon/
