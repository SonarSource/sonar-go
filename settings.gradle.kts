/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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
pluginManagement {
    includeBuild("build-logic/common") {
        name = "build-logic-common"
    }
    includeBuild("build-logic/go") {
        name = "build-logic-go"
    }
}

plugins {
    id("org.sonarsource.cloud-native.common-settings")
}

rootProject.name = "sonar-go"

include(":sonar-go-to-slang")
include(":sonar-go-plugin")
include(":sonar-go-checks")
include(":sonar-go-frontend")
include(":sonar-go-commons")

// "extraSettings.gradle" should not be renamed "settings.gradle" to not create a wrong project rootDir
var extraSettings = File(rootDir, "private/extraSettings.gradle.kts")
if (extraSettings.exists()) {
    apply(extraSettings)
}
