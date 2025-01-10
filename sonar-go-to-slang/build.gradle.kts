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
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.sonarsource.cloudnative.gradle.callMake

plugins {
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.go-binary-builder")
}

val goVersion = providers.environmentVariable("GO_VERSION").getOrElse("1.23.4")
val isCi: Boolean = System.getenv("CI")?.equals("true") ?: false

// CI - run the build of go code with local make.sh/make.bat script
if (isCi) {
    // Define and trigger tasks in this order: clean, compile and test go code
    tasks.register<Exec>("cleanGoCode") {
        description = "Clean all compiled version of the go code."
        group = "build"

        callMake("clean")
    }

    tasks.register<Exec>("compileGo") {
        // description = "Compile the go code for the local system."
        description = "Generate Go parser and build the Go executable for CI"
        group = "build"

        inputs.property("GO_CROSS_COMPILE", System.getenv("GO_CROSS_COMPILE") ?: "0")
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

        outputs.file("goparser_generated.go")
        outputs.dir("build/executable")
        outputs.cacheIf { true }

        callMake("build")
    }

    val lintGoCode = tasks.register<Exec>("lintGoCode") {
        description = "Run an external Go linter."
        group = "verification"

        val reportPath = layout.buildDirectory.file("reports/golangci-lint-report.xml")
        inputs.files(
            fileTree(projectDir).matching {
                include("resources/**/*.go")
            }
        )
        outputs.files(reportPath)
        outputs.cacheIf { true }

        commandLine(
            "golangci-lint",
            "run",
            "--go=${requireNotNull(System.getenv("GO_VERSION")) { "Go version is unset in the environment" }}",
            "--out-format=checkstyle:${reportPath.get().asFile}"
        )
        // golangci-lint returns non-zero exit code if there are issues, we don't want to fail the build in this case.
        // A report with issues will be later ingested by SonarQube.
        isIgnoreExitValue = true
    }

    tasks.register<Exec>("testGoCode") {
        description = "Test the executable produced by the compile go code step."
        group = "verification"

        dependsOn("compileGo")
        callMake("test")
    }

    tasks.named("clean") {
        dependsOn("cleanGoCode")
    }

    tasks.named("assemble") {
        dependsOn("compileGo")
    }

    tasks.named("test") {
        dependsOn("testGoCode")
    }

    tasks.named("check") {
        dependsOn(lintGoCode)
    }

    rootProject.tasks.named("sonar") {
        // As lintGoCode produces a report to be ingested by SonarQube, we need to add an explicit dependency to it.
        // See https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/scanners/sonarscanner-for-gradle/#task-dependencies
        dependsOn(lintGoCode)
    }

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
}

// Local - run the build of go code with docker images
if (!isCi) {
    tasks.register<Exec>("buildDockerImage") {
        description = "Build the docker image to build the go code."
        group = "build"

        inputs.file("$rootDir/Dockerfile")
        // Task outputs are not set, because it is too difficult to check if image is built;
        // We can ignore Gradle caches here, because Docker takes care of its own caches anyway.
        errorOutput = System.out

        val uidProvider = objects.property<Long>()
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        if (os.isLinux || os.isMacOsX) {
            // UID of the user inside the container should match this of the host user, otherwise files from the host will be not accessible by the container.
            val uid = com.sun.security.auth.module.UnixSystem().uid
            uidProvider.set(uid)
        }

        val noTrafficInspection = "false" == System.getProperty("trafficInspection")

        val arguments = buildList {
            add("docker")
            add("buildx")
            add("build")
            add("--file")
            add(rootProject.file("Dockerfile").absolutePath)
            if (noTrafficInspection) {
                add("--build-arg")
                add("BUILD_ENV=dev")
            } else {
                add("--network=host")
                add("--build-arg")
                add("BUILD_ENV=dev_custom_cert")
            }
            if (uidProvider.isPresent) {
                add("--build-arg")
                add("UID=${uidProvider.get()}")
            }
            add("--build-arg")
            add("GO_VERSION=$goVersion")
            add("--platform")
            add("linux/amd64")
            add("-t")
            add("sonar-go-go-builder")
            add("--progress")
            add("plain")
            add("${project.projectDir}")
        }

        commandLine(arguments)
    }

    tasks.register<Exec>("compileGo") {
        description = "Generate Go parser and build the Go executable from the docker image."
        group = "build"
        dependsOn("buildDockerImage")
        errorOutput = System.out

        inputs.files(
            fileTree(projectDir).matching {
                include(
                    "*.go",
                    "**/*.go",
                    "**/go.mod",
                    "**/go.sum",
                    "make.sh",
                    "make.bat"
                )
                exclude("build/**", "*_generated.go")
            }
        )
        outputs.file("goparser_generated.go")
        outputs.dir("build/executable")
        outputs.cacheIf { true }

        commandLine(
            "docker",
            "run",
            "--rm",
            "--network=host",
            "--platform",
            "linux/amd64",
            "--mount",
            "type=bind,source=${project.projectDir},target=/home/sonarsource/sonar-go-to-slang",
            "--env",
            "GO_CROSS_COMPILE=${System.getenv("GO_CROSS_COMPILE") ?: "1"}",
            "sonar-go-go-builder",
            "bash",
            "-c",
            "cd /home/sonarsource/sonar-go-to-slang && ./make.sh clean && ./make.sh build && ./make.sh test"
        )
    }

    tasks.named("assemble") {
        dependsOn("compileGo")
    }

    spotless {
        java {
            // No Java sources in this project
            target("")
        }
    }
}
