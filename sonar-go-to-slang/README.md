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

## Testing

To perform the tests, run:

```shell
go test
```

To update expected test data, use the method `fix_all_go_files_test_automatically` in `goparser_test.go`.

## Tips and tricks

### Segmentation fault during Go build on MacOS Sequoia

In case you can't run it on MacOs Sequoia 15.3.1 and have the issues like: `illegal instructions` or `reflect: /usr/local/go/pkg/tool/linux_amd64/asm: signal: segmentation fault` try flipping the Rosetta option in Docker Settings.
Disabling `Use Rosetta for x86_64/amd64 emulation on Apple Silicon` usually solve the problem.
Src: https://www.reddit.com/r/golang/comments/1eoe3on/docker_illegal_instructions_and_apple_silicon/
