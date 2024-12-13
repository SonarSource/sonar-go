# Sonar-Go

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-go.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-go)
[![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=SonarSource_sonar-go&metric=alert_status&token=sqb_f88f3f95eb835f3a47eaa59f8575229fc6ddbcc5)](https://next.sonarqube.com/sonarqube/dashboard?id=SonarSource_sonar-go)
[![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=SonarSource_sonar-go&metric=coverage&token=sqb_f88f3f95eb835f3a47eaa59f8575229fc6ddbcc5)](https://next.sonarqube.com/sonarqube/dashboard?id=SonarSource_sonar-go)

This is a developer documentation. If you want to analyze source code in SonarQube read the [analysis of Go documentation](https://docs.sonarqube.org/latest/analysis/languages/go/).

We use the native Go parser to parse the Go language.

## Have questions or feedback?

To provide feedback (request a feature, report a bug, etc.) use the [SonarQube Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language, plugin version, and SonarQube version.

## Building

### Setup

If you are on Windows, read the [sonar-go-to-slang/README.md](sonar-go-to-slang/README.md) instructions.


### Build
Build and run Unit Tests:

```shell
./gradlew build
```
`
## Integration Tests

By default, Integration Tests (ITs) are skipped during builds.
If you want to run them, you need first to retrieve the related projects which are used as input:

```shell
git submodule update --init
```

Then build and run the Integration Tests using the `its` property:

```shell
    ./gradlew build -Pits --info --no-daemon
```

You can also build and run only Ruling Tests using the `ruling` property:

```shell
./gradlew build -Pruling --info --no-daemon
```

## License headers

License headers are automatically updated by the spotless plugin but only for Java files. 
Furthermore, there are files such as `package-info.java` and `module-info.java` that spotless ignores. 
Also the Go source files are not handled. For those files use a manual script like below to update the license:

```shell
`find . -type f -name "*.go" -exec sed -i '' 's/2018-2023/2018-2024/' "{}" \;`
```

## License

Copyright 2012-2024 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions,
are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
