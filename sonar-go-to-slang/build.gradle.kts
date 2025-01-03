/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource SA
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
import org.sonarsource.cloudnative.gradle.callMake

plugins {
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.go-binary-builder")
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

val compileGo by tasks.registering(Exec::class) {
    group = "build"
    description = "Generate Go parser and build the Go executable"
    inputs.files(
        fileTree(projectDir).matching {
            include(
                "**/*.go",
                "**/go.mod",
                "**/go.sum",
                "make.bat",
                "make.sh"
            )
            exclude("build/**", "*_generated.go")
        }
    )
    outputs.dir("build/executable")

    callMake("build")
}

val testGo by tasks.registering(Exec::class) {
    group = "verification"
    description = "Generate Go test report"
    inputs.dir("build/executable")

    callMake("generate-test-report")
}

val cleanGo by tasks.registering(Exec::class) {
    group = "build"
    description = "Clean the Go build"

    callMake("clean")
}

tasks.clean {
    dependsOn(cleanGo)
}
testGo {
    dependsOn(compileGo)
}
tasks.build {
    dependsOn(testGo)
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
            gofmt("go$goVersion")
            target("*.go", "**/*.go")
            targetExclude("*_generated.go")
        }
    }
    // For now, these tasks rely on installation of Go performed by the call to make.sh
    tasks.spotlessCheck {
        dependsOn(compileGo)
    }
    tasks.spotlessApply {
        dependsOn(compileGo)
    }
}
