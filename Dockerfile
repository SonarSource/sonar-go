# Possible combinations:
# * BUILD_ENV=dev: dev image (based on golang, w/o custom cert)
# * BUILD_ENV=dev_custom_cert: dev image with custom cert (based on golang, with custom cert)
# * BUILD_ENV=ci:
# * * ci image based on base image, w/o custom cert, Debian-based, with Java and without Go
# * * CI_BUILDER_IMAGE=[...]/base:java-17 ARCH=arm64: ci image based on base image, w/o custom cert, Alpine-based, with Java and without Go

ARG GO_VERSION
# Possible values: ci, dev_custom_cert, dev
ARG BUILD_ENV=ci
# Placeholder; actual value is present on CI
ARG CIRRUS_AWS_ACCOUNT=unknown
ARG CI_BUILDER_IMAGE=${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest
ARG ARCH=amd64

# For local builds, build an image with Go and a dedicated user.
FROM public.ecr.aws/docker/library/golang:${GO_VERSION}-bookworm AS dev_base

# This value should match the host user's UID for volume mounts to work correctly.
ARG UID=1000

RUN groupadd --system --gid ${UID} sonarsource \
      && useradd --system --gid sonarsource --uid ${UID} --shell /bin/bash --create-home sonarsource \
      && mkdir -p /home/sonarsource/sonar-go-to-slang

# If the custom certificate is not provided, use the base image.
# This mode can be activated by providing build argument `BUILD_ENV=dev`.
FROM dev_base AS dev_image

# Install a custom certificate from the build context into the image.
# This mode can be activated by providing build argument `BUILD_ENV=dev_custom_cert`.
FROM dev_base AS dev_custom_cert_image

ARG CA_CERT=Sonar-FGT-FW-TLS-Traffic-Inspection
ARG CERT_LOCATION=/usr/local/share/ca-certificates

ONBUILD COPY --from=root ${CA_CERT}.cer ${CERT_LOCATION}/${CA_CERT}.cer
ONBUILD WORKDIR ${CERT_LOCATION}
ONBUILD RUN cp ${CA_CERT}.cer ${CA_CERT}.crt && update-ca-certificates

# For CI, use a different base image and install Go manually.
# This mode can be activated by providing build argument `BUILD_ENV=ci`.
FROM ${CI_BUILDER_IMAGE} AS ci_image

ARG ARCH
ARG GO_VERSION

USER root

RUN curl --proto "=https" -sSfL https://dl.google.com/go/go${GO_VERSION}.linux-${ARCH}.tar.gz -o go${GO_VERSION}.linux-${ARCH}.tar.gz && \
        tar xf go${GO_VERSION}.linux-${ARCH}.tar.gz --directory=/opt

# Final stage using the base image from one of the previous stages.
FROM ${BUILD_ENV}_image

ARG GO_VERSION
ARG GOLANG_CI_LINT_VERSION=1.62.2

USER sonarsource

RUN curl --proto "=https" -sSfL https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b /home/sonarsource/go/bin v${GOLANG_CI_LINT_VERSION}

ENV PATH="/opt/go/bin:/opt/protoc/bin:/opt/musl/bin:/home/sonarsource/go/bin:${PATH}"
ENV GO_CROSS_COMPILE=1

WORKDIR "/home/sonarsource/sonar-go-to-slang"

# It caches Go dependecies in Docker image
# The "|| true" is needed because "go list" always return non zero exit code and suggest to run go mod tidy
RUN --mount=type=bind,target=/home/sonarsource/sonar-go-to-slang \
    go list -e $(go list -m all) || true
