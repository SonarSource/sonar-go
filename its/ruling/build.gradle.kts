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
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    "integrationTestImplementation"(libs.slf4j.api)
    "integrationTestImplementation"(libs.sonar.analyzer.commons)
    "integrationTestImplementation"(libs.assertj.core)
    "integrationTestImplementation"(libs.sonar.orchestrator.junit5) {
        exclude("ch.qos.logback", "logback-classic")
        exclude("org.slf4j", "slf4j-api")
        exclude("org.slf4j", "jcl-over-slf4j")
        exclude("org.slf4j", "log4j-over-slf4j")
    }
    "integrationTestImplementation"(libs.junit.jupiter.api)

    "integrationTestRuntimeOnly"(project(":sonar-go-plugin", configuration = "shadow"))
    "integrationTestRuntimeOnly"(libs.junit.jupiter.engine)
}

sonar.isSkipProject = true

integrationTest {
    testSources.set(rootProject.file("its/sources"))
}
tasks.named<Test>("integrationTest") {
    systemProperty("java.awt.headless", "true")
}
