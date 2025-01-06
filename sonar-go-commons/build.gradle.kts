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
plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("java-test-fixtures")
}

// require sonar-go-to-slang binaries to be build

dependencies {
    compileOnly(libs.sonar.plugin.api)

    implementation(libs.sonar.analyzer.commons)
    implementation(libs.slang.api)
    implementation(libs.checkstyle.import)
    implementation(libs.minimal.json)

    testFixturesImplementation(libs.slang.api)
    testFixturesImplementation(libs.assertj.core)
    testFixturesImplementation(libs.mockito.core)
    testFixturesImplementation(libs.sonar.analyzer.test.commons)
    testFixturesImplementation(libs.classgraph)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.sonar.plugin.api.impl)
    testFixturesImplementation(libs.sonar.plugin.api.test.fixtures)

    testRuntimeOnly(libs.junit.jupiter.engine)
}
