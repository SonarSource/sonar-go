# Sonar-Go

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-go.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-go)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SonarSource_sonar-go&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SonarSource_sonar-go)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SonarSource_sonar-go&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SonarSource_sonar-go)

This is a developer documentation. If you want to analyze source code in SonarQube read the [analysis of Go documentation](https://docs.sonarqube.org/latest/analysis/languages/go/).

We use the native Go parser to parse the Go language.

## Have questions or feedback?

To provide feedback (request a feature, report a bug, etc.) use the [SonarQube Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language, plugin version, and SonarQube version.

## Building

### Setup

To configure build dependencies, run the following command:

```shell
git submodule update --init -- build-logic
```

To always get the latest version of the build logic during git operations, set the following configuration:

```shell
git config submodule.recurse true
```

For more information see [README.md](https://github.com/SonarSource/cloud-native-gradle-modules/blob/master/README.md) of cloud-native-gradle-modules.

Additionally, if you are on Windows, read the [sonar-go-to-slang/README.md](sonar-go-to-slang/README.md) instructions.


### Build
Build and run Unit Tests:

```shell
./gradlew build
```

### Troubleshooting

In case you can't run it on MacOs Sequoia 15.3.1 and have the issues like: `illegal instructions` or `reflect: /usr/local/go/pkg/tool/linux_amd64/asm: signal: segmentation fault` try flipping the Rosetta option in Docker Settings. 
Disabling `Use Rosetta for x86_64/amd64 emulation on Apple Silicon` usually solve the problem. 
Src: https://www.reddit.com/r/golang/comments/1eoe3on/docker_illegal_instructions_and_apple_silicon/


## License headers

License headers are automatically updated by the spotless plugin.

## License

Copyright 2012-2025 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions,
are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
