/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
    alias(libs.plugins.protobuf)
}

dependencies {
    compileOnly(libs.slf4j.api)

    implementation(libs.sonar.analyzer.commons)
    api(libs.protobuf.java)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(project(":sonar-go-to-slang", configuration = "goBinaries"))
    testImplementation(testFixtures(project(":sonar-go-commons")))

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The classes of the wire format are generated from the same schema the Go side is generated from, so
// that the two can only drift apart through an edit of the schema itself.
protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
}

sourceSets {
    main {
        proto {
            srcDir(rootProject.file("sonar-go-to-slang/proto/slang"))
        }
    }
}

spotless {
    java {
        // protoc formats its output itself, and writes no license header in it.
        targetExclude("build/generated/sources/proto/**")
    }
}
