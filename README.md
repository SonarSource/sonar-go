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

### Dependency verification

Dependency verification by Gradle is enabled for this project. If unknown dependencies appear during the build, you will get an error
with a message like `Dependency verification failed for configuration ...`.

In this case, you can update the file `gradle/verification-metadata.xml` with the command:

```shell
./gradlew --write-verification-metadata sha256 <your gradle task>
```
and manually verify correctness of the added data.

In most cases, the `help` task will resolve most of the dependencies without executing anything:

```shell
./gradlew --write-verification-metadata sha256 help
```

The file is regenerated on CI automatically, once it grows over specified number of lines.

#### Known issues
Due to a known bug in Gradle, `--write-verification-metadata` can still ignore some dependencies, especially BOMs.
In this case, one workaround is to remove directory `$GRADLE_USER_HOME/caches/modules-2/metadata-2.x` and then call the Gradle command again.
See [GitHub issue](https://github.com/gradle/gradle/issues/20194#issuecomment-1652095447) for more context.

## License headers

License headers are automatically updated by the spotless plugin.

## License

Copyright 2012-2025 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions,
are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
