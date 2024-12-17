/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
plugins {
    id("org.sonarsource.cloud-native.code-style-conventions")
}

sonarqube {
    properties {
        property("sonar.sources", ".")
        property("sonar.inclusions", "**/*.go")
        property("sonar.exclusions", "**/render.go,**/generate_source.go,**/*_generated.go,**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.tests", ".")
        property("sonar.test.inclusions", "**/*_test.go")
        property("sonar.test.exclusions", "**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.go.tests.reportPaths", "${project.projectDir}/.gogradle/reports/test-report.out")
        property(
            "sonar.go.coverage.reportPaths",
            "${project.projectDir}/.gogradle/reports/coverage/profiles/github.com%2FSonarSource%2Fslang%2Fsonar-go-to-slang.out"
        )
    }
}

val generateParserAndBuild = tasks.register<Exec>("generateParserAndBuild") {
    group = "build"
    description = "Generate Go parser and build the Go executable"

    commandLine("./make.sh")
    args("build")
}

val generateTestReport = tasks.register<Exec>("generateTestReport") {
    group = "verification"
    description = "Generate Go test report"

    commandLine("./make.sh")
    args("generate-test-report")
}

val cleanTask = tasks.register<Exec>("cleanTask") {
    group = "build"
    description = "Clean the Go build"

    commandLine("./make.sh")
    args("clean")
}

tasks.clean {
    dependsOn(cleanTask)
}
generateTestReport {
    dependsOn(generateParserAndBuild)
}
tasks.build {
    dependsOn(generateTestReport)
}

spotless {
    java {
        // No Java sources in this project
        target("")
    }
}

if (System.getenv("CI") == "true") {
    // spotless is enabled only for CI, because spotless relies on Go installation being available on the machine
    spotless {
        go {
            val goVersion = providers.environmentVariable("GO_VERSION").getOrElse("1.23.4")
            gofmt("go$goVersion").withGoExecutable(System.getenv("HOME") + "/go/bin/go")
            target("*.go", "**/*.go")
            targetExclude("*_generated.go")
        }
    }
    // For now, these tasks rely on installation of Go performed by the call to make.sh
    tasks.spotlessCheck {
        dependsOn(generateParserAndBuild)
    }
    tasks.spotlessApply {
        dependsOn(generateParserAndBuild)
    }
}
