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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashSet
import java.util.jar.JarInputStream

plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

// require sonar-go-to-slang binaries to be build
tasks.shadowJar { dependsOn(":sonar-go-to-slang:build") }
tasks.test { dependsOn(":sonar-go-to-slang:build") }

dependencies {
    compileOnly(libs.sonar.plugin.api)

    implementation(libs.sonar.analyzer.commons)
    implementation(libs.slang.plugin)
    implementation(libs.slang.checks)
    implementation(libs.slang.api)
    implementation(libs.checkstyle.import)
    // dependency on sonar-go-to-slang binaries
    implementation(libs.minimal.json)

    runtimeOnly(files(project.project(":sonar-go-to-slang").buildDir))

    testImplementation(libs.slang.antlr)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.slang.testing)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.sonar.plugin.api.impl)
    testImplementation(libs.sonar.plugin.api.test.fixtures)

    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.jar {
    manifest {
        val displayVersion =
            if (!project.hasProperty("buildNumber")) {
                project.version
            } else {
                project.version.toString()
                    .substring(0, project.version.toString().lastIndexOf(".")) + " (build ${project.property("buildNumber")})"
            }
        val buildDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        val commitHash = providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
        attributes(
            mapOf(
                "Build-Time" to buildDate,
                "Implementation-Build" to commitHash,
                "Plugin-BuildDate" to buildDate,
                "Plugin-ChildFirstClassLoader" to "false",
                "Plugin-Class" to "org.sonar.go.plugin.GoPlugin",
                "Plugin-Description" to "Code Analyzer for Go",
                "Plugin-Developers" to "SonarSource Team",
                "Plugin-Display-Version" to displayVersion,
                "Plugin-Homepage" to "http://redirect.sonarsource.com/plugins/go.html",
                "Plugin-IssueTrackerUrl" to "https://jira.sonarsource.com/browse/SONARGO",
                "Plugin-Key" to "go",
                "Plugin-License" to "GNU LGPL 3",
                "Plugin-Name" to "Go Code Quality and Security",
                "Plugin-Organization" to "SonarSource",
                "Plugin-OrganizationUrl" to "http://www.sonarsource.com",
                "Plugin-SourcesUrl" to "https://github.com/SonarSource/sonar-go",
                "Plugin-Version" to project.version,
                "Plugin-RequiredForLanguages" to "go",
                "Sonar-Version" to "6.7",
                "SonarLint-Supported" to "true",
                "Version" to "${project.version}",
                "Jre-Min-Version" to "11"
            )
        )
    }
}

tasks.shadowJar {
    minimize { }
    dependencies {
        exclude(dependency("org.sonarsource.api.plugin:sonar-plugin-api"))
        exclude(dependency("org.codehaus.woodstox:.*"))
        exclude(dependency("org.codehaus.staxmate:.*"))
        exclude(dependency("com.google.code.findbugs:jsr305"))

        exclude("libs/**")
        exclude("META-INF/maven/**")
        exclude("tmp/**")
        exclude("spotless/**")
    }
    doLast {
        enforceJarSizeAndCheckContent(tasks.shadowJar.get().archiveFile.get().asFile, 9_000_000L, 9_500_000L)
    }
}

artifacts {
    archives(tasks.shadowJar)
}

tasks.artifactoryPublish {
    skip = false
}

publishing {
    publications.withType<MavenPublication> {
        artifact(tasks.shadowJar) {
            classifier = null
        }
        artifact(tasks.sourcesJar)
        artifact(tasks.javadocJar)
    }
}

fun enforceJarSizeAndCheckContent(
    file: File,
    minSize: Long,
    maxSize: Long,
) {
    val size = file.length()
    if (size < minSize) {
        throw GradleException("${file.path} size ($size) too small. Min is $minSize")
    } else if (size > maxSize) {
        throw GradleException("${file.path} size ($size) too large. Max is $maxSize")
    }
    checkJarEntriesPathUniqueness(file)
}

// A jar should not contain 2 entries with the same path, furthermore Pack200 will fail to unpack it
fun checkJarEntriesPathUniqueness(file: File) {
    val allNames = HashSet<String>()
    val duplicatedNames = HashSet<String>()
    file.inputStream().use { input ->
        JarInputStream(input).use { jarInput ->
            for (jarEntry in generateSequence { jarInput.nextJarEntry }) {
                if (!allNames.add(jarEntry.name)) {
                    duplicatedNames.add(jarEntry.name)
                }
            }
        }
    }
    if (duplicatedNames.isNotEmpty()) {
        throw GradleException("Duplicated entries in the jar: '${file.path}': ${duplicatedNames.joinToString(", ")}")
    }
}
