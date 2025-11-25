# Sonar-Go

[![Build](https://github.com/SonarSource/sonar-go-enterprise/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/SonarSource/sonar-go-enterprise/actions/workflows/build.yml)
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

### Fix license packaging issues
During the Gradle build, a license packaging check is executed.
This check can also be triggered manually with `./gradlew validateLicenseFiles`.
It checks if the license files of third party libraries are correctly packaged to the resource folder according to SonarSource standards.
Since sonar-go bundles a go binary, we are also including the licenses of all used go dependencies.

If your build failed, you can fix the license packaging by running:

```shell
./gradlew generateLicenseResources
```

Note that this overwrites your current license files in the `resources/licenses` folder.

#### Known issues
Due to a known bug in Gradle, `--write-verification-metadata` can still ignore some dependencies, especially BOMs.
In this case, one workaround is to remove directory `$GRADLE_USER_HOME/caches/modules-2/metadata-2.x` and then call the Gradle command again.
See [GitHub issue](https://github.com/gradle/gradle/issues/20194#issuecomment-1652095447) for more context.

Using Rancher as the docker container management platform is currently not supported and may lead to issues.

## License headers


License headers are automatically updated by the spotless plugin.

## License

Copyright 2012-2025 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions,
are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
