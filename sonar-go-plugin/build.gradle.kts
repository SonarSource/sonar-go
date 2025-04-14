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
import org.sonarsource.cloudnative.gradle.checkJarEntriesPathUniqueness
import org.sonarsource.cloudnative.gradle.commitHashProvider
import org.sonarsource.cloudnative.gradle.enforceJarSize

plugins {
    id("org.sonarsource.cloud-native.sonar-plugin")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)

    implementation(project(":sonar-go-checks"))
    implementation(project(":sonar-go-commons"))
    implementation(project(":sonar-go-frontend"))
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.minimal.json)
    implementation(project(":sonar-go-to-slang", configuration = "goBinaries"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.sonar.analyzer.test.commons)
    testImplementation(libs.classgraph)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.sonar.plugin.api.impl)
    testImplementation(libs.sonar.plugin.api.test.fixtures)
    testImplementation(testFixtures(project(":sonar-go-commons")))

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.jar {
    manifest {
        val displayVersion =
            if (!project.hasProperty("buildNumber")) {
                project.version
            } else {
                project.version.toString().run {
                    "${substring(0, lastIndexOf("."))} (build ${project.property("buildNumber")})"
                }
            }
        val buildDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        val commitHash = commitHashProvider().get().trim()
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
    val isCrossCompile: Boolean = providers.environmentVariable("GO_CROSS_COMPILE").map { it == "1" }.getOrElse(false)
    val pluginJar = tasks.shadowJar.get().archiveFile.get().asFile
    doLast {
        val (minSize, maxSize) = if (isCrossCompile) {
            10_000_000L to 11_500_000L
        } else {
            3_000_000L to 4_000_000L
        }
        enforceJarSize(pluginJar, minSize, maxSize)
        checkJarEntriesPathUniqueness(pluginJar)
    }
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
