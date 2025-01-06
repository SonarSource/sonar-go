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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashSet
import java.util.jar.JarInputStream

plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.publishing-configuration")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.sonar.plugin.api)

    implementation(project(":sonar-go-checks"))
    implementation(project(":sonar-go-commons"))
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.slang.api)
    implementation(libs.checkstyle.import)
    implementation(libs.minimal.json)
    implementation(project(":sonar-go-to-slang", configuration = "goBinaries"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.sonar.analyzer.test.commons)
    testImplementation(libs.classgraph)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.sonar.plugin.api.impl)
    testImplementation(libs.sonar.plugin.api.test.fixtures)
    testImplementation(testFixtures(project(":sonar-go-commons")))

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
        enforceJarSizeAndCheckContent(tasks.shadowJar.get().archiveFile.get().asFile, 5_000_000L, 5_500_000L)
    }
}

artifacts {
    archives(tasks.shadowJar)
}

publishingConfiguration {
    pomName = properties["projectTitle"] as String
    scmUrl = "https://github.com/SonarSource/sonar-go"

    license {
        name = "SSALv1"
        url = "https://sonarsource.com/license/ssal/"
        distribution = "repo"
    }
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
