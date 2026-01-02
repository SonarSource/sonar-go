/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.go-binary-builder")
    id("org.sonarqube")
}

val isCi: Boolean = System.getenv("CI")?.equals("true") == true

goBuild {
    dockerfile = layout.settingsDirectory.file("build-logic/go/Dockerfile")
    dockerWorkDir = "/home/sonarsource/sonar-go-to-slang"
    additionalOutputFiles.add(layout.projectDirectory.file("goparser_generated.go"))
}

if (isCi) {
    val goVersion = providers.environmentVariable("GO_VERSION")
        .orElse(providers.gradleProperty("goVersion"))
        .orNull ?: error("Either `GO_VERSION` env variable or `goVersion` Gradle property must be set")
    spotless {
        go {
            gofmt("go$goVersion")
            target("*.go", "**/*.go")
            targetExclude("*_generated.go")
        }
    }
    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
} else {
    spotless {
        java {
            // No Java sources in this project
            target("")
        }
    }
}

sonar {
    properties {
        property("sonar.sources", ".")
        property("sonar.inclusions", "**/*.go")
        property("sonar.exclusions", "**/render.go,**/generate_source.go,**/*_generated.go,**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.tests", ".")
        property("sonar.test.inclusions", "**/*_test.go")
        property("sonar.test.exclusions", "**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.go.tests.reportPaths", "build/test-report.json")
        property("sonar.go.coverage.reportPaths", "build/test-coverage.out")
        property("sonar.go.golangci-lint.reportPaths", "build/reports/golangci-lint-report.xml")
    }
}
