#! /usr/bin/env bash
set -euox pipefail

readonly GO_VERSION="${GO_VERSION:-1.23.4}"
readonly DEFAULT_GO_BINARY_DIRECTORY="${GOPATH:=${HOME}/go}/bin"
readonly DEFAULT_GO_BINARY="${DEFAULT_GO_BINARY_DIRECTORY}/go"

is_go_binary_the_expected_version() {
  if [[ "${#}" -ne 2 ]]; then
    echo "Usage: is_go_binary_the_expected_version <path/to/binary> <expected version>"
    exit 1
  fi
  local go_binary="${1}"
  local expected_version="${2}"
  bash -c "${go_binary} version" | grep --quiet "${expected_version}"
}

go_download_go() {
  if [[ "${#}" -ne 2 ]]; then
    echo "Usage: go_install_go <path/to/binary> <expected version>"
    exit 1
  fi
  local go_binary="${1}"
  local expected_version="${2}"
  bash -c "${go_binary} install golang.org/dl/go${go_version}@latest"
  go_binary="${DEFAULT_GO_BINARY_DIRECTORY}/go${go_version}"
  if [[ ! -f "${go_binary}" ]]; then
    if [[ -f "${DEFAULT_GO_BINARY}" ]] && is_go_binary_the_expected_version "${DEFAULT_GO_BINARY}" "${go_version}"; then
      go_binary="${DEFAULT_GO_BINARY}"
    else
      echo "Could not find designated go binary after download" >&2
      exit 1
    fi
  fi
  bash -c "${go_binary} download"
  echo "${go_binary}"
}

install_go() {
  if [[ "${#}" -ne 1 ]]; then
    echo "Usage: install_go <go version>" >&2
    exit 1
  fi

  local go_version="${1}"
  local go_binary

  local go_in_path
  go_in_path=$(command -v go)
  if [[ -n "${go_in_path}" ]]; then
    if is_go_binary_the_expected_version "${go_in_path}" "${go_version}"; then
      go_binary="${go_in_path}"
    else
      go_binary=$(go_download_go "${go_in_path}" "${go_version}")
    fi
  elif [[ -f "${DEFAULT_GO_BINARY}" ]]; then
    if is_go_binary_the_expected_version "${DEFAULT_GO_BINARY}" "${go_version}"; then
      go_binary="${DEFAULT_GO_BINARY}"
    else
      go_binary=$(go_download_go "${DEFAULT_GO_BINARY}" "${go_version}")
    fi
  else
    # Download go
    pushd "${HOME}" >&2
    local url="https://dl.google.com/go/go${go_version}.linux-amd64.tar.gz"
    curl --request GET "${url}" --output go.linux-amd64.tar.gz --silent
    tar xvf go.linux-amd64.tar.gz >/dev/null 2>&1
    if [[ ! -f "${DEFAULT_GO_BINARY}" ]]; then
      echo "Could not extract go from archive" >&2
      popd >&2
      exit 2
    fi
    popd >&2
    # Set up env variables for go
    export PATH="${PATH}:${DEFAULT_GO_BINARY_DIRECTORY}"
    go_binary="${DEFAULT_GO_BINARY}"
  fi
  echo "${go_binary}"
}

compile_binaries() {
  # Install the proper go version
  local platform="${1:-}"
  local architecture="${2:-}"
  local path_to_binary
  path_to_binary=$(install_go "${GO_VERSION}")
  # Build
  bash -c "${path_to_binary} run generate_source.go"

  mkdir -p build/executable
  build_for_platform() {
    local os="${1}"
    local arch="${2}"
    local extension="${3:-}"
    CGO_ENABLED=0 GOOS="${os}" GOARCH="${arch}" ${path_to_binary} build -o build/executable/sonar-go-to-slang-"${os}"-"${arch}""${extension}" "${GO_FLAGS[@]}"
  }
  # Note: -ldflags="-s -w" is used to strip debug information from the binary and reduce its size.
  GO_FLAGS=(-ldflags='-s -w' -buildmode=exe)
  if [ "${GO_CROSS_COMPILE:-}" != 0 ]; then
    echo "Building for all supported platforms"
    build_for_platform "darwin" "amd64"
    build_for_platform "darwin" "arm64"
    build_for_platform "linux" "amd64"
    build_for_platform "linux" "arm64"
    build_for_platform "windows" "amd64" ".exe"
  else
    if [[ -n "$platform" && -n "$architecture" ]]; then
      GOOS=$platform
      GOARCH=$architecture
    else
      GOOS=$("${path_to_binary}" env GOOS)
      GOARCH=$("${path_to_binary}" env GOARCH)
    fi
    if [[ "$GOOS" == "windows" ]]; then
      EXTENSION=".exe"
    else
      EXTENSION=""
    fi
    echo "Building for platform: ${GOOS}/${GOARCH}"
    build_for_platform "${GOOS}" "${GOARCH}" "${EXTENSION}"
  fi
}

generate_test_report() {
  # Install the proper go version
  local path_to_binary
  path_to_binary=$(install_go "${GO_VERSION}")
  # Test
  # bash -c "${path_to_binary} test -json > test-report.out"
  CGO_ENABLED=0 bash -c "${path_to_binary} test -timeout 5s -coverprofile=build/test-coverage.out -json > build/test-report.json"
}

main() {
  if [[ "${#}" -lt 1 ]]; then
    echo "Usage: ${0} build \[platform\] \[arch\] | clean | test"
    exit 0
  fi
  local command="${1}"
  local platform="${2:-}"
  local architecture="${3:-}"
  case "${command}" in
    build)
      compile_binaries "${platform}" "${architecture}"
      ;;
    test)
      generate_test_report
      ;;
    clean)
      rm -f goparser_generated.go
      rm -f build/sonar-go-to-slang-*
      rm -f build/test-report.json
      rm -f build/executable/*
      ;;
    *)
      echo "Unrecognized command ${command}" >&2
      exit 1
      ;;
  esac
  exit 0
}

main "${@}"
