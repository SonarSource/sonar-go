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
}

dependencies {
    compileOnly(libs.slf4j.api)

    implementation(libs.sonar.analyzer.commons)
    implementation(libs.minimal.json)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation("org.junit.jupiter:junit-jupiter")
}
